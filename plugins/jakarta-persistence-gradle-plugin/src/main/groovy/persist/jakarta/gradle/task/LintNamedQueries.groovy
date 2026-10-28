package persist.jakarta.gradle.task

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
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
     * The input source directories tracking active Java source code files (.java).
     * Sourced from the active compilation source sets.
     *
     * @return The file collection managing the incremental source file inputs.
     */
    @Incremental
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getSourceFiles()

    /**
     * The input source directory holding the compiled Java class files to be scanned via ASM.
     *
     * @return The lazy directory property tracking the compiled classes directory.
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getCompiledClassesDir()

    /**
     * The complete runtime and compilation classpath containing the JPA provider binaries
     * (Hibernate/EclipseLink) used to drive the internal semantic parsing engine.
     *
     * @return The file collection managing the linter's runtime classpath.
     */
    @CompileClasspath
    abstract ConfigurableFileCollection getClasspath()

    /**
     * The generated JSON compilation report containing metrics regarding processed and skipped files.
     * Acts as the mandatory @OutputFile checkpoint to satisfy Gradle's execution graph rules.
     *
     * @return The lazy file property tracking the JSON report output location.
     */
    @OutputFile
    abstract RegularFileProperty getLintReportFile()

    /** The Gradle worker executor used to submit isolated linter work actions. */
    private final WorkerExecutor workerExecutor

    /**
     * Creates a new task instance.
     *
     * @param workerExecutor The Gradle {@link WorkerExecutor} used to submit
     *                       the isolated linter work action.
     */
    @Inject
    LintNamedQueries(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    /**
     * Executes the named query lint analysis.
     * <p>
     * Differentiates between a full sweep and an incremental delta pass based
     * on the provided {@link InputChanges}. Modified source files are mapped
     * to their compiled class counterparts and submitted to an isolated
     * {@link LintNamedQueriesWorker} for AST validation. A JSON execution
     * metrics report is written to {@link #getLintReportFile()}.
     * </p>
     *
     * @param inputChanges The Gradle incremental input changes descriptor.
     */
    @TaskAction
    void lint(InputChanges inputChanges) {
        File classesFolder = compiledClassesDir.get().asFile
        Set<File> runtimeClasspathFiles = classpath.getFiles()

        boolean failOnValidationError = failOnError.get()
        boolean skipWarnings = ignoreWarnings.get()
        String version = jpaVersion.get()

        // 1. Differentiate between a full compilation pass and a localized delta pass
        List<String> affectedClassFiles = []
        List<String> removedClassFiles = []
        boolean isIncrementalPass = inputChanges.incremental

        // Track files matching our target bytecode criteria
        int totalTargetFiles = 0
        int totalProcessed = 0
        int totalSkipped = 0

        // Resolve total classes count directly from the file system directory pass
        classesFolder.eachFileRecurse { file ->
            if (file.name.endsWith('.class')) {
                totalTargetFiles++
            }
        }

        if (isIncrementalPass) {
            // Stream altered file collections from the immutable source repository graph
            inputChanges.getFileChanges(sourceFiles).each { change ->
                if (change.file.name.endsWith('.java')) {
                    String relativePath = change.normalizedPath

                    // Translate the source java file structure path to its expected bytecode counterpart
                    String classRelativePath = relativePath.substring(0, relativePath.lastIndexOf('.java')) + '.class'
                    File correspondingClassFile = new File(classesFolder, classRelativePath)

                    if (change.changeType == ChangeType.REMOVED) {
                        removedClassFiles.add(classRelativePath)
                    } else if (correspondingClassFile.exists()) {
                        affectedClassFiles.add(correspondingClassFile.absolutePath)
                    }
                }
            }
            totalProcessed = affectedClassFiles.size()
            totalSkipped = Math.max(0, totalTargetFiles - totalProcessed)
        } else {
            // Full pass execution fallback: sweep all files present inside the compilation folder
            classesFolder.eachFileRecurse { file ->
                if (file.name.endsWith('.class')) {
                    affectedClassFiles.add(file.absolutePath)
                }
            }
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
