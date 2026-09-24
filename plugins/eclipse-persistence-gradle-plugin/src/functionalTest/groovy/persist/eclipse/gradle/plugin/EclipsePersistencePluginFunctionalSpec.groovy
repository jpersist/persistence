package persist.eclipse.gradle.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile

class EclipsePersistencePluginFunctionalSpec extends Specification {

    // Isolated sandbox directory refreshed before every feature method
    @TempDir
    Path projectDir

    def setup() {
        def resourceUrl = getClass().classLoader.getResource('test-eclipse-persistence-module')
        assert resourceUrl != null : "The resource template 'test-eclipse-persistence-module' could not be found!"

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

    def "plugin executes eclipseWeaveClasses task successfully and modifies classes"() {
        given:
        def runner = createRunner()

        when:
        def result = runner.build()

        then:
        // Verify that both compilation and our custom weaving tasks run successfully
        result.task(":compileJava").outcome == TaskOutcome.SUCCESS
        result.task(":eclipseWeaveClasses").outcome == TaskOutcome.SUCCESS
        result.task(":eclipseWeaveTestClasses").outcome == TaskOutcome.NO_SOURCE
        result.task(":jar").outcome == TaskOutcome.SUCCESS

        // Asserting against real logs intercepted from EclipseLink processing
        result.output.contains("The access type for the persistent class [class org.eclipse.persistence.entity.Person] is set to [FIELD]")
        result.output.contains("The alias name for the entity class [class org.eclipse.persistence.entity.Person] is being defaulted to: Person")

        // Verify that downstream distribution output file contains our class
        File jarFile = new File(projectDir.toFile(), 'build/libs/test-eclipse-persistence-module.jar')
        jarFile.exists()

        and: "The compiled class is present in the artifact package structure"
        JarFile archive = new JarFile(jarFile)
        archive.getJarEntry('org/eclipse/persistence/entity/Person.class') != null

        and: "The generated static metamodel companion class file is packed"
        archive.getJarEntry('org/eclipse/persistence/entity/Person_.class') != null
        archive.close()
    }

    def "plugin supports incremental builds and respects UP_TO_DATE task checks"() {
        given:
        def runner = createRunner()

        when: "First execution calculates and caches build graphs"
        def firstResult = runner.build()

        then:
        firstResult.task(":eclipseWeaveClasses").outcome == TaskOutcome.SUCCESS
        firstResult.task(":eclipseWeaveTestClasses").outcome == TaskOutcome.NO_SOURCE
        firstResult.output.contains("Configuration cache entry stored.")

        when: "Second execution with no code changes"
        def secondResult = runner.build()

        then: "Every step checks green and is safely bypassed"
        secondResult.output.contains("Configuration cache entry reused.")
        secondResult.task(":compileJava").outcome == TaskOutcome.UP_TO_DATE
        secondResult.task(":eclipseWeaveClasses").outcome == TaskOutcome.UP_TO_DATE
        secondResult.task(":eclipseWeaveTestClasses").outcome == TaskOutcome.NO_SOURCE
        secondResult.task(":jar").outcome == TaskOutcome.UP_TO_DATE
    }

    private GradleRunner createRunner() {
        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments('jar', '--configuration-cache', '--stacktrace')
            .withPluginClasspath()
    }

}
