package persist.hibernate.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class HibernatePersistencePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(HibernateJpamodelgenPlugin)
        project.plugins.apply(HibernateEnhancementPlugin)
    }

}
