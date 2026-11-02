package persist.openjpa.gradle.plugin

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources
import persist.openjpa.gradle.extension.OpenJPAExtension
import persist.openjpa.gradle.task.OpenJPAEnhanceTask

/**
 * Gradle plugin that performs compile-time bytecode enhancement for OpenJPA entity classes.
 * <p>
 * This plugin registers per-source-set {@link persist.openjpa.gradle.task.OpenJPAEnhanceTask}
 * instances that fork isolated JVM processes running the OpenJPA {@code PCEnhancer} tool.
 * Enhanced entity classes gain persistence capability hooks injected directly into the
 * compiled binaries, enabling transparent state management and dirty tracking without
 * runtime proxies or a {@code -javaagent}.
 * </p>
 * <p>
 * The plugin automatically creates an {@code enhancement} dependency configuration, sets up
 * isolated classpath resolution per source set, and hooks the enhancement task into the
 * standard Gradle build lifecycle between compilation and JAR packaging.
 * </p>
 *
 * @since 1.8.0
 */
class OpenJPAEnhancementPlugin implements Plugin<Project> {

    /**
     * Applies the OpenJPA bytecode enhancement configuration to the target project.
     * <p>
     * Registers the {@code openjpa} extension container (if not already present),
     * creates per-source-set isolated classpath configurations, registers
     * {@link OpenJPAEnhanceTask} instances that fork the OpenJPA {@code PCEnhancer}
     * tool, hooks them into the build lifecycle, and provisions the default
     * {@code org.apache.openjpa:openjpa} dependency when no user override is present.
     * </p>
     *
     * @param project The target Gradle project.
     */
    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)

        NamedDomainObjectContainer<OpenJPAExtension> container = project.extensions.findByName('openjpa') as NamedDomainObjectContainer<OpenJPAExtension>
        if (container == null) {
            container = project.objects.domainObjectContainer(OpenJPAExtension)
            project.extensions.add('openjpa', container)
        }

        def enhancementConfig = project.configurations.maybeCreate('enhancement')
        project.configurations.matching { it.name == 'jpa'}.configureEach { jpaConfig ->
            enhancementConfig.extendsFrom(jpaConfig)
        }

        project.extensions.getByType(SourceSetContainer).configureEach { sourceSet ->
            // Enhancement requires both entity classes and an active persistence.xml context file
            boolean hasSources = sourceSet.java.srcDirs.any { it.exists() }

            if (hasSources) {
                OpenJPAExtension extension = container.maybeCreate(sourceSet.name)

                // Dynamic task naming based on the source set context (e.g., main -> compileJava)
                String classesTaskName = sourceSet.getClassesTaskName()
                String compileTaskName = sourceSet.getCompileJavaTaskName()
                String enhanceTaskName = "openjpaEnhance${classesTaskName.capitalize()}"

                // Instantiate source-set-specific internal enhancement classpaths
                def enhancementClasspathName = sourceSet.name == 'main' ? "enhancementClasspath" : "${sourceSet.name}EnhancementClasspath"
                def enhancementClasspathProvider = project.configurations.register(enhancementClasspathName) { config ->
                    config.canBeConsumed = false
                    config.canBeResolved = true
                    // Extends from the global user-facing 'enhancement' configuration container
                    config.extendsFrom(enhancementConfig)
                }

                // Fetch the compileJava task provider safely
                def compileJavaProvider = project.tasks.named(compileTaskName, JavaCompile)
                def compileJavaDestinationDir = compileJavaProvider.flatMap { it.destinationDirectory }

                // Locate the native, built-in processResources task provider
                def processResourcesProvider = project.tasks.named(sourceSet.processResourcesTaskName, ProcessResources)
                def processResourcesDestinationDir = processResourcesProvider.flatMap {task ->
                    project.layout.dir(project.provider { task.destinationDir })
                }

                // Map to an explicitly isolated enhanced directory
                def enhancedClassesDir = project.layout.buildDirectory.dir("enhanced/classes/java/${sourceSet.name}")

                // Register your enhancement task at the PROJECT level (Fixed Context!)
                def enhanceTaskProvider = project.tasks.register(enhanceTaskName, OpenJPAEnhanceTask) { task ->
                    task.group = LifecycleBasePlugin.BUILD_GROUP
                    task.description = "Performs OpenJPA build time enhancement of entity ${classesTaskName}"

                    // Source comes directly from the compile output directory
                    task.sourceClassesDir.set(compileJavaDestinationDir)

                    // Target goes to a brand new isolated directory
                    task.targetClassesDir.set(enhancedClassesDir)

                    // Pass the native processResources destination directory as the resource info root!
                    // This guarantees that all resources, metadata files, and the generated persistence.xml
                    // are fully present on disk before enhancement begins.
                    task.resourcesDir.set(processResourcesDestinationDir)

                    // Pass the toolset dependencies configuration provider directly
                    task.enhancerClasspath.from(enhancementClasspathProvider)

                    // Pass the standard project compilation classpath container natively
                    task.compileClasspath.from(sourceSet.compileClasspath)

                    task.addDefaultConstructor.set(extension.enhancement.addDefaultConstructor)
                    task.enforcePropertyRestrictions.set(extension.enhancement.enforcePropertyRestrictions)

                    // Skip enhancement if no persistence.xml available
                    task.onlyIf("Performs enhancement only if persistence.xml is available") {
                        processResourcesDestinationDir.get().file("META-INF/persistence.xml").asFile.exists()
                    }

                    // Explicitly dictate that enhancement runs after BOTH compilation and resource processing are finalized
                    task.mustRunAfter(compileJavaProvider)
                    task.mustRunAfter(processResourcesProvider)
                    task.dependsOn(processResourcesProvider)
                }

                // Safely feed both directories to the jar task and prioritize enhanced outputs
                project.tasks.withType(Jar).configureEach { jarTask ->

                    // 1. Tell Gradle how to resolve duplicate file conflicts
                    jarTask.duplicatesStrategy = DuplicatesStrategy.EXCLUDE

                    // 2. Add the ENHANCED classes FIRST so they take precedence
                    jarTask.from(enhanceTaskProvider.flatMap { task -> task.targetClassesDir })

                    // 3. Add the ORIGINAL classes SECOND (Duplicates matching enhanced classes will be ignored)
                    jarTask.from(compileJavaProvider.flatMap { task -> task.destinationDirectory })
                }

                // Hook the enhancement task back into the build lifecycle
                project.tasks.named(classesTaskName) { classesTask ->
                    classesTask.dependsOn(enhanceTaskProvider)
                }
            }
        }

        project.afterEvaluate {
            def enhancementConfigName = 'enhancement'
            project.configurations.named(enhancementConfigName).configure { config ->
                if (config.dependencies.isEmpty()) {
                    project.dependencies.add(enhancementConfigName, 'org.apache.openjpa:openjpa')
                }
            }
        }
    }

}
