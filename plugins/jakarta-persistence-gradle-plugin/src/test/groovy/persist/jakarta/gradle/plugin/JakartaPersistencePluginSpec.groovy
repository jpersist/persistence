package persist.jakarta.gradle.plugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import persist.jakarta.gradle.task.ProcessPersistenceDescriptor
import spock.lang.Specification

class JakartaPersistencePluginSpec extends Specification {

    def "plugin applies successfully and registers extension and tasks"() {
        given: "A target Gradle project"
        Project project = ProjectBuilder.builder().build()

        when: "The java plugin and our jakarta-persistence plugin are applied"
        project.plugins.apply('io.github.jpersist.jpa')

        then: "The persistence extension is initialized"
        project.extensions.getByName('persistence') != null

        and: "The jpa configuration is registered"
        project.configurations.named('jpa') != null

        and: "The jar file configuration is registered"
        project.configurations.named('jarFile') != null

        and: "The process persistence descriptor task is registered with the correct type"
        def task = project.tasks.named("processPersistenceDescriptor")
        task != null
        task.isPresent()
        task.get() instanceof ProcessPersistenceDescriptor
    }

}
