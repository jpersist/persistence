package persist.platform.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer

/**
 * A platform alignment plugin that allows users to declare a JPA implementation
 * platform (BOM) dependency via a dedicated {@code persistence} configuration.
 * <p>
 * Once applied, the user can specify a BOM artifact to centrally manage JPA-related
 * dependency versions across all standard Java configurations:
 * </p>
 * <pre>
 * dependencies {
 *     persistence platform('org.hibernate.orm:hibernate-platform:6.6.56.Final')
 * }
 * </pre>
 * <p>
 * The plugin automatically propagates the version constraints from the declared platform
 * into {@code implementation}, {@code compileOnly}, {@code annotationProcessor},
 * {@code runtimeOnly}, and {@code testImplementation} configurations, ensuring consistent
 * version alignment without requiring explicit version strings on individual dependencies.
 * </p>
 */
class PersistencePlatformPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)

        def persistence = project.configurations.register('persistence') { config ->
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
