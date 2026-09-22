package persist.eclipse.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import persist.eclipse.gradle.extension.EclipselinkExtension

class EclipseJpaModelgenPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply('java')

        // Register the top-level 'eclipselink' extension
        def extension = project.extensions.create('eclipselink', EclipselinkExtension)

        // Set the lazy default value for the nested property
        extension.jpaModelgen.persistenceXml.convention(
            project.layout.projectDirectory.file('src/main/resources/META-INF/persistence.xml')
        )

        project.dependencies.add('implementation', 'jakarta.persistence:jakarta.persistence-api')
        project.dependencies.add('implementation', 'org.eclipse.persistence:org.eclipse.persistence.jpa')

        // Fetch the project sourceSets container extension
        def sourceSets = project.extensions.getByType(SourceSetContainer)

        // Automatically and lazily iterate over all source sets configured in the module
        sourceSets.configureEach { sourceSet ->
            // Determine the matching annotation processor configuration name.
            // - If the source set name is 'main', the configuration is 'annotationProcessor'
            // - For any other source set (e.g. 'test'), it becomes '${name}AnnotationProcessor' (e.g. 'testAnnotationProcessor')
            String configName = sourceSet.name == 'main' ? 'annotationProcessor' : "${sourceSet.name}AnnotationProcessor"

            // Inject the dependency dynamically into the calculated configuration name
            project.dependencies.add(configName, 'org.eclipse.persistence:org.eclipse.persistence.jpa.modelgen.processor')
        }

        project.tasks.withType(JavaCompile).configureEach { task ->
            // Use project.provider to safely delay evaluation until execution time
            def compilerArgProvider = project.provider {
                def file = extension.jpaModelgen.persistenceXml.get().asFile
                return "-Aeclipselink.persistencexml=${file.absolutePath}"
            }

            task.options.compilerArgs.add(compilerArgProvider.get())
        }
    }

}
