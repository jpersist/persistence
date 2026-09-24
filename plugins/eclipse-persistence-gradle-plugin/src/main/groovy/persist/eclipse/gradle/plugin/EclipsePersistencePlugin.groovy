package persist.eclipse.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * An aggregate utility plugin that configures a comprehensive EclipseLink toolbelt out-of-the-box.
 * <p>
 * Applying this plugin serves as a single macro shortcut that automatically triggers the registration
 * and orchestration parameters of both the model generator framework ({@link EclipseJpaModelgenPlugin})
 * and the compile-time bytecode enhancement framework ({@link EclipseStaticWeavePlugin}).
 * </p>
 * <p>
 * This allows multi-module architecture projects to establish complete JPA standard baselines using
 * an elegant single-line plugin layout declaration.
 * </p>
 */
class EclipsePersistencePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(EclipseJpaModelgenPlugin)
        project.plugins.apply(EclipseStaticWeavePlugin)
    }

}
