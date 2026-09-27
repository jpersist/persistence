package persist.jakarta.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.language.jvm.tasks.ProcessResources
import persist.jakarta.gradle.extension.PersistenceExtension
import persist.jakarta.gradle.task.ProcessPersistenceDescriptor

/**
 * Gradle plugin that manages Jakarta Persistence (JPA) descriptor generation.
 * <p>
 * Registered with plugin id {@code io.github.jpersist.jpa}, this plugin:
 * </p>
 * <ol>
 *     <li>Applies the {@link org.gradle.api.plugins.JavaPlugin}.</li>
 *     <li>Creates a {@code jpa} dependency configuration for propagating
 *         platform/BOM version constraints into standard Java configurations.</li>
 *     <li>Creates a {@code jarFile} dependency configuration for declaring
 *         module JARs to be injected as {@code <jar-file>} entries.</li>
 *     <li>Registers the {@code persistence}
 *         {@link persist.jakarta.gradle.extension.PersistenceExtension} DSL
 *         extension.</li>
 *     <li>Registers the {@code processPersistenceDescriptor}
 *         {@link ProcessPersistenceDescriptor} task that generates or merges
 *         the final {@code persistence.xml}.</li>
 *     <li>Wires the task output into {@code processResources} so the
 *         generated descriptor ends up in the JAR.</li>
 * </ol>
 * <p>
 * This plugin replaces the deprecated {@code persistence-gradle-plugin}
 * ({@code io.github.jpersist.persistence}).
 * </p>
 *
 * @since 1.1.0
 */
class JakartaPersistencePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)

        configureJpaConfiguration(project)

        // 1. Create native dependency configuration
        def jarFileConfig = project.configurations.create("jarFile") {
            canBeConsumed = false
            canBeResolved = true
        }

        // 2. Register Extension API DSL
        def extension = project.extensions.create("persistence", PersistenceExtension)

        project.plugins.withType(JavaPlugin).configureEach {
            project.configurations.named("implementation").configure {
                it.extendsFrom(jarFileConfig)
            }

            // 3. Register standard descriptors processor task
            def processTask = project.tasks.register("processPersistenceDescriptor", ProcessPersistenceDescriptor) { task ->
                task.xmlVersion.set(extension.version)

                // Lazily snapshot current domain states to prevent execution timing issues
                task.units.set(project.provider { new ArrayList<>(extension.persistenceUnits) })

                // Resolve first level declared jars
                task.jarFileNames.set(project.provider {
                    if (jarFileConfig.isEmpty()) return []
                    jarFileConfig.resolvedConfiguration.firstLevelModuleDependencies.collectMany { dep ->
                        dep.moduleArtifacts.collect { artifact -> artifact.file.name }
                    }
                })

                // Safe input lookup tracking direct source files — only set when the file exists
                // so that @Optional @InputFile allows the task to run in generate-from-scratch mode
                File sourceFile = project.file("src/main/resources/META-INF/persistence.xml")
                if (sourceFile.exists()) {
                    task.persistenceXml.set(project.layout.projectDirectory.file("src/main/resources/META-INF/persistence.xml"))
                }

                // Direct output tracking safely to resources destination
                task.destinationFile.set(project.layout.buildDirectory.file("generated/resources/main/META-INF/persistence.xml"))
            }

            // 4. Feed output securely back to resource processor as an input source!
            project.tasks.named("processResources", ProcessResources).configure { resourceTask ->
                // Prevent duplicate/conflict matching by excluding the un-patched original file
                resourceTask.exclude("META-INF/persistence.xml")

                // Place the generated file into META-INF/ within the resources output
                resourceTask.from(processTask.flatMap { it.destinationFile }) {
                    into("META-INF")
                }
            }
        }
    }

    /**
     * Creates the {@code jpa} dependency configuration and extends all
     * standard Java source-set configurations from it, so that platform/BOM
     * constraints declared in {@code jpa} propagate automatically.
     *
     * @param project The Gradle project to configure.
     */
    private static void configureJpaConfiguration(Project project) {
        def persistence = project.configurations.register('jpa') { config ->
            config.visible = false
            config.canBeConsumed = false
            config.canBeResolved = false
        }

        // Propagate platform version constraints into all standard Java configurations
        project.extensions.getByType(SourceSetContainer).configureEach { sourceSet ->
            [
                sourceSet.implementationConfigurationName,
                sourceSet.compileOnlyConfigurationName,
                sourceSet.annotationProcessorConfigurationName,
                sourceSet.runtimeOnlyConfigurationName
            ].forEach { configName ->
                project.configurations.named(configName) {
                    it.extendsFrom(persistence.get())
                }
            }
        }
    }

}
