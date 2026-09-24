package persist.eclipse.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.model.Classpath
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.gradle.plugins.ide.eclipse.model.SourceFolder
import persist.eclipse.gradle.extension.EclipselinkExtension

/**
 * Main entry point for the EclipseLink JPA Modelgen Processor Plugin.
 * <p>
 * This plugin automates the generation of the JPA static canonical metamodel using
 * the EclipseLink JpaModelgen Annotation Processor. It dynamically applies the standard
 * Gradle 'java' and 'eclipse' development environments, provisions the required processing
 * dependencies safely using a lazy version fallback mechanism, and dynamically injects
 * compiler arguments targeting the appropriate <code>persistence.xml</code> configuration file path.
 * </p>
 * <p>
 * Additionally, it integrates with Eclipse Buildship's synchronization engine via a strongly-typed
 * hook to register non-empty generated source directories directly into the Eclipse IDE <code>.classpath</code> file.
 * </p>
 *
 * @see EclipselinkExtension
 */
class EclipseJpaModelgenPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)
        project.plugins.apply(EclipsePlugin)

        // 1. Create a NamedDomainObjectContainer using Gradle's ObjectFactory
        def container = project.objects.domainObjectContainer(EclipselinkExtension)

        // 2. Expose the container as the top-level 'eclipselink' extension block
        project.extensions.add('eclipselink', container)

        // 3. Automatically listen for source sets to prepopulate conventions and wire dependencies
        project.extensions.getByType(SourceSetContainer).configureEach { sourceSet ->
            // Automatically initialize a configuration instance inside the container matching the source set name
            // (e.g., this instantly builds 'eclipselink.main' and 'eclipselink.test')
            EclipselinkExtension extension = container.maybeCreate(sourceSet.name)

            // Setup default convention layout relative to the specific source set resources
            extension.jpaModelgen { jpaModelgen ->
                jpaModelgen.persistenceXml.convention(
                    project.layout.projectDirectory.file("src/${sourceSet.name}/resources/META-INF/persistence.xml")
                )
            }

            // Dynamic dependency assignment matching configuration name targets
            def annotationProcessorConfigName = sourceSet.annotationProcessorConfigurationName
            def implementationConfigName = sourceSet.implementationConfigurationName

            project.dependencies.add(implementationConfigName, 'jakarta.persistence:jakarta.persistence-api')
            project.dependencies.add(implementationConfigName, 'org.eclipse.persistence:org.eclipse.persistence.jpa')

            project.dependencies.add(annotationProcessorConfigName, 'org.eclipse.persistence:org.eclipse.persistence.jpa.modelgen.processor')

            // 4. Safely configure JavaCompile arguments using the source-set-specific path
            String compileTaskName = sourceSet.compileJavaTaskName
            project.tasks.named(compileTaskName, JavaCompile) { compileTask ->
                def compilerArgProvider = project.provider {
                    File file = extension.jpaModelgen.persistenceXml.get().asFile
                    return "-Aeclipselink.persistencexml=${file.absolutePath}"
                }
                compileTask.options.compilerArgs.add(compilerArgProvider.get())
            }
        }

        // 5. Keep your elegant strongly-typed Eclipse sync engine intact
        EclipseModel eclipseModel = project.extensions.getByType(EclipseModel)
        eclipseModel.classpath.file.whenMerged { Object classpathObj ->
            Classpath classpath = (Classpath) classpathObj

            project.extensions.getByType(SourceSetContainer).configureEach { sourceSet ->
                def generatedSourcePath = "build/generated/sources/annotationProcessor/java/${sourceSet.name}"
                def generatedFolder = project.file(generatedSourcePath)

                boolean hasGeneratedFiles = generatedFolder.exists() && generatedFolder.list() != null && generatedFolder.list().length > 0

                if (hasGeneratedFiles) {
                    boolean alreadyExists = classpath.entries.any { entry ->
                        entry instanceof SourceFolder && ((SourceFolder) entry).path == generatedSourcePath
                    }

                    if (!alreadyExists) {
                        def srcFolder = new SourceFolder(generatedSourcePath, null)
                        if (sourceSet.name != SourceSet.MAIN_SOURCE_SET_NAME) {
                            srcFolder.entryAttributes.put("test", "true")
                        }
                        classpath.entries.add(srcFolder)
                    }
                } else {
                    classpath.entries.removeIf { entry ->
                        entry instanceof SourceFolder && ((SourceFolder) entry).path == generatedSourcePath
                    }
                }
            }
        }
    }

}
