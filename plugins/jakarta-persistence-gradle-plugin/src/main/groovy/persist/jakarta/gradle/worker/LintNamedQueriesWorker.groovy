package persist.jakarta.gradle.worker

import groovy.io.FileType
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
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

    /**
     * Whether this execution is an incremental delta pass ({@code true}) or
     * a full sweep ({@code false}).
     */
    Property<Boolean> getIncrementalPass()

    /**
     * Absolute paths of compiled class files affected by source changes
     * during an incremental build pass.
     */
    ListProperty<String> getAffectedClassFiles()

    /**
     * Relative paths of compiled class files that were removed since the
     * last build pass.
     */
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

    private final Logger logger = Logging.getLogger(LintNamedQueriesWorker)

    /**
     * Executes the named query lint analysis inside the isolated worker process.
     * <p>
     * Scans compiled class files using ASM to discover {@code @NamedQuery} and
     * {@code @NamedStoredProcedureQuery} annotations, then validates query syntax
     * via the SPI-loaded ANTLR4 parser and checks parameter naming conventions.
     * Supports both full sweep and incremental delta passes.
     * </p>
     */
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

        def log = logger

        // 1. ASM Bytecode Sweep Channel
        if (isIncremental) {
            log.lifecycle("⚡ Processing linter inputs incrementally. Auditing ${affectedFiles.size()} modified class boundaries...")
            affectedFiles.forEach { filePath ->
                File classFile = new File(filePath)
                if (classFile.exists()) {
                    Helper.sweepClassFile(classFile, discoveredQueries, discoveredProcedures, discoveredClasses)
                }
            }
            if (!removedFiles.isEmpty()) {
                log.lifecycle("🗑️ Detected ${removedFiles.size()} removed entities. Evicting structural caches.")
            }
        } else {
            log.lifecycle("⏳ Performing complete structural sweep linter pass over: ${classesDir.name}...")
            classesDir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class$/) { classFile ->
                Helper.sweepClassFile(classFile, discoveredQueries, discoveredProcedures, discoveredClasses)
            }
        }

        int totalObjects = discoveredQueries.size() + discoveredProcedures.size()

        if (totalObjects == 0 && isIncremental) {
            log.lifecycle("✅ No annotations alterations detected inside the modified entities. Build up-to-date.")
            return
        }

        if (totalObjects == 0) {
            log.lifecycle("✅ No named metadata configurations declared in this source set. Pass complete.")
            return
        }

        log.lifecycle("📊 Discovered ${totalObjects} static structural items to validate.")

        int errorCount = 0

        // 2. RE-ENGINEERED TRY-WITH-RESOURCES PARSING PIPELINE
        try {
            // Factory lookup maps via ServiceLoader
            def factory = NamedObjectParserFactory.newNamedObjectParserFactory(jpaVersion)

            // Open the closeable parser inside a safe container block
            factory.newNamedObjectParser(discoveredClasses).withCloseable { namedObjectParser ->
                log.lifecycle("✅ In-memory verification metamodel generated successfully.")

                // 3a. VALIDATE STANDARD QUERIES
                errorCount += Helper.validateDiscoveredQueries(namedObjectParser, discoveredQueries, ignoreWarnings)

                // 3b. VALIDATE STORED PROCEDURES
                errorCount += Helper.validateDiscoveredStoredProcedures(namedObjectParser, discoveredProcedures, ignoreWarnings)
            }
        } catch (Exception e) {
            // Eager validation failures triggered during buildSessionFactory() end up here
            log.error("❌ Critical: Metamodel initialization crash: " + e.getMessage())
            if (failOnError) {
                throw new GradleException("JPersist Linter failed on setup configuration.", e)
            }
            return
        }

        if (errorCount > 0) {
            log.error("⚠️ JPersist Static Linter found ${errorCount} structural metadata errors!")
            if (failOnError) {
                throw new GradleException("Build failed due to ${errorCount} invalid named metadata definitions.")
            }
        } else {
            log.lifecycle("✅ All processed static metadata structures validated successfully!")
        }
    }

    /**
     * ASM {@link ClassVisitor} that scans compiled class files for JPA annotation
     * metadata, collecting {@code @NamedQuery} and {@code @NamedStoredProcedureQuery}
     * definitions along with entity class names.
     */
    private static class ComprehensiveMetadataScanner extends ClassVisitor {

        /** Collected named query definitions keyed by query name. */
        private final Map<String, String> queries

        /** Collected stored procedure definitions keyed by procedure annotation name. */
        private final Map<String, Map<String, Set<String>>> procedures

        /** Collected fully qualified entity class names discovered during the scan. */
        private final List<String> classes

        /**
         * Creates a new scanner targeting ASM API version {@link Opcodes#ASM9}.
         *
         * @param queries    The mutable map to populate with discovered named queries.
         * @param procedures The mutable map to populate with discovered stored procedures.
         * @param classes    The mutable list to populate with discovered class names.
         */
        ComprehensiveMetadataScanner(Map<String, String> queries, Map<String, Map<String, Set<String>>> procedures, List<String> classes) {
            super(Opcodes.ASM9)
            this.queries = queries
            this.procedures = procedures
            this.classes = classes
        }

        /** {@inheritDoc} */
        @Override
        void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            if (name) {
                classes.add(name.replace('/', '.'))
            }
            super.visit(version, access, name, signature, superName, interfaces)
        }

        /** {@inheritDoc} */
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

    /**
     * ASM {@link AnnotationVisitor} that extracts the {@code name} and {@code query}
     * attributes from a single {@code @NamedQuery} annotation instance.
     */
    private static class NamedQueryDataExtractor extends AnnotationVisitor {

        /** The target map where the extracted query name and JPQL string are stored. */
        private final Map<String, String> registry

        /** The annotation's {@code name} attribute value. */
        private String name = ""

        /** The annotation's {@code query} attribute value (JPQL/HQL string). */
        private String query = ""

        /**
         * Creates a new extractor writing results into the given registry map.
         *
         * @param registry The mutable map to populate with the extracted query entry.
         */
        NamedQueryDataExtractor(Map<String, String> registry) { super(Opcodes.ASM9); this.registry = registry }

        /** {@inheritDoc} */
        @Override
        void visit(String name, Object value) {
            if (name == "name") this.name = value.toString()
            else if (name == "query") this.query = value.toString()
        }

        /** {@inheritDoc} */
        @Override
        void visitEnd() {
            if (name && query) registry.put(name, query)
        }
    }

    /**
     * ASM {@link AnnotationVisitor} that extracts the {@code name}, {@code procedureName},
     * and nested {@code parameters[].name} attributes from a single
     * {@code @NamedStoredProcedureQuery} annotation instance.
     */
    private static class StoredProcedureDataExtractor extends AnnotationVisitor {

        /** The target map where extracted procedure metadata is stored. */
        private final Map<String, Map<String, Set<String>>> registry

        /** The annotation's {@code name} attribute value. */
        private String name = ""

        /** The annotation's {@code procedureName} attribute value. */
        private String procedureName = ""

        /** Ordered set of parameter names extracted from the nested {@code parameters} array. */
        private Set<String> parameterNames = new TreeSet<>()

        /**
         * Creates a new extractor writing results into the given registry map.
         *
         * @param registry The mutable map to populate with the extracted procedure entry.
         */
        StoredProcedureDataExtractor(Map<String, Map<String, Set<String>>> registry) { super(Opcodes.ASM9); this.registry = registry }

        /** {@inheritDoc} */
        @Override
        void visit(String name, Object value) {
            if (name == "name") this.name = value.toString()
            else if (name == "procedureName") this.procedureName = value.toString()
        }

        /** {@inheritDoc} */
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

        /** {@inheritDoc} */
        @Override
        void visitEnd() {
            if (name && procedureName) {
                registry.put(name, [(procedureName): parameterNames])
            }
        }
    }

    /**
     * Static helper that encapsulates ASM bytecode scanning and SPI-based
     * query/procedure validation logic.
     * <p>
     * Isolating these methods in a plain static inner class prevents Groovy's
     * dynamic method dispatch from routing calls through Gradle's decorated
     * worker proxy.
     * </p>
     */
    private static class Helper {

        /**
         * Scans a single compiled class file using ASM, collecting named query
         * and stored procedure annotations plus the class name.
         *
         * @param classFile  The {@code .class} file to scan.
         * @param queries    The mutable map to populate with discovered named queries.
         * @param procedures The mutable map to populate with discovered stored procedures.
         * @param classes    The mutable list to populate with discovered class names.
         */
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

        /**
         * Validates all discovered {@code @NamedQuery} definitions against the
         * ANTLR4-based JPQL/HQL parser and checks parameter naming conventions.
         *
         * @param parser            The SPI-loaded named object parser instance.
         * @param discoveredQueries The map of query name to JPQL/HQL string.
         * @param ignoreWarnings    Whether to suppress style warning diagnostics.
         * @return The number of validation errors encountered.
         */
        private static int validateDiscoveredQueries(NamedObjectParser parser,
                                                     Map<String, String> discoveredQueries,
                                                     boolean ignoreWarnings) {
            int errorCount = 0;

            if (!discoveredQueries.isEmpty()) {
                println "⏳ Driving native ANTLR4 query semantic validation sweep..."
                discoveredQueries.each { name, queryString ->
                    try {
                        NamedObjectDescriptor descriptor = parser.parseQuery(name, queryString)
                        println "   ✅ Query '${name}' passed semantic compilation checks."

                        Set<String> namedParams = descriptor.namedParameters()

                        // Style Naming Governance Check
                        if (!ignoreWarnings && descriptor.namedParameters() != null) {
                            descriptor.namedParameters().each { String param ->
                                validateParameterStyle(name, param)
                            }
                        }
                    } catch (Exception e) {
                        errorCount++
                        Throwable cause = e.getCause() ?: e
                        System.err.println("   ❌ LINT ERROR in Query [${name}]: ${cause.getMessage() ?: cause.toString()}")
                    }
                }
            }

            return errorCount;
        }

        /**
         * Validates all discovered {@code @NamedStoredProcedureQuery} definitions
         * and checks parameter naming conventions against strict camelCase rules.
         *
         * @param parser               The SPI-loaded named object parser instance.
         * @param discoveredProcedures The map of procedure annotation name to procedure metadata.
         * @param ignoreWarnings       Whether to suppress style warning diagnostics.
         * @return The number of validation errors encountered.
         */
        private static int validateDiscoveredStoredProcedures(NamedObjectParser parser,
                                                              Map<String, Map<String, Set<String>>> discoveredProcedures,
                                                              boolean ignoreWarnings) {
            int errorCount = 0;

            if (!discoveredProcedures.isEmpty()) {
                println "⏳ Running parameters governance lint checks over named stored procedures..."
                discoveredProcedures.each { name, procedureMap ->
                    procedureMap.each { procedureName, asmCapturedParams ->
                        try {
                            // Delegate validation pass to the core engine
                            NamedObjectDescriptor descriptor = parser.parseStoredProcedure(name, procedureName, asmCapturedParams)
                            println "   ✅ Stored Procedure '${name}' [${procedureName}] metadata structural paths validated."

                            // Normalize nulls into clean empty sets to prevent NullPointerExceptions
                            Set<String> verifiedAsmParams = asmCapturedParams ?: ([] as Set<String>)

                            // Style Check Loop (Only runs if parameters are physically present)
                            if (!ignoreWarnings && !verifiedAsmParams.isEmpty()) {
                                verifiedAsmParams.each { String param ->
                                    validateParameterStyle(name, param)
                                }
                            }
                        } catch (Exception e) {
                            errorCount++
                            System.err.println("   ❌ LINT ERROR in Stored Procedure [${name}]: ${e.getMessage()}")
                        }
                    }
                }
            }

            return errorCount;
        }

    }

}
