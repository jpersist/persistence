package persist.eclipse.gradle.plugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import persist.eclipse.gradle.task.EclipseWeaveTask
import spock.lang.Specification

class EclipsePersistencePluginSpec extends Specification {

    def "plugin applies successfully and registers extension and tasks"() {
        given: "A target Gradle project"
        Project project = ProjectBuilder.builder().build()

        when: "The java plugin and our eclipse-persistence plugin are applied"
        project.plugins.apply('java')
        project.plugins.apply('io.github.jpersist.eclipse-persistence')

        then: "The annotation processor configuration is registered"
        project.configurations.named('annotationProcessor') != null

        and: "The weave task is registered with the correct type"
        def task = project.tasks.named("eclipseWeaveClasses")
        task != null
        task.isPresent()
        task.get() instanceof EclipseWeaveTask
    }

}
