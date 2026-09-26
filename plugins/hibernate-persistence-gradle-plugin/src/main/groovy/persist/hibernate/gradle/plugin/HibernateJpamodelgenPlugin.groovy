package persist.hibernate.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.model.Classpath
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.gradle.plugins.ide.eclipse.model.SourceFolder
import persist.platform.gradle.plugin.PersistencePlatformPlugin

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
        project.plugins.apply(PersistencePlatformPlugin)
        project.plugins.apply(EclipsePlugin)

        // Fetch the project sourceSets container extension
        def sourceSets = project.extensions.getByType(SourceSetContainer)

        // Automatically and lazily iterate over all source sets configured in the module
        sourceSets.configureEach { sourceSet ->
            // Determine the matching annotation processor configuration name.
            String configName = sourceSet.annotationProcessorConfigurationName

            // Inject the dependency dynamically into the calculated configuration name
            project.dependencies.add(configName, 'org.hibernate.orm:hibernate-jpamodelgen')
        }

        // Dynamically register generated source folders for ALL source sets into Eclipse
        EclipseModel eclipse = project.extensions.getByType(EclipseModel)

        eclipse.classpath.file.whenMerged { classpathObj ->
            // Cast the raw object to its concrete type provided by Buildship
            Classpath classpath = (Classpath) classpathObj

            sourceSets.configureEach { sourceSet ->
                def generatedSourcePath = "build/generated/sources/annotationProcessor/java/${sourceSet.name}"
                def generatedFolder = project.file(generatedSourcePath)

                // 1. Check if the folder physically exists and contains files
                boolean hasGeneratedFiles = generatedFolder.exists() && generatedFolder.list() != null && generatedFolder.list().length > 0

                if (hasGeneratedFiles) {
                    // 2. Add the source folder if it contains files and isn't already registered
                    boolean alreadyExists = classpath.entries.any { entry ->
                        entry instanceof SourceFolder && ((SourceFolder) entry).path == generatedSourcePath
                    }

                    if (!alreadyExists) {
                        def srcFolder = new SourceFolder(generatedSourcePath, null)
                        if (sourceSet.name != SourceSet.MAIN_SOURCE_SET_NAME) {
                            srcFolder.entryAttributes.put('test', 'true')
                        }
                        classpath.entries.add(srcFolder)
                    }
                } else {
                    // 3. Remove the entry cleanly if it's empty or doesn't exist
                    classpath.entries.removeIf { entry ->
                        entry instanceof SourceFolder && ((SourceFolder) entry).path == generatedSourcePath
                    }
                }
            }
        }
    }

}
