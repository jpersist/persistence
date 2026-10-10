package persist.eclipse.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources
import persist.eclipse.gradle.task.EclipseWeaveTask

/**
 * Main entry point for the EclipseLink Static Weaving Plugin.
 * <p>
 * This plugin applies the {@link org.gradle.api.plugins.JavaPlugin} and registers a high-performance,
 * incremental bytecode modification task for every discovered source set container in the project.
 * It programmatically triggers EclipseLink's <code>StaticWeave</code> execution framework
 * post-compilation, transforming standard JPA entities in-place within the compilation pipeline
 * output boundaries. When the {@code jpa} configuration is present (e.g.&nbsp;via
 * {@link persist.jakarta.gradle.plugin.JakartaPersistencePlugin}), the {@code weave} configuration
 * lazily extends from it to inherit platform version constraints.
 * </p>
 * <p>
 * This enables performance optimizations such as strict lazy loading, fetch graph mechanics,
 * and advanced inline dirty tracking without requiring an active <code>-javaagent</code> JVM argument at runtime.
 * </p>
 * <p>
 * <b>Since 1.5.1:</b> The weaving task is automatically skipped if no
 * {@code META-INF/persistence.xml} is found in the processed resources output directory,
 * preventing unnecessary failures in modules that do not define a persistence unit.
 * Additionally, the plugin now resolves a source-set-specific {@code jarFile} configuration
 * (e.g.&nbsp;{@code jarFile} for {@code main}, {@code testJarFile} for {@code test}) and feeds
 * it to the {@link EclipseWeaveTask#getJarFileClasspath() jarFileClasspath}, enabling correct
 * resolution of {@code <jar-file>} entries in multi-module projects.
 * </p>
 *
 * <p>
 * <b>Since 1.5.4:</b> The default {@code org.eclipse.persistence.jpa} weave dependency
 * is now added conditionally — only when the user has not declared any custom dependencies
 * in the {@code weave} configuration. This allows projects to substitute or override
 * the default weave dependency without conflicts.
 * </p>
 *
 * @see EclipseWeaveTask
 */
class EclipseStaticWeavePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)

        def weaveConfig = project.configurations.maybeCreate('weave')
        project.configurations.matching { it.name == 'jpa'}.configureEach { jpaConfig ->
            weaveConfig.extendsFrom(jpaConfig)
        }

        project.extensions.getByType(SourceSetContainer).configureEach { sourceSet ->
            // Dynamic task naming based on the source set context (e.g., main -> compileJava)
            String classesTaskName = sourceSet.getClassesTaskName()
            String compileTaskName = sourceSet.getCompileJavaTaskName()
            String weaveTaskName = "eclipseWeave${classesTaskName.capitalize()}"

            def jarFileConfigName = sourceSet.name == 'main' ? "jarFile" : "${sourceSet.name}JarFile"

            // Fetch the compileJava task provider safely
            def compileJavaProvider = project.tasks.named(compileTaskName, JavaCompile)

            // Locate the native, built-in processResources task provider
            def processResourcesProvider = project.tasks.named(sourceSet.processResourcesTaskName, ProcessResources)
            def processResourcesDestinationDir = processResourcesProvider.flatMap {task ->
                project.layout.dir(project.provider { task.destinationDir })
            }

            // Map to an explicitly isolated woven directory
            def wovenClassesDir = project.layout.buildDirectory.dir("woven/classes/java/${sourceSet.name}")

            // Register your weaving task at the PROJECT level (Fixed Context!)
            def weaveTaskProvider = project.tasks.register(weaveTaskName, EclipseWeaveTask) { task ->
                task.group = LifecycleBasePlugin.BUILD_GROUP
                task.description = "Performs EclipseLink static weaving of entity ${classesTaskName}"

                // Source comes directly from the compile output directory
                task.sourceClassesDir.set(compileJavaProvider.flatMap { it.destinationDirectory })

                // Target goes to a brand new isolated directory
                task.targetClassesDir.set(wovenClassesDir)

                // Pass the native processResources destination directory as the resource info root!
                // This guarantees that all resources, metadata files, and the generated persistence.xml
                // are fully present on disk before weaving begins.
                task.resourcesDir.set(processResourcesDestinationDir)

                // Feed the resolved multi-module jars configuration straight to the task
                // Safely hook into the configuration container only if it exists.
                task.jarFileClasspath.from(project.provider {
                    def config = project.configurations.matching { it.name == jarFileConfigName }.first()
                    return config ? config.files : []
                })

                // Wire the classpaths safely using lazy FileCollections
                task.weaveClasspath.from(weaveConfig)
                task.compileClasspath.from(sourceSet.compileClasspath)

                // Skip weaving if no persistence.xml available
                task.onlyIf("Performs weaving only if persistence.xml is available") {
                    processResourcesDestinationDir.get().file("META-INF/persistence.xml").asFile.exists()
                }

                // Explicitly dictate that weaving runs after BOTH compilation and resource processing are finalized
                task.mustRunAfter(compileJavaProvider)
                task.mustRunAfter(processResourcesProvider)
                task.dependsOn(processResourcesProvider)
            }

            // Safely feed both directories to the jar task and prioritize woven outputs
            project.tasks.withType(Jar).configureEach { jarTask ->

                // 1. Tell Gradle how to resolve duplicate file conflicts
                jarTask.duplicatesStrategy = DuplicatesStrategy.EXCLUDE

                // 2. Add the WOVEN classes FIRST so they take precedence
                jarTask.from(weaveTaskProvider.flatMap { task -> task.getTargetClassesDir() })

                // 3. Add the ORIGINAL classes SECOND (Duplicates matching woven classes will be ignored)
                jarTask.from(compileJavaProvider.flatMap { task -> task.destinationDirectory })
            }

            // Hook the weaving task back into the build lifecycle
            project.tasks.named(classesTaskName) { classesTask ->
                classesTask.dependsOn(weaveTaskProvider)
            }
        }

        project.afterEvaluate {
            def weaveConfigName = 'weave'
            project.configurations.named(weaveConfigName).configure { config ->
                if (config.dependencies.isEmpty()) {
                    project.dependencies.add(weaveConfigName, 'org.eclipse.persistence:org.eclipse.persistence.jpa')
                }
            }
        }
    }

}
