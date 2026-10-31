package persist.hibernate.gradle.task

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.DefaultTask
import org.gradle.work.DisableCachingByDefault
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import persist.hibernate.gradle.worker.EnhancementWorker

import javax.inject.Inject

/**
 * A custom Gradle task that performs compile-time bytecode enhancement on Hibernate entities
 * using a classloader-isolated worker process.
 * <p>
 * This task delegates the actual enhancement work to an {@link EnhancementWorker}, which
 * loads the Persistence Tools SPI factories and the Hibernate bytecode engine inside a
 * separate classloader managed by Gradle's {@link WorkerExecutor}. The worker rewrites
 * entity class definitions to support optimizations such as lazy initialization at the
 * field level, inline change tracking (dirty checking), and automatic bidirectional
 * association management. By writing enhanced results out to an isolated target directory,
 * the task preserves full compatibility with Gradle's Configuration Cache and incremental
 * build system.
 * </p>
 * <p>
 * The {@link HibernateEnhancementTask#getEnhanceClasspath() enhanceClasspath} carries the JPersist
 * {@code persistence-tools-api}, the namespace-specific provider
 * ({@code persistence-tools-jakarta} or {@code persistence-tools-legacy}), and the
 * Hibernate core binaries. The {@link HibernateEnhancementTask#getCompileClasspath() compileClasspath}
 * carries the project's own compilation dependencies so the worker can resolve entity
 * relationships, embedded types, and enums.
 * </p>
 *
 * @since 1.0.0
 * @see EnhancementWorker
 */
@DisableCachingByDefault(because = "Enhancement modifies classes in place")
abstract class HibernateEnhancementTask extends DefaultTask {

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
     * Toggles whether to enhance entities to support field-level lazy initialization.
     * Evaluated lazily via a reactive property state to prevent premature lifecycle reads.
     *
     * @return The property wrapping the lazy initialization toggle.
     */
    @Input
    abstract Property<Boolean> getLazyInitializationEnabled()

    /**
     * Toggles whether to inject inline dirty tracking code directly into entity fields,
     * allowing Hibernate to manage changes without relying on heavy reflections during transaction flush phases.
     *
     * @return The property wrapping the dirty tracking toggle.
     */
    @Input
    abstract Property<Boolean> getDirtyTrackingEnabled()

    /**
     * Toggles whether to automatically manage bidirectional associations across mapped relationship bounds.
     *
     * @return The property wrapping the association management toggle.
     */
    @Input
    abstract Property<Boolean> getAssociationManagementEnabled()

    /**
     * Toggles whether to enable advanced extended bytecode enhancement strategies.
     *
     * @return The property wrapping the extended enhancement toggle.
     */
    @Input
    abstract Property<Boolean> getExtendedEnhancementEnabled()

    /**
     * The read-only input source directory containing the clean, raw compiled Java class files before enhancement.
     * <p>
     * Employs {@link PathSensitivity#RELATIVE} to ensure path-agnostic remote build caching capabilities.
     * The {@link SkipWhenEmpty} annotation ensures that if no classes are present, the execution engine skips
     * this task automatically with a true {@code NO-SOURCE} outcome status.
     * </p>
     *
     * @return The directory property containing un-enhanced bytecode source elements.
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    abstract DirectoryProperty getSourceClassesDir()

    /**
     * The target output directory where the modified and enhanced class files will be generated.
     * <p>
     * Isolating this folder guarantees strict incremental snapshot compliance (subsequent build executions
     * evaluate successfully as {@code UP-TO-DATE} if no changes occur). Downstream archive operations
     * like the standard {@code Jar} task will consume from this location.
     * </p>
     *
     * @return The destination directory property for enhanced output bytecode.
     */
    @OutputDirectory
    abstract DirectoryProperty getTargetClassesDir()

    /**
     * The isolated toolset dependencies classpath carrying the JPersist persistence-tools binaries
     * alongside Hibernate core binaries. Used to drive the worker classloader.
     *
     * @return The file collection managing the enhancement toolset dependencies.
     */
    @CompileClasspath
    abstract ConfigurableFileCollection getEnhanceClasspath()

    /**
     * The full compilation dependencies classpath necessary for Hibernate to inspect, resolve,
     * and validate complex relationships or fields during the bytecode analysis phase.
     *
     * @return The file collection managing the project's compilation dependencies.
     */
    @CompileClasspath
    abstract ConfigurableFileCollection getCompileClasspath()

    private final WorkerExecutor workerExecutor

    /**
     * Creates a new task instance.
     *
     * @param workerExecutor The Gradle {@link WorkerExecutor} used to submit
     *                       the isolated enhancement work action.
     */
    @Inject
    HibernateEnhancementTask(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    /**
     * Executes bytecode enhancement by submitting an {@link EnhancementWorker} to a
     * classloader-isolated worker process.
     * <p>
     * The worker receives the JPA version string, the source and target class directories,
     * the compilation classpath, and the four enhancement toggle flags. The
     * {@link #getEnhanceClasspath() enhanceClasspath} is loaded into the worker's parent
     * classloader so that the Persistence Tools SPI and the Hibernate bytecode provider
     * are available without polluting the Gradle daemon's own classpath.
     * </p>
     */
    @TaskAction
    void enhance() {
        def version = jpaVersion.get()

        def src = sourceClassesDir.get().asFile
        def dst = targetClassesDir.get().asFile

        def enableLazyInitialization = lazyInitializationEnabled.getOrElse(false)
        def enableDirtyTracking = dirtyTrackingEnabled.getOrElse(false)
        def enableAssociationManagement = associationManagementEnabled.getOrElse(false)
        def enableExtendedEnhancement = extendedEnhancementEnabled.getOrElse(false)

        def toolsetFiles = enhanceClasspath.getFiles()
        def externalDependencies = compileClasspath.getFiles()

        // Configure classloader isolation for the child worker container.
        // We load the structural tools and SPI factories into the worker's parent context.
        workerExecutor.classLoaderIsolation { spec ->
            spec.classpath.from(toolsetFiles)
        }.submit(EnhancementWorker) { params ->
            params.jpaVersion.set(version)
            params.compiledClassesDir.set(src)
            params.enhancedClassesDir.set(dst)

            // Pass the complete compilation dependencies classpath so the worker can
            // merge it dynamically with the execution context URLClassLoader array
            params.compileClasspath.from(externalDependencies)

            params.lazyInitializationEnabled.set(enableLazyInitialization)
            params.dirtyTrackingEnabled.set(enableDirtyTracking)
            params.associationManagementEnabled.set(enableAssociationManagement)
            params.extendedEnhancementEnabled.set(enableExtendedEnhancement)
        }
    }

}
