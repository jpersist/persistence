package persist.jakarta.gradle.plugin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import persist.jakarta.gradle.task.GenerateJPAGraalVMMetadata
import persist.jakarta.gradle.task.LintNamedQueries
import persist.jakarta.gradle.task.ProcessPersistenceDescriptor
import persist.jakarta.gradle.task.ValidatePersistenceSchema
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

        and: "The modelgen processor configuration is registered"
        project.configurations.named('modelgenProcessor') != null

        and: "All tasks are registered with the correct type"
        Map<String, Class<?>> taskMap = [
            "processPersistenceDescriptor": ProcessPersistenceDescriptor,
            "generateJPAGraalVMMetadata": GenerateJPAGraalVMMetadata,
            "validatePersistenceSchema": ValidatePersistenceSchema,
            "lintNamedQueries": LintNamedQueries
        ]
        def tasks = project.tasks.matching { taskMap.containsKey(it.name) }
        tasks.size() == taskMap.size()
        tasks.each { task ->
            assert taskMap.get(task.name).isInstance(task)
        }
    }

}
