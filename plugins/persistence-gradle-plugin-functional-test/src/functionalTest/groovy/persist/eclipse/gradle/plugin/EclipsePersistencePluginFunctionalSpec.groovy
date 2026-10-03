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

    // Indicates whether the JaCoCo agent is active for coverage collection
    static final boolean JACOCO_ACTIVE = System.getProperty('jacocoAgentJvmArg') != null

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

    def "plugin auto-registers existing persistence units from file and executes successfully"() {
        given: "a test runner executing against the pre-loaded resource project template"
        def runner = createRunner()

        when: "building the project lifecycle milestones"
        def result = runner.build()

        then: "the core java compilation and processing tasks pass cleanly"
        result.task(":compileJava").outcome == TaskOutcome.SUCCESS
        result.task(":eclipseWeaveClasses").outcome == TaskOutcome.SUCCESS
        result.task(":eclipseWeaveTestClasses").outcome == TaskOutcome.NO_SOURCE
        result.task(":jar").outcome == TaskOutcome.SUCCESS

        and: "the persistence descriptor task shifts from SKIPPED to SUCCESS due to auto-registration"
        // Previously, this evaluated to SKIPPED because the DSL container was empty.
        // Now, it detects the unit declared in the resource project's template file,
        // initializes it, runs the ASM scanner, and outputs a success marker.
        result.task(":processPersistenceDescriptor").outcome == TaskOutcome.SUCCESS

        and: "the dynamic ASM scanner has cleanly extracted and injected compilation bytecode annotations"
        // Read the final processed build output artifact descriptor file
        File processedXmlFile = new File(projectDir.toFile(), 'build/generated/resources/main/META-INF/persistence.xml')
        assert processedXmlFile.exists()

        String xmlContent = processedXmlFile.text

        // Assert that the processor automatically injected the discovered entity class
        // into the parsed persistence unit structure block
        assert xmlContent.contains("<class>org.eclipse.persistence.entity.Person</class>")

        and: "version and namespace are for the right descriptor processor delegate"
        assert xmlContent.contains("version=\"2.2\"")
        assert xmlContent.contains("xmlns=\"http://xmlns.jcp.org/xml/ns/persistence\"")

        and: "standard logs intercepted from EclipseLink processing are maintained"
        result.output.contains("The access type for the persistent class [class org.eclipse.persistence.entity.Person] is set to [FIELD]")
        result.output.contains("The alias name for the entity class [class org.eclipse.persistence.entity.Person] is being defaulted to: Person")

        // Verify that downstream distribution output file contains our class
        File jarFile = new File(projectDir.toFile(), 'build/libs/test-eclipse-persistence-module.jar')
        assert jarFile.exists()

        and: "the compiled class is present in the final packaged archive package structure"
        JarFile archive = new JarFile(jarFile)
        assert archive.getJarEntry('org/eclipse/persistence/entity/Person.class') != null
        assert archive.getJarEntry('org/eclipse/persistence/entity/Person_.class') != null
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
        JACOCO_ACTIVE || firstResult.output.contains("Configuration cache entry stored.")

        when: "Second execution with no code changes"
        def secondResult = runner.build()

        then: "Every step checks green and is safely bypassed"
        JACOCO_ACTIVE || secondResult.output.contains("Configuration cache entry reused.")
        secondResult.task(":compileJava").outcome == TaskOutcome.SUCCESS    // Becomes up-to-date from the third build
        secondResult.task(":eclipseWeaveClasses").outcome == TaskOutcome.UP_TO_DATE
        secondResult.task(":eclipseWeaveTestClasses").outcome == TaskOutcome.NO_SOURCE
        secondResult.task(":jar").outcome == TaskOutcome.UP_TO_DATE
    }

    private GradleRunner createRunner() {
        def jacocoAgentJvmArg = System.getProperty('jacocoAgentJvmArg')
        def jacocoDestFile = System.getProperty('jacocoDestFile')

        // Configuration cache is incompatible with Java agents in TestKit builds,
        // so it must be disabled when the JaCoCo agent is active for coverage collection
        def arguments = jacocoAgentJvmArg
            ? ['jar', '--stacktrace', '--no-configuration-cache']
            : ['jar', '--stacktrace', '--configuration-cache']

        def runner = GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments(arguments)
            .withPluginClasspath()

        // Forward the JaCoCo agent to the TestKit JVM so coverage is collected on plugin code
        // Extract the agent jar path from the original arg and reconstruct with the correct
        // destfile and append=true so data merges into the functional test's .exec file
        if (jacocoAgentJvmArg) {
            def agentJar = (jacocoAgentJvmArg =~ /-javaagent:(.+?)=/)[0][1]
            runner.withJvmArguments("-javaagent:${agentJar}=destfile=${jacocoDestFile},append=true,jmx=false")
        }

        return runner
    }

}
