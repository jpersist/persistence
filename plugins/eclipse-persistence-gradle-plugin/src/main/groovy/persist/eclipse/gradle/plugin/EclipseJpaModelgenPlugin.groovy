package persist.eclipse.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import persist.eclipse.gradle.extension.EclipselinkExtension

/**
 * Main entry point for the EclipseLink JPA Modelgen Processor Plugin.
 * <p>
 * This plugin automates the generation of the JPA static canonical metamodel using
 * the EclipseLink JpaModelgen Annotation Processor. It applies the standard
 * Gradle {@link org.gradle.api.plugins.JavaPlugin}, provisions the required processing
 * dependencies safely using a lazy version fallback mechanism, dynamically injects
 * compiler arguments targeting the appropriate <code>persistence.xml</code> configuration file path,
 * and registers the annotation processor generated source directory into each source set
 * so that IDEs such as Eclipse and IntelliJ can compile and link the generated static
 * metamodel classes (e.g.&nbsp;{@code Customer_.java}) seamlessly.
 * </p>
 *
 * @see EclipselinkExtension
 */
class EclipseJpaModelgenPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)

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

            project.dependencies.add(annotationProcessorConfigName, 'org.eclipse.persistence:org.eclipse.persistence.jpa.modelgen.processor')

            // Explicitly expose the standard annotation processor source folder to the Java source set path.
            // This ensures the Eclipse IDE compiles and links generated static metamodels (e.g. Customer_.java) seamlessly.
            sourceSet.java.srcDir(project.layout.buildDirectory.dir("generated/sources/annotationProcessor/java/${sourceSet.name}"))

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
    }

}
