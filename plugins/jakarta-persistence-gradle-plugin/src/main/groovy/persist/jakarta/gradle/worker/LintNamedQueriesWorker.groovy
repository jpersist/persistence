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
import persist.lint.query.spi.QueryParser
import persist.lint.query.spi.QueryParserFactory

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
            println "✅ No compiled classes found. Skipping query linter pass."
            return
        }

        // Registry map to collect extracted queries: Key = Query Name, Value = JPQL/HQL String
        Map<String, String> discoveredQueries = [:]
        List<String> discoveredClasses = [] // Tracks structural entity class types

        // 1. HIGH-SPEED ASM BYTECODE SWEEP
        classesDir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class$/) { classFile ->
            classFile.withInputStream { is ->
                try {
                    ClassReader reader = new ClassReader(is)
                    reader.accept(new NamedQueryAnnotationScanner(discoveredQueries, discoveredClasses),
                        ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES)
                } catch (Exception e) {
                    System.err.println("⚠️ Warning: Failed to sweep class file metrics for ${classFile.name}: ${e.message}")
                }
            }
        }

        println "📊 Discovered ${discoveredQueries.size()} static @NamedQuery entries to validate."

        if (discoveredQueries.isEmpty()) {
            println "✅ No Named Queries declared in this source set. Pass complete."
            return
        }

        // Trace print discovered entries cleanly for validation tracing
        discoveredQueries.each { qName, qString ->
            println "   🔍 Found Query: '${qName}' -> [${qString.trim().replaceAll("\\s+", " ")}]"
        }

        // 2. Factory Builder Design Pattern: Resolve strategy and build Metamodel
        QueryParser queryParser = null
        try {
            def queryParserFactory = QueryParserFactory.newQueryParserFactory(jpaVersion)
            queryParser = queryParserFactory.newQueryParser(discoveredClasses)
            println "✅ In-memory verification metamodel generated successfully."
        } catch (Exception e) {
            System.err.println("❌ Critical: Metamodel initialization crash: " + e.message)
            if (failOnError) throw new GradleException("JPersist Linter failed on setup configuration.", e)
            return
        }

        // 3. COMPLETE STRONGLY-TYPED SEMANTIC VALIDATION SWEEP
        println "⏳ Driving native ANTLR4 query semantic validation sweep..."
        int errorCount = 0

        discoveredQueries.each { queryName, queryString ->
            try {
                // Execute version-isolated typed verification routine safely
                queryParser.parseQuery(queryName, queryString)
                println "   ✅ Query '${queryName}' passed semantic compilation checks."
            } catch (Exception e) {
                errorCount++
                Throwable cause = e.getCause() ?: e
                System.err.println("   ❌ LINT ERROR in Query [${queryName}]:")
                System.err.println("      Expression: ${queryString.trim()}")
                System.err.println("      Reason:     ${cause.getMessage() ?: cause.toString()}")
            }
        }

        if (errorCount > 0) {
            System.err.println("⚠️ JPersist Static Linter found ${errorCount} query compilation errors!")
            if (failOnError) {
                throw new GradleException("Build failed due to ${errorCount} invalid @NamedQuery syntax definitions.")
            }
        } else {
            println "✅ All discovered static named queries validated successfully!"
        }
    }

    /**
     * Specialized ASM ClassVisitor targeting JPA NamedQuery annotations at the class metadata layer.
     */
    private static class NamedQueryAnnotationScanner extends ClassVisitor {
        private final Map<String, String> queriesRegistry
        private final List<String> classesRegistry

        NamedQueryAnnotationScanner(Map<String, String> queriesRegistry, List<String> classesRegistry) {
            super(Opcodes.ASM9)
            this.queriesRegistry = queriesRegistry
            this.classesRegistry = classesRegistry
        }

        @Override
        void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            // Convert internal ASM slash notation to a standard fully qualified binary dot path string
            // e.g., "com/example/model/InventoryItem" -> "com.example.model.InventoryItem"
            if (name) {
                classesRegistry.add(name.replace('/', '.'))
            }
            super.visit(version, access, name, signature, superName, interfaces)
        }

        @Override
        AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            // Case A: Single @NamedQuery declaration block found
            if (descriptor == 'Ljakarta/persistence/NamedQuery;' || descriptor == 'Ljavax/persistence/NamedQuery;') {
                return new NamedQueryDataExtractor(queriesRegistry)
            }
            // Case B: Multi-query wrapper @NamedQueries container block found
            if (descriptor == 'Ljakarta/persistence/NamedQueries;' || descriptor == 'Ljavax/persistence/NamedQueries;') {
                return new NamedQueriesContainerExtractor(queriesRegistry)
            }
            return null
        }
    }

    /**
     * Extracts primitive string value pairs ('name' and 'query') out of a @NamedQuery block.
     */
    private static class NamedQueryDataExtractor extends AnnotationVisitor {
        private final Map<String, String> queriesRegistry
        private String queryName = ""
        private String queryString = ""

        NamedQueryDataExtractor(Map<String, String> queriesRegistry) {
            super(Opcodes.ASM9)
            this.queriesRegistry = queriesRegistry
        }

        @Override
        void visit(String name, Object value) {
            if (name == "name") {
                this.queryName = value.toString()
            } else if (name == "query") {
                this.queryString = value.toString()
            }
        }

        @Override
        void visitEnd() {
            if (queryName && queryString) {
                queriesRegistry.put(queryName, queryString)
            }
        }
    }

    /**
     * Iterates over the arrays matrix elements inside a @NamedQueries multi-container annotation wrapper.
     */
    private static class NamedQueriesContainerExtractor extends AnnotationVisitor {
        private final Map<String, String> queriesRegistry

        NamedQueriesContainerExtractor(Map<String, String> queriesRegistry) {
            super(Opcodes.ASM9)
            this.queriesRegistry = queriesRegistry
        }

        @Override
        AnnotationVisitor visitArray(String name) {
            if (name == "value") {
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    AnnotationVisitor visitAnnotation(String unnamed, String descriptor) {
                        if (descriptor == 'Ljakarta/persistence/NamedQuery;' || descriptor == 'Ljavax/persistence/NamedQuery;') {
                            return new NamedQueryDataExtractor(queriesRegistry)
                        }
                        return null
                    }
                }
            }
            return null
        }
    }

}
