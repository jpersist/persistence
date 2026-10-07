package persist.hibernate.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer

/**
 * Main entry point for the Hibernate JPA Modelgen Processor Plugin.
 * <p>
 * This plugin applies the {@link org.gradle.api.plugins.JavaPlugin} and registers the Hibernate JPA
 * Annotation Processor configuration engines, helping engineers build type-safe Criteria API queries.
 * It tracks active project source sets, lazily wires the unversioned
 * <code>hibernate-jpamodelgen</code> framework dependency directly onto the targeted
 * <code>annotationProcessor</code> configurations, registers the annotation processor generated
 * source directory into each source set for seamless IDE indexing, and ensures version suggestions
 * play seamlessly alongside upstream platform BOM definitions or corporate Gradle Version Catalogs.
 * </p>
 * <p>
 * <b>Since 1.5.2:</b> The default {@code hibernate-jpamodelgen} annotation processor dependency
 * is now added conditionally — only when the user has not declared any custom annotation processor
 * dependencies in the corresponding {@code annotationProcessor} configuration. This allows
 * projects to substitute or override the default processor without conflicts.
 * </p>
 */
class HibernateJpamodelgenPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)

        // Fetch the project sourceSets container extension
        def sourceSets = project.extensions.getByType(SourceSetContainer)

        // Automatically and lazily iterate over all source sets configured in the module
        sourceSets.configureEach { sourceSet ->
            // Determine the matching annotation processor configuration name.
            String configName = sourceSet.annotationProcessorConfigurationName

            // Expose the annotation processor source folder into the core Java source directory pool.
            sourceSet.java.srcDir(project.layout.buildDirectory.dir("generated/sources/annotationProcessor/java/${sourceSet.name}"))

            // Only add the default Hibernate processor if the user's annotationProcessor block is completely empty
            project.afterEvaluate {
                project.configurations.named(configName).configure { config ->
                    if (config.dependencies.isEmpty()) {
                        project.dependencies.add(configName, 'org.hibernate.orm:hibernate-jpamodelgen')
                    }
                }
            }
        }
    }

}
