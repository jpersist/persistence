package persist.eclipse.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class EclipsePersistencePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(EclipseJpaModelgenPlugin)
        project.plugins.apply(EclipseStaticWeavePlugin)
    }

}
