package persist.platform.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import persist.jakarta.gradle.plugin.JakartaPersistencePlugin

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
        project.logger.warn(
            """
            ==========================================================================
            WARNING: Plugin 'io.github.jpersist.persistence-platform' is DEPRECATED.
            It has been replaced by 'io.github.jpersist.jpa'.
            Please migrate as soon as possible.
            For migration instructions, see: https://github.com/jpersist/persistence
            ==========================================================================
            """.stripIndent());

        // Apply JakartaPersistencePlugin
        if (!project.plugins.hasPlugin(JakartaPersistencePlugin)) {
            project.plugins.apply(JakartaPersistencePlugin)
        }

        // Allow user to still use 'persistence' configuration
        def persistence = project.configurations.register('persistence') { config ->
            config.canBeConsumed = false
            config.canBeResolved = false

            config.dependencies.configureEach { dependency ->
                project.logger.warn(
                    """
                    ======================================================================
                    WARNING: The 'persistence' configuration is DEPRECATED.
                    You are adding a dependency on '${dependency.group}:${dependency.name}'.
                    Please migrate this dependency to 'jpa' instead.
                    ======================================================================
                    """.stripIndent()
                )
            }
        }

        // Delegate 'persistence' configuration to 'jpa' configuration
        project.configurations.named('jpa') {
            it.extendsFrom(persistence)
        }
    }

}
