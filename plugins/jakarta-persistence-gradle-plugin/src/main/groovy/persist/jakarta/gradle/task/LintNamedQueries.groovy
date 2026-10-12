package persist.jakarta.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
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
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    abstract DirectoryProperty getCompiledClassesDir()

    /**
     * The complete runtime and compilation classpath containing the JPA provider binaries
     * (Hibernate/EclipseLink) used to drive the internal semantic parsing engine.
     */
    @CompileClasspath
    abstract ConfigurableFileCollection getClasspath()

    private final WorkerExecutor workerExecutor

    @Inject
    LintNamedQueries(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void lint() {
        File classesFolder = compiledClassesDir.get().asFile
        Set<File> runtimeClasspathFiles = classpath.getFiles()

        boolean failOnValidationError = failOnError.getOrElse(true)
        boolean skipWarnings = ignoreWarnings.getOrElse(false)

        String version = jpaVersion.getOrElse('3.0')

        // Leverage Worker API classloader isolation to prevent classpath pollution
        workerExecutor.classLoaderIsolation { workerSpec ->
            workerSpec.classpath.from(runtimeClasspathFiles)
            workerSpec.classpath.from(classesFolder)
        }.submit(LintNamedQueriesWorker) { params ->
            params.getCompiledClassesDir().set(classesFolder)
            params.getFailOnError().set(failOnValidationError)
            params.getIgnoreWarnings().set(skipWarnings)
            params.getJpaVersion().set(version)
        }
    }

}
