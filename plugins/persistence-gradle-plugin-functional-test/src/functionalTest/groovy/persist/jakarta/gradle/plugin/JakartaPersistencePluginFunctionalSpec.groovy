package persist.jakarta.gradle.plugin

import groovy.xml.XmlParser
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class JakartaPersistencePluginFunctionalSpec extends Specification {

    @TempDir
    Path testProjectDir

    def setup() {
        def resourceUrl = getClass().classLoader.getResource("test-multi-module-project")
        assert resourceUrl != null : "The resource template 'test-multi-module-project' could not be found!"

        Path sourceTemplateDir = Paths.get(resourceUrl.toURI())

        // Walk the static directory tree and copy files into our execution sandbox
        Files.walk(sourceTemplateDir).forEach { sourcePath ->
            Path targetPath = testProjectDir.resolve(sourceTemplateDir.relativize(sourcePath))
            if (Files.isDirectory(sourcePath)) {
                Files.createDirectories(targetPath)
            } else {
                Files.copy(sourcePath, targetPath)
            }
        }
    }

    def "should generate a valid persistence.xml from scratch using Extension DSL"() {
        given: "a build script utilizing the persistence extension"
        def runner = createRunner()

        when: "running the classes task"
        def result = runner.build()

        then: "the build succeeds and generates the correct xml file structural bindings"
        result.task(":common-persistence-module:processPersistenceDescriptor").outcome.toString() == "SUCCESS"

        File outputFile = new File(testProjectDir.toFile(), "common-persistence-module/build/resources/main/META-INF/persistence.xml")
        outputFile.exists()

        and: "the content matches the extension definitions precisely"
        def parser = new XmlParser(false, false)
        Node root = parser.parse(outputFile)
        root.attribute("version") == "3.0"

        Node unit = (Node) ((List<?>) root.get("persistence-unit"))[0]
        unit.attribute("name") == "common-persistence-unit"
        unit.attribute("transaction-type") == "RESOURCE_LOCAL"
        // ((Node) ((List<?>) unit.get("description"))[0]).text() == "Generated unit context"
        ((Node) ((List<?>) unit.get("provider"))[0]).text() == "org.hibernate.jpa.HibernatePersistenceProvider"
        // ((Node) ((List<?>) unit.get("non-jta-data-source"))[0]).text() == "jdbc/testDb"

        List<?> classesList = (List<?>) unit.get("class")
        classesList.size() == 1
        ((Node) classesList.get(0)).text() == "com.example.entity.Identifiable"

        Node props = (Node) ((List<?>) unit.get("properties"))[0]
        Node prop = (Node) ((List<?>) props.get("property"))[0]
        prop.attribute("name") == "hibernate.show_sql"
        prop.attribute("value") == "true"
    }

    def "should intelligently merge configuration values into an existing template persistence.xml"() {
        given: "a template persistence.xml in the source resources directory"
        def runner = createRunner()

        when: "executing the build"
        def result = runner.build()

        then: "the build succeeds and outputs a merged layout"
        result.task(":bookstore-persistence-module:processPersistenceDescriptor").outcome.toString() == "SUCCESS"

        File outputFile = new File(testProjectDir.toFile(), "bookstore-persistence-module/build/resources/main/META-INF/persistence.xml")
        outputFile.exists()

        and: "values from both the template and the extension are combined correctly"
        def parser = new XmlParser(false, false)
        Node root = parser.parse(outputFile)

        Node unit = (Node) ((List<?>) root.get("persistence-unit"))[0]
        // ((Node) ((List<?>) unit.get("description"))[0]).text() == "User Template Description" // Maintained from user XML
        ((Node) ((List<?>) unit.get("provider"))[0]).text() == "org.hibernate.jpa.HibernatePersistenceProvider" // Patched from Extension

        and: "the newly resolved project dependency jar file is injected dynamically"
        List<?> jarFilesList = (List<?>) unit.get("jar-file")
        jarFilesList.size() == 1
        ((Node) jarFilesList.get(0)).text() == "common-persistence-module-0.1-SNAPSHOT.jar"

        and: "the discovered managed classes are injected dynamically"
        List<?> classesList = (List<?>) unit.get("class")
        classesList.size() == 2
        ((Node) classesList.get(0)).text() == "com.example.bookstore.entity.Author"
        ((Node) classesList.get(1)).text() == "com.example.bookstore.entity.Book"

        and: "non-conflicting template parameters such as native properties are safely preserved"
        Node props = (Node) ((List<?>) unit.get("properties"))[0]
        List<?> propertyNodes = (List<?>) props.get("property")

        // Assert both properties coexist peacefully
        propertyNodes.find { ((Node) it).attribute("name") == "hibernate.hbm2ddl.auto" && ((Node) it).attribute("value") == "update" } != null
        propertyNodes.find { ((Node) it).attribute("name") == "hibernate.show_sql" && ((Node) it).attribute("value") == "false" } != null
    }

    private GradleRunner createRunner() {
        def jacocoAgentJvmArg = System.getProperty('jacocoAgentJvmArg')
        def jacocoDestFile = System.getProperty('jacocoDestFile')

        // Configuration cache is incompatible with Java agents in TestKit builds,
        // so it must be disabled when the JaCoCo agent is active for coverage collection
        def arguments = jacocoAgentJvmArg
            ? ['classes', '--stacktrace', '--no-configuration-cache']
            : ['classes', '--stacktrace', '--configuration-cache']

        def runner = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
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
