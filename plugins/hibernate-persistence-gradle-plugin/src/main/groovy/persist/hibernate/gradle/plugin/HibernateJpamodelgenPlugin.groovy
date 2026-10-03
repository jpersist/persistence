package persist.hibernate.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import persist.jakarta.gradle.plugin.JakartaPersistencePlugin

/**
 * Main entry point for the Hibernate JPA Modelgen Processor Plugin.
 * <p>
 * This plugin registers the Hibernate JPA Annotation Processor configuration engines, helping
 * engineers build type-safe Criteria API queries. It tracks active project source sets, lazily wires
 * the unversioned <code>hibernate-jpamodelgen</code> framework dependency directly onto the targeted
 * <code>annotationProcessor</code> configurations, and ensures version suggestions play seamlessly
 * alongside upstream platform BOM definitions or corporate Gradle Version Catalogs.
 * </p>
 */
class HibernateJpamodelgenPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(JakartaPersistencePlugin)

        // Fetch the project sourceSets container extension
        def sourceSets = project.extensions.getByType(SourceSetContainer)

        // Automatically and lazily iterate over all source sets configured in the module
        sourceSets.configureEach { sourceSet ->
            // Determine the matching annotation processor configuration name.
            String configName = sourceSet.annotationProcessorConfigurationName

            // Expose the annotation processor source folder into the core Java source directory pool.
            sourceSet.java.srcDir(project.layout.buildDirectory.dir("generated/sources/annotationProcessor/java/${sourceSet.name}"))

            // Inject the dependency dynamically into the calculated configuration name
            project.dependencies.add(configName, 'org.hibernate.orm:hibernate-jpamodelgen')
        }
    }

}
