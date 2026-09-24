package persist.eclipse.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations

import javax.inject.Inject

/**
 * A custom Gradle task that handles compile-time static bytecode weaving for EclipseLink entities.
 * <p>
 * This task intercepts standard compilation outputs and executes the official EclipseLink
 * {@code StaticWeave} command-line processor in an isolated JVM process using {@link ExecOperations}.
 * In-place bytecode manipulation updates entities to natively handle lazy loading hooks, fetch graph
 * optimizations, and active dirty tracking, entirely removing the need for a dynamic runtime javaagent.
 * </p>
 * <p>
 * Designed to satisfy strict Gradle Configuration Cache rules and incremental build validations,
 * this task isolates its input/output directory tracking to avoid cache invalidation loops.
 * </p>
 *
 * @since 1.0.0
 */
abstract class EclipseWeaveTask extends DefaultTask {

    /**
     * The input directory housing configuration infrastructure metadata files, such as your
     * target {@code persistence.xml} or ORM mapping descriptors.
     * <p>
     * Utilizes {@link PathSensitivity#RELATIVE} to guarantee that absolute machine paths do not
     * compromise the shareable integrity of the Gradle remote build cache.
     * </p>
     *
     * @return The directory property containing persistence configurations.
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getResourcesDir()

    /**
     * The immutable input source directory holding clean, raw compiled Java class files before weaving.
     * <p>
     * The {@link SkipWhenEmpty} annotation indicates to the Gradle build lifecycle engine that if this
     * directory is empty or missing (e.g., if there are no files to process), this transformation
     * task must automatically bypass execution and mark its operational state outcome as {@code NO-SOURCE}.
     * </p>
     *
     * @return The read-only directory tracking the raw compiled classes.
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    abstract DirectoryProperty getSourceClassesDir()

    /**
     * The target output directory where the newly generated, woven bytecode outputs will be deposited.
     * <p>
     * Separating this path from the source input directory enables full incremental build correctness,
     * allowing subsequent task iterations to return a clean {@code UP-TO-DATE} status if no changes are made.
     * </p>
     *
     * @return The output directory tracking the modified classes.
     */
    @OutputDirectory
    abstract DirectoryProperty getTargetClassesDir()

    /**
     * The standalone classpath collection carrying the EclipseLink core libraries and tool binaries
     * required to execute the {@code StaticWeave} main class execution process.
     *
     * @return The file collection managing the weaver runtime binaries.
     */
    @CompileClasspath
    abstract ConfigurableFileCollection getWeaveClasspath()

    /**
     * The full compilation dependencies classpath necessary for EclipseLink to inspect, map,
     * and validate structural relationship models while modifying entity classes.
     *
     * @return The file collection managing the project's compilation dependencies.
     */
    @CompileClasspath
    abstract ConfigurableFileCollection getCompileClasspath()

    /**
     * An internal worker reference providing isolated process execution operations.
     */
    private ExecOperations execOperations

    /**
     * Constructs a new {@code EclipseWeaveTask} instance.
     * <p>
     * Uses Gradle dependency injection to pass an active {@link ExecOperations} instance safely,
     * keeping the task configuration cache compliant without leaking live project handles.
     * </p>
     *
     * @param execOperations The injected execution manager utility.
     */
    @Inject
    EclipseWeaveTask(ExecOperations execOperations) {
        this.execOperations = execOperations
    }

    /**
     * The core action block for the task execution.
     * <p>
     * Spawns an isolated Java process running the {@code org.eclipse.persistence.tools.weaving.jpa.StaticWeave}
     * main class, configuring arguments to map the byte structures cleanly from the raw source
     * to the enhanced output target.
     * </p>
     */
    @TaskAction
    void weave() {
        def resourcesPath = getResourcesDir().get().asFile.absolutePath
        def sourcePath = getSourceClassesDir().get().asFile.absolutePath
        def targetPath = getTargetClassesDir().get().asFile.absolutePath

        execOperations.javaexec { spec ->
            spec.mainClass.set('org.eclipse.persistence.tools.weaving.jpa.StaticWeave')
            spec.classpath = getWeaveClasspath()
            spec.args(
                '-persistenceinfo', resourcesPath,
                '-classpath', getCompileClasspath().asPath,
                '-loglevel', 'FINE',
                sourcePath,
                targetPath
            )
        }
    }

}
