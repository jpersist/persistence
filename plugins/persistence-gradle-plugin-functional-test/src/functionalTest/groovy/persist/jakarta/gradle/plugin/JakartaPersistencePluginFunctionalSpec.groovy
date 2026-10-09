package persist.jakarta.gradle.plugin

import groovy.json.JsonSlurper
import groovy.xml.XmlParser
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
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
        result.task(":common-persistence-module:processPersistenceDescriptor").outcome == TaskOutcome.SUCCESS

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

        and: "the generated XML lines strictly follow the user customized two-space indentation tier"
        List<String> lines = outputFile.readLines("UTF-8")
        assert lines.size() > 1

        lines.eachWithIndex { String line, int index ->
            if (index == 0) return // Skip the XML header declaration block

            // Ensure no trailing spaces exist
            assert !line.endsWith(" ") : "Line ${index + 1} has trailing whitespace"

            // FIX: Explicitly extract the matched string fragment using [0]
            def matcher = (line =~ /^\s*/)
            String leadingSpaces = matcher[0]

            // Assert that the indentation is strictly a multiple of 2 spaces
            assert leadingSpaces.length() % 2 == 0 : "Line ${index + 1} indentation is not a multiple of 2 spaces: '${line}'"

            // Assert it follows the customized 2-space layout exactly
            if (line.contains("<persistence-unit")) {
                assert leadingSpaces.length() == 2 : "Persistence unit tag was not indented by exactly 2 spaces: '${line}'"
            }
            if (line.contains("<provider")) {
                assert leadingSpaces.length() == 4 : "Nested provider tag was not indented by exactly 4 spaces: '${line}'"
            }
        }
    }

    def "should intelligently merge configuration values into an existing template persistence.xml"() {
        given: "a template persistence.xml in the source resources directory"
        def runner = createRunner()

        when: "executing the build"
        def result = runner.build()

        then: "the build succeeds and outputs a merged layout"
        result.task(":bookstore-persistence-module:processPersistenceDescriptor").outcome == TaskOutcome.SUCCESS

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

        and: "the generated XML is pretty-printed with four-space indentation and no trailing spaces"
        List<String> lines = outputFile.readLines("UTF-8")

        // 1. Ensure the document isn't condensed onto a single line
        assert lines.size() > 1

        lines.eachWithIndex { String line, int index ->
            // Skip the root XML declaration declaration line <?xml ...?>
            if (index == 0) return

            // 2. Strict Trailing Spaces Verification
            // Checks that no line ends with a space or a hidden carriage return (\r)
            assert !line.endsWith(" ") : "Line ${index + 1} has trailing whitespace: '${line}'"
            assert !line.endsWith("\r") : "Line ${index + 1} contains unexpected CR line endings"

            // 3. Strict 4-Space Indentation Level Verification
            // Captures all leading whitespaces on the line
            String leadingSpaces = (line =~ /^\s*/)[0]

            // Asserts that the indentation is strictly a multiple of 4 spaces
            assert leadingSpaces.length() % 4 == 0 : "Line ${index + 1} indentation is not a multiple of 4 spaces: '${line}'"

            // Asserts that no hard literal tabs (\t) are used for padding
            assert !leadingSpaces.contains("\t") : "Line ${index + 1} contains hard tab characters instead of spaces"
        }
    }

    def "should inject custom jar file location prefixes when namespaced attributes are declared"() {
        given: "a template persistence.xml in the source resources directory"
        def runner = createRunner()

        when: "executing the build"
        def result = runner.build()

        then: "the build succeeds and outputs a merged layout"
        result.task(":bookstore-service-module:compileJava").outcome == TaskOutcome.SUCCESS
        result.task(":bookstore-service-module:eclipseWeaveClasses").outcome == TaskOutcome.SUCCESS
        result.task(":bookstore-service-module:processPersistenceDescriptor").outcome == TaskOutcome.SUCCESS

        File outputFile = new File(testProjectDir.toFile(), "bookstore-service-module/build/resources/main/META-INF/persistence.xml")
        outputFile.exists()

        and: "the newly resolved project dependency jar file are injected to the right location"
        def parser = new XmlParser(false, false)
        Node root = parser.parse(outputFile)

        Node unit = (Node) ((List<?>) root.get("persistence-unit"))[0]
        List<?> jarFilesList = (List<?>) unit.get("jar-file")

        // Assert that the generated file contains exactly 2 entries matching the custom path format
        jarFilesList.size() == 2

        ((Node) jarFilesList.get(0)).text() == "lib/common-persistence-module-0.1-SNAPSHOT.jar"
        ((Node) jarFilesList.get(1)).text() == "lib/bookstore-persistence-module-0.1-SNAPSHOT.jar"
    }

    def "should automatically generate a valid GraalVM reflect-config.json metadata file"() {
        given: "a standard build execution context"
        def runner = createRunner()

        when: "running the classes compilation and processing pipeline"
        def result = runner.build()

        then: "the core task and our newly introduced GraalVM task execute successfully"
        result.task(":common-persistence-module:processPersistenceDescriptor").outcome == TaskOutcome.SUCCESS
        result.task(":common-persistence-module:generateJPAGraalVMMetadata").outcome == TaskOutcome.SUCCESS

        and: "the target reflect-config.json file is written to the correct namespaced location"
        // Note: 'unspecified' is used here since the test-multi-module template project group isn't declared
        File jsonFile = new File(testProjectDir.toFile(), "common-persistence-module/build/resources/main/META-INF/native-image/com/example/common-persistence-module/reflect-config.json")
        assert jsonFile.exists()

        and: "the generated JSON contents follow the required GraalVM reflection AOT structural contract"
        String jsonContent = jsonFile.text

        // Parse using Groovy's built-in safe JSON slurper to validate structural semantics
        def jsonSlurper = new JsonSlurper()
        List<Map<String, Object>> entries = (List<Map<String, Object>>) jsonSlurper.parseText(jsonContent)

        // Verify that exactly 1 entry exists matching our ASM discovered entity class footprint
        assert entries.size() == 1

        Map<String, Object> entityRecord = entries.find { it.name == "com.example.entity.Identifiable" }
        assert entityRecord != null
        assert entityRecord.allDeclaredConstructors == true
        assert entityRecord.allDeclaredFields == true
        assert entityRecord.allDeclaredMethods == true
    }

    def "should fail the build when the JPA entity configuration does not align with the database schema"() {
        given: "a project layout with an active persistence unit configured for verification"
        def runner = createRunner()
        runner.withArguments(":test-validate-persistence-module:validatePersistenceSchema", "--stacktrace")

        when: "executing the schema validation task against a blank in-memory database configuration"
        // We use runner.buildAndFail() because a successful guard MUST fail the build
        // when a structural mismatch is explicitly detected.
        def result = runner.buildAndFail()

        then: "the task outcome evaluates to FAILED"
        result.task(":test-validate-persistence-module:validatePersistenceSchema").outcome == TaskOutcome.FAILED

        and: "the intercepted console error logs contain explicit Hibernate schema validation traces"
        result.output.contains("Initiating automated schema validation guard for unit: 'failing-validation-unit'")
        result.output.contains("SchemaManagementException") || result.output.contains("Schema-validation: missing table")
    }

    def "should successfully execute lintNamedQueries task and fail the build when an invalid property typo is detected inside a @NamedQuery definition"() {
        given: "a workspace module with a compiled JPA entity containing an intentional typo inside a @NamedQuery"
        // Since our task hooks natively into the 'check' phase lifecycle, running compileJava prepares the classes,
        // and then the linter task kicks in immediately to parse the bytecode structures.
        def runner = createRunner()
        runner.withArguments(":test-lint-persistence-module:compileJava", ":test-lint-persistence-module:lintNamedQueries", "--stacktrace")

        when: "triggering a full build or explicit verification check lifecycle pass"
        // Use buildAndFail() to explicitly verify that a linting failure code 1 is propagated correctly
        def result = runner.buildAndFail()

        then: "the lintNamedQueries task execution is attempted and accurately marks the build state as a failure"
        result.task(":test-lint-persistence-module:lintNamedQueries").outcome == TaskOutcome.FAILED

        and: "the output console logs contain the explicit semantic compilation mismatch details parsed by the ANTLR4 engine"
        assert result.output.contains("❌ LINT ERROR in Query [InventoryItem.findByInvalidProperty]")
        assert result.output.contains("Reason:")
        // Verify that Hibernate's internal SQM compiler explicitly calls out the unresolvable property typo node
        assert result.output.contains("Could not resolve attribute 'itemCode' of 'com.example.model.InventoryItem'")
        assert result.output.contains("Build failed due to 1 invalid @NamedQuery syntax definitions.")
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
