package persist.hibernate.gradle.plugin

import org.gradle.testkit.runner.TaskOutcome

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.gradle.testkit.runner.GradleRunner

import spock.lang.Specification
import spock.lang.TempDir

import java.util.jar.JarFile

class HibernatePersistencePluginFunctionalSpec extends Specification {

    // Isolated sandbox directory refreshed before every feature method
    @TempDir
    Path projectDir

    def setup() {
        def resourceUrl = getClass().classLoader.getResource("test-hibernate-persistence-module")
        assert resourceUrl != null : "The resource template 'test-hibernate-persistence-module' could not be found!"

        Path sourceTemplateDir = Paths.get(resourceUrl.toURI())

        // Walk the static directory tree and copy files into our execution sandbox
        Files.walk(sourceTemplateDir).forEach { sourcePath ->
            Path targetPath = projectDir.resolve(sourceTemplateDir.relativize(sourcePath))
            if (Files.isDirectory(sourcePath)) {
                Files.createDirectories(targetPath)
            } else {
                Files.copy(sourcePath, targetPath)
            }
        }
    }

    def "plugin generates jpa metamodel and bundles it inside the jar archive"() {
        given:
        def runner = createRunner()

        when:
        def result = runner.build()

        then:
        result.task(":compileJava").outcome == TaskOutcome.SUCCESS
        result.task(":compileTestJava").outcome == TaskOutcome.NO_SOURCE
        result.task(":hibernateEnhanceClasses").outcome == TaskOutcome.SUCCESS
        result.task(":hibernateEnhanceTestClasses").outcome == TaskOutcome.NO_SOURCE
        result.task(":jar").outcome == TaskOutcome.SUCCESS

        // Asserting against real logs intercepted from enhancement task
        result.output.contains('Successfully enhanced class : org.hibernate.persistence.entity.Person.class')
        result.output.contains('Skipping class : org.hibernate.persistence.entity.Person_.class')
        result.output.contains('Skipping class : org.hibernate.persistence.entity.Person$Gender.class')

        // Verify distribution archive layout paths
        File jarFile = new File(projectDir.toFile(), "build/libs/test-hibernate-persistence-module.jar")
        jarFile.exists()

        and: "The generated static metamodel companion class file is packed"
        JarFile archive = new JarFile(jarFile)
        archive.getJarEntry("org/hibernate/persistence/entity/Person_.class") != null
        archive.close()
    }

    def "plugin applies successfully and builds downstream tasks with configuration cache support"() {
        given:
        def runner = createRunner()

        when: "First execution path checks compilation graph configurations"
        def firstResult = runner.build()

        then:
        firstResult.task(":compileJava").outcome == TaskOutcome.SUCCESS
        firstResult.task(":compileTestJava").outcome == TaskOutcome.NO_SOURCE
        firstResult.task(":hibernateEnhanceClasses").outcome == TaskOutcome.SUCCESS
        firstResult.task(":hibernateEnhanceTestClasses").outcome == TaskOutcome.NO_SOURCE
        firstResult.task(":jar").outcome == TaskOutcome.SUCCESS
        firstResult.output.contains("Configuration cache entry stored.")

        when: "Second run with zero modification boundaries"
        def secondResult = runner.build()

        then: "Incremental tracking bypasses overhead execution loops cleanly"
        secondResult.output.contains("Configuration cache entry reused.")
        secondResult.task(":compileJava").outcome == TaskOutcome.UP_TO_DATE
        secondResult.task(":compileTestJava").outcome == TaskOutcome.NO_SOURCE
        secondResult.task(":hibernateEnhanceClasses").outcome == TaskOutcome.UP_TO_DATE
        secondResult.task(":hibernateEnhanceTestClasses").outcome == TaskOutcome.NO_SOURCE
        secondResult.task(":jar").outcome == TaskOutcome.UP_TO_DATE
    }

    private GradleRunner createRunner() {
        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments('jar', '--configuration-cache', '--stacktrace')
            .withPluginClasspath()
    }

}
