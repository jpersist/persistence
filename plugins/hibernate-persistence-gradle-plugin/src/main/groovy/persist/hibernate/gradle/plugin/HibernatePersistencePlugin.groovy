package persist.hibernate.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.hibernate.orm.tooling.gradle.HibernateOrmPlugin
import org.hibernate.orm.tooling.gradle.HibernateOrmSpec

class HibernatePersistencePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(HibernateJpamodelgenPlugin)
        project.plugins.apply(HibernateOrmPlugin)

        project.plugins.withType(HibernateOrmPlugin).configureEach {
            def hibernate = project.extensions.getByType(HibernateOrmSpec)
            hibernate.enhancement { enhancement ->
                enhancement.enableLazyInitialization.convention(true)
                enhancement.enableDirtyTracking.convention(true)
                enhancement.enableAssociationManagement.convention(true)
                enhancement.enableExtendedEnhancement.convention(false)
            }
        }
    }

}
