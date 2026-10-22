package persist.jakarta.gradle.task

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.ChangeType
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import persist.jakarta.gradle.worker.LintNamedQueriesWorker
import javax.inject.Inject

/**
 * Executes a static, compile-time AST verification loop over all @NamedQuery definitions
 * discovered inside the compiled source set output directories.
 *
 * @since 1.6.0
 */
@DisableCachingByDefault(because = "Validates active source metadata state at execution time")
abstract class LintNamedQueries extends DefaultTask {

    /**
     * The JPA specification version used to determine the annotation namespace
     * ({@code jakarta.persistence} vs {@code javax.persistence}) during bytecode scanning.
     * <p>Defaults to {@code "3.0"}.</p>
     *
     * @return The lazy property tracking the JPA version string.
     */
    @Input
    abstract Property<String> getJpaVersion()

    /**
     * Whether discovering a query syntax error or metamodel mismatch should
     * immediately fail the Gradle build.
     * <p>Defaults to {@code true}. When set to {@code false}, errors are logged as warnings.</p>
     *
     * @return The lazy property tracking the build failure strategy on errors.
     */
    @Input
    abstract Property<Boolean> getFailOnError()

    /**
     * Whether sub-critical semantic validation warnings should be muted.
     * <p>Defaults to {@code false}.</p>
     *
     * @return The lazy property tracking the warning suppression strategy.
     */
    @Input
    abstract Property<Boolean> getIgnoreWarnings()

    /**
     * The input source directory holding the compiled Java class files to be scanned via ASM.
     * Annotated with @Incremental to instruct the Gradle engine to compute delta tracking states.
     */
    @Incremental
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileTree getCompiledClasses()

    /**
     * The complete runtime and compilation classpath containing the JPA provider binaries
     * (Hibernate/EclipseLink) used to drive the internal semantic parsing engine.
     */
    @CompileClasspath
    abstract ConfigurableFileCollection getClasspath()

    /**
     * The generated JSON compilation report containing metrics regarding processed and skipped files.
     * Acts as the mandatory @OutputFile checkpoint to satisfy Gradle's execution graph rules.
     */
    @OutputFile
    abstract RegularFileProperty getLintReportFile()

    private final WorkerExecutor workerExecutor

    @Inject
    LintNamedQueries(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void lint(InputChanges inputChanges) {
        File classesFolder = compiledClasses.getDir()
        Set<File> runtimeClasspathFiles = classpath.getFiles()

        boolean failOnValidationError = failOnError.get()
        boolean skipWarnings = ignoreWarnings.get()
        String version = jpaVersion.get()

        // 1. Differentiate between a full compilation pass and a localized delta pass
        List<String> affectedClassFiles = []
        List<String> removedClassFiles = []
        boolean isIncrementalPass = inputChanges.incremental

        int totalProcessed = 0
        int totalSkipped = 0

        // Count total matching .class files inside the tree context
        int totalTargetFiles = compiledClasses.asFileTree.matching { include '**/*.class' }.files.size()

        if (isIncrementalPass) {
            // Isolate strictly the added, modified, or removed .class bytecodes
            inputChanges.getFileChanges(compiledClasses).each { change ->
                if (change.file.name.endsWith('.class')) {
                    String relativePath = change.normalizedPath
                    if (change.changeType == ChangeType.REMOVED) {
                        removedClassFiles.add(relativePath)
                    } else {
                        affectedClassFiles.add(change.file.absolutePath)
                    }
                }
            }
            // In an incremental pass, processed files match the size of the delta
            totalProcessed = affectedClassFiles.size()
            totalSkipped = Math.max(0, totalTargetFiles - totalProcessed)
        } else {
            // In a non-incremental full pass, every class file inside the directory is processed
            totalProcessed = totalTargetFiles
            totalSkipped = 0
        }

        // 2. Submit execution payload across the ClassLoader barrier to the isolated worker container
        workerExecutor.classLoaderIsolation { workerSpec ->
            workerSpec.classpath.from(runtimeClasspathFiles)
            workerSpec.classpath.from(classesFolder)
        }.submit(LintNamedQueriesWorker) { params ->
            params.compiledClassesDir.set(classesFolder)
            params.failOnError.set(failOnValidationError)
            params.ignoreWarnings.set(skipWarnings)
            params.jpaVersion.set(version)

            // Inject incremental delta tracing lists into parameters
            params.incrementalPass.set(isIncrementalPass)
            params.affectedClassFiles.set(affectedClassFiles)
            params.removedClassFiles.set(removedClassFiles)
        }

        // 3. Generate and save a structured JSON execution metrics report on the file system
        Map reportMetadata = [
            taskName          : name,
            timestamp         : System.currentTimeMillis(),
            incremental       : isIncrementalPass,
            totalFilesDetected: totalTargetFiles,
            processedFiles    : totalProcessed,
            skippedFiles      : totalSkipped,
            removedFiles      : removedClassFiles.size()
        ]

        File reportFile = lintReportFile.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(reportMetadata))
    }

}
