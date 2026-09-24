package persist.hibernate.gradle.task

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.hibernate.bytecode.enhance.spi.Enhancer
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext
import org.hibernate.bytecode.enhance.spi.UnloadedClass
import org.hibernate.bytecode.enhance.spi.UnloadedField

import static org.hibernate.bytecode.internal.BytecodeProviderInitiator.buildDefaultBytecodeProvider

/**
 * A custom Gradle task that programmatically performs compile-time bytecode enhancement on Hibernate entities.
 * <p>
 * This task parses a directory of raw compiled classes and runs Hibernate's internal {@link Enhancer} engine.
 * It rewrites entity class definitions to support optimizations such as lazy initialization at the field level,
 * inline change tracking (dirty checking), and automatic bidirectional association management. By writing
 * enhanced results out to an isolated target directory, the task preserves full compatibility with
 * Gradle's Configuration Cache and incremental build system.
 * </p>
 *
 * @since 1.0.0
 */
abstract class HibernateEnhancementTask extends DefaultTask {

    /**
     * The full compilation dependencies classpath necessary for Hibernate to inspect, resolve,
     * and validate complex relationships, types, or embedded enums (e.g., {@code Person$Gender})
     * during the bytecode analysis phase.
     *
     * @return The file collection managing the project's compilation dependencies.
     */
    @CompileClasspath
    abstract ConfigurableFileCollection getCompileClasspath()

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
     * The main execution block for the task.
     * <p>
     * Constructs a specialized, isolated {@link URLClassLoader} linking the raw class sources and the
     * complete compile classpath. It establishes a custom {@link DefaultEnhancementContext} mapping your
     * configuration flags, invokes the multi-pass Hibernate toolbelt (performing both type discovery
     * and byte transformation), and populates the separate target directory with copy/rewrite results.
     * </p>
     */
    @TaskAction
    void enhance() {
        def src = sourceClassesDir.get().asFile
        def dst = targetClassesDir.get().asFile

        if (src.exists() && src.list()?.length > 0) {
            if (!dst.exists()) {
                dst.mkdirs()
            }

            URL[] urls = [src.toURI().toURL()] as URL[]
            compileClasspath.files.forEach { file ->
                urls += file.toURI().toURL()
            }

            def classLoader = new URLClassLoader(urls, Enhancer.class.classLoader)

            def enhancementContext = new DefaultEnhancementContext() {
                @Override
                ClassLoader getLoadingClassLoader() {
                    return classLoader
                }

                @Override
                boolean doBiDirectionalAssociationManagement(UnloadedField field) {
                    return associationManagementEnabled.get()
                }

                @Override
                boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
                    return dirtyTrackingEnabled.get()
                }

                @Override
                boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
                    return lazyInitializationEnabled.get()
                }

                @Override
                boolean isLazyLoadable(UnloadedField field) {
                    return lazyInitializationEnabled.get()
                }

                @Override
                boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
                    return extendedEnhancementEnabled.get()
                }
            }

            Enhancer enhancer = buildDefaultBytecodeProvider().getEnhancer(enhancementContext)

            src.eachFileRecurse { file ->
                if (file.isFile() && file.name.endsWith('.class')) {
                    def relativePath = src.toPath().relativize(file.toPath()).toString()
                    def className = relativePath.replace(File.separator, '.')
                    def outputFile = dst.toPath().resolve(relativePath).toFile()
                    outputFile.parentFile.mkdirs()

                    def originalBytes = file.bytes
                    enhancer.discoverTypes(className, originalBytes)
                    def enhancedBytes = enhancer.enhance(className, originalBytes)

                    if (enhancedBytes != null) {
                        outputFile.bytes = enhancedBytes
                        logger.lifecycle("Successfully enhanced class : ${className}")
                    } else {
                        outputFile.bytes = originalBytes
                        logger.lifecycle("Skipping class : ${className}")
                    }
                }
            }
        }
    }

}
