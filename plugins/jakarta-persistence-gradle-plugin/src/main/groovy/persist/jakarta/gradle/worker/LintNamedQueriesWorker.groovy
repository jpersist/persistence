package persist.jakarta.gradle.worker

import groovy.io.FileType
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import persist.lint.api.NamedObjectDescriptor
import persist.lint.api.spi.NamedObjectParser
import persist.lint.api.spi.NamedObjectParserFactory

/**
 * Parameters required by the Static NamedQuery Analysis Linter Worker.
 */
interface LintNamedQueriesParameters extends WorkParameters {

    DirectoryProperty getCompiledClassesDir()

    Property<String> getJpaVersion()

    Property<Boolean> getFailOnError()

    Property<Boolean> getIgnoreWarnings()

}

/**
 * An isolated Worker Action executing bytecode sweeping and query AST validation.
 */
abstract class LintNamedQueriesWorker implements WorkAction<LintNamedQueriesParameters> {

    @Override
    void execute() {
        File classesDir = parameters.compiledClassesDir.get().asFile
        String jpaVersion = parameters.jpaVersion.get()
        boolean failOnError = parameters.failOnError.get()
        boolean ignoreWarnings = parameters.ignoreWarnings.get()

        println "⏳ Starting static query analysis linter pass over: ${classesDir.name}..."

        if (!classesDir.exists()) {
            println "✅ No compiled classes found. Skipping linter pass."
            return
        }

        Map<String, String> discoveredQueries = [:]
        // Key = Metadata Unit Identifier Name, Value = [procedureName: Set<parameterNames>]
        Map<String, Map<String, Set<String>>> discoveredProcedures = [:]
        List<String> discoveredClasses = []

        // 1. ADVANCED ASM MULTI-ANNOTATION SWEEP (QUERIES + STORED PROCEDURES)
        classesDir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class$/) { classFile ->
            classFile.withInputStream { is ->
                try {
                    ClassReader reader = new ClassReader(is)
                    reader.accept(new ComprehensiveMetadataScanner(discoveredQueries, discoveredProcedures, discoveredClasses),
                        ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES)
                } catch (Exception e) {
                    System.err.println("⚠️ Warning: Failed to sweep class file metrics for ${classFile.name}: ${e.message}")
                }
            }
        }

        int totalObjects = discoveredQueries.size() + discoveredProcedures.size()
        println "📊 Discovered ${totalObjects} static structural items to validate."

        if (totalObjects == 0) {
            println "✅ No named metadata configurations declared in this source set. Pass complete."
            return
        }

        // 2. Factory Generation via ServiceLoader
        NamedObjectParser namedObjectParser
        try {
            def factory = NamedObjectParserFactory.newNamedObjectParserFactory(jpaVersion)
            namedObjectParser = factory.newNamedObjectParser(discoveredClasses)
            println "✅ In-memory verification metamodel generated successfully."
        } catch (Exception e) {
            System.err.println("❌ Critical: Metamodel initialization crash: " + e.message)
            if (failOnError) throw new GradleException("JPersist Linter failed on setup configuration.", e)
            return
        }

        int errorCount = 0

        // 3a. COMPLETE STRONGLY-TYPED SEMANTIC & PARAMETER STYLE VALIDATION FOR QUERIES
        if (!discoveredQueries.isEmpty()) {
            println "⏳ Driving native ANTLR4 query semantic validation sweep..."
            discoveredQueries.each { name, queryString ->
                try {
                    // Execute parser and capture parameter sets
                    NamedObjectDescriptor descriptor = namedObjectParser.parseQuery(name, queryString)
                    println "   ✅ Query '${name}' passed semantic compilation checks."

                    // Style Governance Check Loop
                    if (!ignoreWarnings && descriptor.namedParameters() != null) {
                        descriptor.namedParameters().each { String param ->
                            Helper.validateParameterStyle(name, param)
                        }
                    }
                } catch (Exception e) {
                    errorCount++
                    Throwable cause = e.getCause() ?: e
                    System.err.println("   ❌ LINT ERROR in Query [${name}]: ${cause.getMessage() ?: cause.toString()}")
                }
            }
        }

        // 3b. COMPLETE PARAMETER STYLE VALIDATION FOR STORED PROCEDURES
        if (!discoveredProcedures.isEmpty()) {
            println "⏳ Running parameters governance lint checks over named stored procedures..."
            discoveredProcedures.each { name, procedureMap ->
                procedureMap.each { procedureName, params ->
                    try {
                        // Pass down seamlessly into your updated v2 SPI contract layers
                        namedObjectParser.parseStoredProcedure(name, procedureName, params)
                        println "   ✅ Stored Procedure '${name}' [${procedureName}] metadata structural paths validated."

                        // Style Governance Check Loop
                        if (!ignoreWarnings && params != null) {
                            params.each { String param ->
                                Helper.validateParameterStyle(name, param)
                            }
                        }
                    } catch (Exception e) {
                        errorCount++
                        System.err.println("   ❌ LINT ERROR in Stored Procedure [${name}]: ${e.getMessage()}")
                    }
                }
            }
        }

        if (errorCount > 0) {
            System.err.println("⚠️ JPersist Static Linter found ${errorCount} structural metadata errors!")
            if (failOnError) {
                throw new GradleException("Build failed due to ${errorCount} invalid named metadata definitions.")
            }
        } else {
            println "✅ All discovered static metadata structures validated successfully!"
        }
    }

    private static class ComprehensiveMetadataScanner extends ClassVisitor {
        private final Map<String, String> queries
        private final Map<String, Map<String, Set<String>>> procedures
        private final List<String> classes

        ComprehensiveMetadataScanner(Map<String, String> queries, Map<String, Map<String, Set<String>>> procedures, List<String> classes) {
            super(Opcodes.ASM9)
            this.queries = queries
            this.procedures = procedures
            this.classes = classes
        }

        @Override
        void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            if (name) {
                classes.add(name.replace('/', '.'))
            }
            super.visit(version, access, name, signature, superName, interfaces)
        }

        @Override
        AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor in ['Ljakarta/persistence/NamedQuery;', 'Ljavax/persistence/NamedQuery;']) {
                return new NamedQueryDataExtractor(queries)
            }
            if (descriptor in ['Ljakarta/persistence/NamedStoredProcedureQuery;', 'Ljavax/persistence/NamedStoredProcedureQuery;']) {
                return new StoredProcedureDataExtractor(procedures)
            }
            return null
        }
    }

    private static class NamedQueryDataExtractor extends AnnotationVisitor {
        private final Map<String, String> registry
        private String name = ""
        private String query = ""

        NamedQueryDataExtractor(Map<String, String> registry) { super(Opcodes.ASM9); this.registry = registry }

        @Override
        void visit(String name, Object value) {
            if (name == "name") this.name = value.toString()
            else if (name == "query") this.query = value.toString()
        }

        @Override
        void visitEnd() {
            if (name && query) registry.put(name, query)
        }
    }

    private static class StoredProcedureDataExtractor extends AnnotationVisitor {
        private final Map<String, Map<String, Set<String>>> registry
        private String name = ""
        private String procedureName = ""
        private Set<String> parameterNames = new TreeSet<>()

        StoredProcedureDataExtractor(Map<String, Map<String, Set<String>>> registry) { super(Opcodes.ASM9); this.registry = registry }

        @Override
        void visit(String name, Object value) {
            if (name == "name") this.name = value.toString()
            else if (name == "procedureName") this.procedureName = value.toString()
        }

        @Override
        AnnotationVisitor visitArray(String name) {
            if (name == "parameters") {
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    AnnotationVisitor visitAnnotation(String unnamed, String descriptor) {
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override
                            void visit(String pName, Object pValue) {
                                if (pName == "name" && pValue) parameterNames.add(pValue.toString())
                            }
                        }
                    }
                }
            }
            return null
        }

        @Override
        void visitEnd() {
            if (name && procedureName) {
                registry.put(name, [(procedureName): parameterNames])
            }
        }
    }

    private static class Helper {

        /**
         * Asserts parameter tokens against strict camelCase regular expression rules.
         */
        private static void validateParameterStyle(String contextName, String paramName) {
            boolean isCamelCase = paramName ==~ /^[a-z][a-zA-Z0-9]*$/
            if (!isCamelCase) {
                println "      ⚠️ LINTER STYLE WARNING [${contextName}]: " +
                    "Parameter ':${paramName}' breaks strict camelCase style conventions."
            }
        }

    }

}
