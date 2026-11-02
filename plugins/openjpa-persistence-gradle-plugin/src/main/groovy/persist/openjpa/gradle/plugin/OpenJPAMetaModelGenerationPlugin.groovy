package persist.openjpa.gradle.plugin

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import persist.openjpa.gradle.extension.OpenJPAExtension

/**
 * Gradle plugin that configures the OpenJPA annotation processor for static canonical
 * metamodel class generation.
 * <p>
 * This plugin wires the OpenJPA annotation processor compiler arguments into each source set's
 * {@code compileJava} task, enabling automatic generation of JPA static metamodel classes
 * (e.g., {@code Entity_}) from annotated entity sources during the compilation phase.
 * Configuration options such as logging level, source version, and naming conventions are
 * exposed through the {@link persist.openjpa.gradle.extension.MetaModelExtension} DSL block.
 * </p>
 * <p>
 * The plugin also registers a fallback coordination mechanism that provisions the default
 * {@code org.apache.openjpa:openjpa} dependency on the {@code modelgenProcessor} configuration
 * if no explicit dependency has been declared by the consumer build script.
 * </p>
 *
 * @since 1.8.0
 */
class OpenJPAMetaModelGenerationPlugin implements Plugin<Project> {

    /**
     * Applies the OpenJPA metamodel annotation processor configuration to the target project.
     * <p>
     * Registers the {@code openjpa} extension container (if not already present),
     * wires per-source-set compiler arguments derived from
     * {@link persist.openjpa.gradle.extension.MetaModelExtension}, and provisions the
     * default {@code org.apache.openjpa:openjpa} dependency on the
     * {@code modelgenProcessor} configuration when no user override is present.
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

        project.extensions.getByType(SourceSetContainer).configureEach { sourceSet ->
            OpenJPAExtension extension = container.maybeCreate(sourceSet.name)

            // Expose annotation processor output directory to the IDE compiler paths
            sourceSet.java.srcDir(project.layout.buildDirectory.dir("generated/sources/annotationProcessor/java/${sourceSet.name}"))

            project.tasks.named(sourceSet.compileJavaTaskName, JavaCompile) { compileTask ->
                compileTask.options.compilerArgs.addAll(project.provider {
                    List<String> args = []

                    def metaModel = extension.metaModel

                    metaModel.enabled.orNull?.with { value -> args.add("-Aopenjpa.metamodel=${value}") }
                    metaModel.logLevel.orNull?.with { value -> args.add("-Aopenjpa.log=${value}") }
                    metaModel.source.orNull?.with { value -> args.add("-Aopenjpa.source=${value}") }
                    metaModel.naming.orNull?.with { value -> args.add("-Aopenjpa.naming=${value}") }
                    metaModel.header.orNull?.with { value -> args.add("-Aopenjpa.header=${value}") }
                    metaModel.addGeneratedAnnotation.orNull?.with { value -> args.add("-Aopenjpa.addGeneratedAnnotation=${value}") }

                    return args
                }.get())
            }
        }

        // Apply OpenJPA annotation processor fallback coordination logic
        project.afterEvaluate {
            def globalConfigName = 'modelgenProcessor'
            if (project.configurations.names.contains(globalConfigName)) {
                project.configurations.named(globalConfigName).configure { config ->
                    if (config.dependencies.isEmpty()) {
                        project.dependencies.add(globalConfigName, 'org.apache.openjpa:openjpa')
                    }
                }
            }
        }
    }

}
