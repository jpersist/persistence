package persist.openjpa.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import persist.jakarta.gradle.plugin.JakartaPersistencePlugin

/**
 * An aggregate utility plugin that configures a comprehensive OpenJPA toolbelt out-of-the-box.
 * <p>
 * Applying this plugin serves as a single macro shortcut that automatically triggers the registration
 * and orchestration parameters of the Jakarta Persistence descriptor generator
 * ({@link persist.jakarta.gradle.plugin.JakartaPersistencePlugin}), the metamodel generator framework
 * ({@link OpenJPAMetaModelGenerationPlugin}), and the compile-time bytecode enhancement framework
 * ({@link OpenJPAEnhancementPlugin}).
 * </p>
 * <p>
 * This allows multi-module architecture projects to establish complete JPA standard baselines using
 * an elegant single-line plugin layout declaration.
 * </p>
 *
 * @since 1.8.0
 */
class OpenJPAPersistencePlugin implements Plugin<Project> {

    /**
     * Applies the Jakarta Persistence, OpenJPA Enhancement, and OpenJPA MetaModel Generation
     * plugins to the target project.
     *
     * @param project The target Gradle project.
     */
    @Override
    void apply(Project project) {
        project.plugins.apply(JakartaPersistencePlugin)
        project.plugins.apply(OpenJPAEnhancementPlugin)
        project.plugins.apply(OpenJPAMetaModelGenerationPlugin)
    }

}
