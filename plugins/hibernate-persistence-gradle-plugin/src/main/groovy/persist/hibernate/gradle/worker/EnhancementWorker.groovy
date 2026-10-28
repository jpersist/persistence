package persist.hibernate.gradle.worker

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import persist.hibernate.enhance.spi.HibernateEnhancerProvider

import static persist.hibernate.enhance.spi.HibernateEnhancer.Option.*

/**
 * Gradle {@link WorkAction} that performs Hibernate compile-time bytecode enhancement
 * inside a classloader-isolated worker process.
 * <p>
 * The worker resolves the appropriate Hibernate bytecode engine via the decoupled
 * {@link HibernateEnhancerProvider} SPI, which is loaded from the
 * {@code persistence-tools-jakarta} or {@code persistence-tools-legacy} provider
 * library depending on the active JPA specification version. It then iterates over
 * every {@code .class} file in the compiled source directory, applies the two-pass
 * enhancement cycle (type discovery followed by byte transformation), and writes
 * the results to an isolated target directory.
 * </p>
 * <p>
 * This worker is submitted by {@link persist.hibernate.gradle.task.HibernateEnhancementTask}
 * using {@link org.gradle.workers.WorkerExecutor#classLoaderIsolation classLoaderIsolation},
 * ensuring that the Persistence Tools SPI factories and Hibernate core binaries are loaded
 * in a separate classloader that does not pollute the Gradle daemon's own classpath.
 * </p>
 *
 * @since 1.8.0
 * @see persist.hibernate.gradle.task.HibernateEnhancementTask
 * @see HibernateEnhancerProvider
 */
abstract class EnhancementWorker implements WorkAction<Parameters> {

    /** Logger instance used to report enhancement progress and skipped classes. */
    private final Logger logger = Logging.getLogger(EnhancementWorker)

    /**
     * Executes the two-pass bytecode enhancement cycle.
     * <p>
     * Resolves the {@link HibernateEnhancerProvider} SPI for the configured JPA version,
     * iterates over every {@code .class} file in the compiled source directory, applies
     * the enhancement transformation, and writes the results (enhanced or original bytes)
     * to the isolated target directory.
     * </p>
     */
    @Override
    void execute() {
        def version = parameters.jpaVersion.get()
        def src = parameters.compiledClassesDir.get().asFile
        def dst = parameters.enhancedClassesDir.get().asFile

        if (src.exists() && src.list()?.length > 0) {
            if (!dst.exists()) {
                dst.mkdirs()
            }

            def log = logger

            // Resolve the specific lifecycle engine provider via decoupled SPI ServiceLoader maps
            def enhancerProvider = HibernateEnhancerProvider.getHibernateEnhancerProvider(version)
            def enhancer = enhancerProvider.getEnhancer(src, parameters.compileClasspath.getFiles(), [
                (LAZY_INITIALIZATION)    : parameters.lazyInitializationEnabled.getOrElse(true),
                (DIRTY_TRACKING)         : parameters.dirtyTrackingEnabled.getOrElse(true),
                (ASSOCIATION_MANAGEMENT) : parameters.associationManagementEnabled.getOrElse(true),
                (EXTENDED_ENHANCEMENT)   : parameters.extendedEnhancementEnabled.getOrElse(false)
            ])

            src.eachFileRecurse { file ->
                if (file.isFile() && file.name.endsWith('.class')) {
                    def relativePath = src.toPath().relativize(file.toPath()).toString()
                    def className = relativePath.substring(0, relativePath.length() - 6)
                            .replace('\\', '.')
                            .replace('/', '.')

                    def outputFile = dst.toPath().resolve(relativePath).toFile()

                    def originalBytes = file.bytes
                    def enhancedBytes = enhancer.enhance(className, originalBytes)

                    outputFile.parentFile.mkdirs()

                    if (enhancedBytes != null) {
                        outputFile.bytes = enhancedBytes
                        log.lifecycle("Successfully enhanced class : ${className}")
                    } else {
                        outputFile.bytes = originalBytes
                        log.lifecycle("Skipping class (unmodified) : ${className}")
                    }
                }
            }
        }
    }

    /**
     * Parameter interface for the {@link EnhancementWorker} work action.
     * <p>
     * Carries the JPA version, input/output directories, the project's compilation
     * classpath, and the four bytecode enhancement toggle flags from the
     * {@link persist.hibernate.gradle.task.HibernateEnhancementTask} into the
     * isolated worker process.
     * </p>
     *
     * @since 1.8.0
     */
    static interface Parameters extends WorkParameters {

        /** The JPA specification version for selecting the correct SPI provider. */
        Property<String> getJpaVersion()

        /** The input directory containing the raw, un-enhanced compiled class files. */
        DirectoryProperty getCompiledClassesDir()

        /** The output directory where enhanced class files are written. */
        DirectoryProperty getEnhancedClassesDir()

        /**
         * The project's compilation classpath, merged into the worker's
         * {@link URLClassLoader} so Hibernate can resolve entity relationships,
         * embedded types, and enums.
         */
        ConfigurableFileCollection getCompileClasspath()

        /** Whether field-level lazy initialization enhancement is enabled. */
        Property<Boolean> getLazyInitializationEnabled()

        /** Whether inline dirty tracking enhancement is enabled. */
        Property<Boolean> getDirtyTrackingEnabled()

        /** Whether bidirectional association management enhancement is enabled. */
        Property<Boolean> getAssociationManagementEnabled()

        /** Whether extended bytecode enhancement strategies are enabled. */
        Property<Boolean> getExtendedEnhancementEnabled()

    }

}
