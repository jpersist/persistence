package persist.hibernate.gradle.plugin

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.language.base.plugins.LifecycleBasePlugin
import persist.api.spi.PersistenceServiceProvider
import persist.hibernate.gradle.extension.HibernateExtension
import persist.hibernate.gradle.task.HibernateEnhancementTask
import persist.jakarta.gradle.extension.PersistenceExtension
import persist.jakarta.gradle.plugin.JakartaPersistencePlugin

/**
 * Main entry point for the Hibernate Bytecode Enhancement Plugin.
 * <p>
 * This plugin applies the {@link org.gradle.api.plugins.JavaPlugin}, registers a customizable
 * <code>hibernate</code> configuration extension block, and dynamically instantiates an independent
 * {@link HibernateEnhancementTask} for every active project source set layout whose
 * {@code java.srcDirs} contain at least one directory that physically exists on disk.
 * </p>
 * <p>
 * To ensure absolute compliance with the Gradle Configuration Cache and incremental verification states,
 * this plugin manages separate input/output staging directory targets, safeguarding compilation
 * <code>UP-TO-DATE</code> checking parameters across multi-module build paths.
 * </p>
 * <p>
 * <b>Since 1.7.0:</b> The enhancement task is only registered for source sets that have
 * at least one physical Java source directory present on disk. Source sets with no existing
 * {@code java.srcDirs} directories are silently skipped, preventing clean IDE workspaces
 * from being cluttered with empty tasks (e.g.&nbsp;{@code hibernateEnhanceTestClasses}).
 * </p>
 * <p>
 * <b>Since 1.8.0:</b> The enhancement task now delegates bytecode transformation to an
 * isolated {@link persist.hibernate.gradle.worker.EnhancementWorker} submitted via
 * {@link org.gradle.workers.WorkerExecutor#classLoaderIsolation classLoaderIsolation}.
 * The plugin resolves the active JPA specification version from the
 * {@link PersistenceExtension} registered by the
 * {@link JakartaPersistencePlugin} and dynamically selects the correct
 * Persistence Tools provider library ({@code persistence-tools-jakarta} or
 * {@code persistence-tools-legacy}) into a per-source-set isolated classpath
 * configuration. This decouples the enhancement task from direct Hibernate API
 * imports and ensures the Gradle daemon's classpath remains unpolluted.
 * </p>
 *
 * @see HibernateExtension
 * @see persist.hibernate.gradle.worker.EnhancementWorker
 */
class HibernateEnhancementPlugin implements Plugin<Project> {

    /**
     * Applies the Hibernate bytecode enhancement configuration to the target project.
     * <p>
     * Registers the {@code hibernate} extension, resolves the active JPA specification
     * version from the {@link PersistenceExtension}, creates per-source-set isolated
     * classpath configurations carrying the correct Persistence Tools provider, and
     * registers {@link HibernateEnhancementTask} instances wired into the build lifecycle.
     * </p>
     *
     * @param project The target Gradle project.
     */
    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)

        def hibernate = project.extensions.create('hibernate', HibernateExtension)
        hibernate.enhancement { enhancement ->
            enhancement.enableLazyInitialization.convention(true)
            enhancement.enableDirtyTracking.convention(true)
            enhancement.enableAssociationManagement.convention(true)
            enhancement.enableExtendedEnhancement.convention(false)
        }

        project.getExtensions().getByType(SourceSetContainer).configureEach { sourceSet ->
            // We strictly check for Java source directories since this is a bytecode enhancer
            boolean hasJavaSources = sourceSet.java.srcDirs.any { it.exists() }

            if (hasJavaSources) {
                // Dynamic task naming based on the source set context (e.g., main -> compileJava)
                String classesTaskName = sourceSet.getClassesTaskName()
                String compileTaskName = sourceSet.getCompileJavaTaskName()
                String enhanceTaskName = "hibernateEnhance${classesTaskName.capitalize()}"
                def enhancement = hibernate.enhancement

                // Fetch the compilation task lazily using provider mappings to prevent premature instantiation
                def compileTaskProvider = project.tasks.named(compileTaskName, JavaCompile)

                // Map to an explicitly isolated enhanced directory
                def enhancedClassesDir = project.layout.buildDirectory.dir("enhanced/classes/java/${sourceSet.name}")

                // Locate the active root JPA extension instance to inherit the specification version string
                def extensionContainer = project.extensions.findByName('persistence')
                def activeVersionProvider = project.provider {
                    if (extensionContainer instanceof NamedDomainObjectContainer) {
                        def persistenceExtension = extensionContainer.maybeCreate(sourceSet.name)
                        if (persistenceExtension instanceof PersistenceExtension) {
                            return persistenceExtension.version.getOrElse("3.0")
                        }
                    }
                    return "3.0" // Default fallback standard
                }

                // Register an isolated internal configuration container for this source set's execution tools
                def enhanceConfigName = sourceSet.name == 'main' ? "hibernateEnhanceClasspath" : "${sourceSet.name}HibernateEnhanceClasspath"
                def enhanceConfigProvider = project.configurations.register(enhanceConfigName) { config ->
                    config.canBeConsumed = false
                    config.canBeResolved = true

                    // Wire version constraints and coordinates dynamically at configuration time
                    config.extendsFrom(project.configurations.maybeCreate('jpa'))
                }

                // 🌟 TECHNICAL BREAKTHROUGH: ClassLoader Extraction Loop
                // Instead of calling project.dependencies.add with remote text coordinates,
                // harvest the physical jar files that are already part of the plugin's active buildscript classpath context.
                project.configurations.named(enhanceConfigName).configure { config ->
                    Set<Object> files = []
                    // We add a lazy FileCollection wrapper which Gradle handles perfectly at execution time
                    config.dependencies.add(project.dependencies.create(project.files(project.provider {
                        def version = activeVersionProvider.get()
                        boolean isLegacy = ["2.0", "2.1", "2.2"].contains(version)
                        String markerClassName = isLegacy
                            ? "persist.hibernate.enhance.legacy.LegacyHibernateEnhancerProvider"
                            : "persist.hibernate.enhance.jakarta.JakartaHibernateEnhancerProvider"

                        try {
                            // Attempt to locate the classes on the active plugin/buildscript classpath wrapper context
                            Class<?> providerClass = Class.forName(markerClassName, false, HibernateEnhancementPlugin.class.classLoader)
                            def codeSourceLocation = providerClass.protectionDomain.codeSource?.location

                            if (codeSourceLocation != null) {
                                def toolsApiLocation = PersistenceServiceProvider.class.protectionDomain.codeSource?.location
                                // Return the array of physical file locations discovered inside the environment
                                files.addAll([codeSourceLocation, toolsApiLocation])
                            }
                        } catch (ClassNotFoundException ignored) {
                            // Fallback strategy if executing inside a standard standalone build outside the mono-repo context
                            // Note: Since we cannot return a GAV string provider here inside project.files(),
                            // we resolve it as a detached dependency placeholder inside the fallback block.
                            def toolsVersion = JakartaPersistencePlugin.resolvePersistenceToolsVersion()
                            String providerArtifact = isLegacy ? "persistence-tools-legacy" : "persistence-tools-jakarta"

                            // Return the resolved files of a detached configuration context to bypass the file constraints safely
                            def detachedConfig = project.configurations.detachedConfiguration(
                                project.dependencies.create("io.github.jpersist.tools:${providerArtifact}:${toolsVersion}")
                            )
                            files.add(detachedConfig.files)
                        }

                        return files
                    })))
                }

                // Register the task completely outside the compile task configuration execution context
                def enhanceTaskProvider = project.tasks.register(enhanceTaskName, HibernateEnhancementTask) { enhanceTask ->
                    enhanceTask.group = LifecycleBasePlugin.BUILD_GROUP
                    enhanceTask.description = "Enhances entity ${classesTaskName}."

                    // Pass the reactive JPA version provider down to the task instance
                    enhanceTask.jpaVersion.set(activeVersionProvider)

                    // Inject the isolated toolset libraries containing our API SPI and provider factories
                    enhanceTask.enhanceClasspath.from(enhanceConfigProvider)

                    // Pass third-party structures and module dependencies separately via compilation boundaries
                    enhanceTask.compileClasspath.from(sourceSet.compileClasspath)

                    // Properly map input/output boundaries to maintain task correctness
                    enhanceTask.sourceClassesDir.set(compileTaskProvider.flatMap { it.getDestinationDirectory() })
                    enhanceTask.targetClassesDir.set(enhancedClassesDir)

                    // Feed extension configuration states safely into the Task fields
                    enhanceTask.lazyInitializationEnabled.set(enhancement.enableLazyInitialization)
                    enhanceTask.dirtyTrackingEnabled.set(enhancement.enableDirtyTracking)
                    enhanceTask.associationManagementEnabled.set(enhancement.enableAssociationManagement)
                    enhanceTask.extendedEnhancementEnabled.set(enhancement.enableExtendedEnhancement)

                    // Hook the lifecycle dependencies safely
                    enhanceTask.mustRunAfter(compileTaskProvider)
                }

                project.tasks.named(classesTaskName).configure { task ->
                    task.dependsOn(enhanceTaskProvider)
                }

                // Re-route the packaging steps to collect classes from the enhanced directory
                project.tasks.withType(Jar).configureEach { jarTask ->
                    // 1. Tell Gradle how to resolve duplicate file conflicts
                    jarTask.duplicatesStrategy = DuplicatesStrategy.EXCLUDE

                    // 2. Add the ENHANCED classes FIRST so they take precedence
                    jarTask.from(enhanceTaskProvider.flatMap { task -> task.getTargetClassesDir() })

                    // 3. Add the ORIGINAL classes SECOND (Duplicates matching woven classes will be ignored)
                    jarTask.from(compileTaskProvider.flatMap { task -> task.destinationDirectory })
                }
            }
        }
    }

}
