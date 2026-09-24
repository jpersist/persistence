package persist.hibernate.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * An aggregate utility plugin that configures a complete, modern corporate standard Hibernate
 * persistence pipeline out-of-the-box.
 * <p>
 * Applying this single identifier macro shortcut automatically applies and synchronizes both
 * the static criteria metamodel generator ({@link HibernateJpamodelgenPlugin}) and the isolated
 * programmatic bytecode transformation task lifecycle ({@link HibernateEnhancementPlugin}).
 * </p>
 */
class HibernatePersistencePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(HibernateJpamodelgenPlugin)
        project.plugins.apply(HibernateEnhancementPlugin)
    }

}
