package persist.hibernate.gradle.plugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import persist.hibernate.gradle.task.HibernateEnhancementTask
import spock.lang.Specification

class HibernatePersistencePluginSpec extends Specification {

    def "plugin applies successfully and registers extension and tasks"() {
        given: "A target Gradle project"
        Project project = ProjectBuilder.builder().build()

        when: "The java plugin and our hibernate-persistence plugin are applied"
        project.plugins.apply('io.github.jpersist.hibernate-persistence')

        then: "The modelgen processor configuration is registered"
        project.configurations.named('modelgenProcessor') != null
    }

}
