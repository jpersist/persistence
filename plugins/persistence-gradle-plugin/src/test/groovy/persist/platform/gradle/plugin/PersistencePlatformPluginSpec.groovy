package persist.platform.gradle.plugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class PersistencePlatformPluginSpec extends Specification {

    def "plugin applies successfully and registers 'persistence' configuration"() {
        given: "A target Gradle project"
        Project project = ProjectBuilder.builder().build()

        when: "The java plugin and our persistence-platform plugin are applied"
        project.plugins.apply('java')
        project.plugins.apply('io.github.jpersist.persistence-platform')

        then: "The persistence configuration is registered"
        project.configurations.named('persistence') != null
    }

}
