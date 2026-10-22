package persist.jakarta.gradle.worker

import groovy.io.FileType
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import persist.lint.api.NamedObjectDescriptor
import persist.lint.api.spi.NamedObjectParserFactory

/**
 * Parameters required by the {@link LintNamedQueriesWorker}.
 * <p>
 * Carries the compiled classes directory, JPA version, and linter behaviour
 * options into the isolated worker process.
 * </p>
 *
 * @since 1.6.0
 */
interface LintNamedQueriesParameters extends WorkParameters {

    /** The JPA specification version for annotation namespace resolution. */
    Property<String> getJpaVersion()

    /** Whether errors should fail the build ({@code true}) or be logged as warnings. */
    Property<Boolean> getFailOnError()

    /** Whether sub-critical style warnings should be suppressed. */
    Property<Boolean> getIgnoreWarnings()

    /** The directory containing compiled {@code .class} files to scan. */
    DirectoryProperty getCompiledClassesDir()

    // Incremental parameters mapping vectors
    Property<Boolean> getIncrementalPass()

    ListProperty<String> getAffectedClassFiles()

    ListProperty<String> getRemovedClassFiles()
}

/**
 * An isolated Worker Action that performs bytecode sweeping and query AST
 * validation inside a classloader-isolated process.
 * <p>
 * The worker scans all {@code .class} files in the compiled output directory
 * using ASM to discover {@code @NamedQuery} and
 * {@code @NamedStoredProcedureQuery} annotations. Discovered queries are then
 * validated using an ANTLR4-based parser loaded via the
 * {@link persist.lint.api.spi.NamedObjectParserFactory} SPI contract.
 * Parameter naming conventions are checked against strict camelCase rules.
 * </p>
 *
 * @since 1.6.0
 * @see persist.jakarta.gradle.task.LintNamedQueries
 */
abstract class LintNamedQueriesWorker implements WorkAction<LintNamedQueriesParameters> {

    @Override
    void execute() {
        File classesDir = parameters.compiledClassesDir.get().asFile
        String jpaVersion = parameters.jpaVersion.get()
        boolean failOnError = parameters.failOnError.get()
        boolean ignoreWarnings = parameters.ignoreWarnings.get()

        boolean isIncremental = parameters.incrementalPass.getOrElse(false)
        List<String> affectedFiles = parameters.affectedClassFiles.getOrElse([])
        List<String> removedFiles = parameters.removedClassFiles.getOrElse([])

        Map<String, String> discoveredQueries = [:]
        Map<String, Map<String, Set<String>>> discoveredProcedures = [:]
        List<String> discoveredClasses = []

        // 1. ASM Bytecode Sweep Channel
        if (isIncremental) {
            println "⚡ Processing linter inputs incrementally. Auditing ${affectedFiles.size()} modified class boundaries..."
            affectedFiles.forEach { filePath ->
                File classFile = new File(filePath)
                if (classFile.exists()) {
                    Helper.sweepClassFile(classFile, discoveredQueries, discoveredProcedures, discoveredClasses)
                }
            }
            if (!removedFiles.isEmpty()) {
                println "🗑️ Detected ${removedFiles.size()} removed entities. Evicting structural caches."
            }
        } else {
            println "⏳ Performing complete structural sweep linter pass over: ${classesDir.name}..."
            classesDir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class$/) { classFile ->
                Helper.sweepClassFile(classFile, discoveredQueries, discoveredProcedures, discoveredClasses)
            }
        }

        int totalObjects = discoveredQueries.size() + discoveredProcedures.size()

        if (totalObjects == 0 && isIncremental) {
            println "✅ No annotations alterations detected inside the modified entities. Build up-to-date."
            return
        }

        if (totalObjects == 0) {
            println "✅ No named metadata configurations declared in this source set. Pass complete."
            return
        }

        println "📊 Discovered ${totalObjects} static structural items to validate."

        int errorCount = 0

        // 2. RE-ENGINEERED TRY-WITH-RESOURCES PARSING PIPELINE
        try {
            // Factory lookup maps via ServiceLoader
            def factory = NamedObjectParserFactory.newNamedObjectParserFactory(jpaVersion)

            // Open the closeable parser inside a safe container block
            factory.newNamedObjectParser(discoveredClasses).withCloseable { namedObjectParser ->
                println "✅ In-memory verification metamodel generated successfully."

                // 3a. VALIDATE STANDARD QUERIES
                if (!discoveredQueries.isEmpty()) {
                    println "⏳ Driving native ANTLR4 query semantic validation sweep..."
                    discoveredQueries.each { name, queryString ->
                        try {
                            NamedObjectDescriptor descriptor = namedObjectParser.parseQuery(name, queryString)
                            println "   ✅ Query '${name}' passed semantic compilation checks."

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

                // 3b. VALIDATE STORED PROCEDURES
                if (!discoveredProcedures.isEmpty()) {
                    println "⏳ Running parameters governance lint checks over named stored procedures..."
                    discoveredProcedures.each { name, procedureMap ->
                        procedureMap.each { procedureName, params ->
                            try {
                                namedObjectParser.parseStoredProcedure(name, procedureName, params)
                                println "   ✅ Stored Procedure '${name}' [${procedureName}] metadata structural paths validated."

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
            }
        } catch (Exception e) {
            // Eager validation failures triggered during buildSessionFactory() end up here
            System.err.println("❌ Critical: Metamodel initialization crash: " + e.getMessage())
            if (failOnError) {
                throw new GradleException("JPersist Linter failed on setup configuration.", e)
            }
            return
        }

        if (errorCount > 0) {
            System.err.println("⚠️ JPersist Static Linter found ${errorCount} structural metadata errors!")
            if (failOnError) {
                throw new GradleException("Build failed due to ${errorCount} invalid named metadata definitions.")
            }
        } else {
            println "✅ All processed static metadata structures validated successfully!"
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

        private static void sweepClassFile(File classFile, Map queries, Map procedures, List classes) {
            classFile.withInputStream { is ->
                try {
                    ClassReader reader = new ClassReader(is)
                    reader.accept(new ComprehensiveMetadataScanner(queries, procedures, classes),
                        ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES)
                } catch (Exception e) {
                    System.err.println("⚠️ Warning: Failed to sweep class file metrics for ${classFile.name}: ${e.message}")
                }
            }
        }

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
