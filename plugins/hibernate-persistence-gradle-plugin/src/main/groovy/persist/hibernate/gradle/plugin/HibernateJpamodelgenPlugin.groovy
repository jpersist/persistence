package persist.hibernate.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.model.Classpath
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.gradle.plugins.ide.eclipse.model.SourceFolder

class HibernateJpamodelgenPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)
        project.plugins.apply(EclipsePlugin)

        // Fetch the project sourceSets container extension
        def sourceSets = project.extensions.getByType(SourceSetContainer)

        // Automatically and lazily iterate over all source sets configured in the module
        sourceSets.configureEach { sourceSet ->
            // Determine the matching annotation processor configuration name.
            // - If the source set name is 'main', the configuration is 'annotationProcessor'
            // - For any other source set (e.g. 'test'), it becomes '${name}AnnotationProcessor' (e.g. 'testAnnotationProcessor')
            String configName = sourceSet.name == 'main' ? 'annotationProcessor' : "${sourceSet.name}AnnotationProcessor"

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
