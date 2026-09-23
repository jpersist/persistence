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

abstract class HibernateEnhancementTask extends DefaultTask {

    @CompileClasspath
    abstract ConfigurableFileCollection getCompileClasspath()

    @Input
    abstract Property<Boolean> getLazyInitializationEnabled()

    @Input
    abstract Property<Boolean> getDirtyTrackingEnabled()

    @Input
    abstract Property<Boolean> getAssociationManagementEnabled()

    @Input
    abstract Property<Boolean> getExtendedEnhancementEnabled()

    // Source directory holds clean compiled files (Read-Only Input)
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    abstract DirectoryProperty getSourceClassesDir()

    // Target directory holds newly generated woven outputs
    @OutputDirectory
    abstract DirectoryProperty getTargetClassesDir()

    @TaskAction
    void enhance() {
        def src = sourceClassesDir.get().asFile
        def dst = targetClassesDir.get().asFile

        if (src.exists() && src.list()?.length > 0) {
            if (!dst.exists()) {
                dst.mkdirs()
            }

            // Build a classloader pointing to our output classes folder.
            // This allows ByteBuddy to load inner classes/enums like Person$Gender.class cleanly.
            URL[] urls = [src.toURI().toURL()] as URL[]
            compileClasspath.files.forEach { file ->
                urls += file.toURI().toURL()
            }

            def classLoader = new URLClassLoader(urls, Enhancer.class.classLoader)

            // 1. Construct a clean Hibernate enhancement context
            def enhancementContext = new DefaultEnhancementContext() {
                // Explicitly bind the classloader context to this worker thread runtime execution
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

            // 2. Instantiate the programmatic Hibernate Enhancer tool
            Enhancer enhancer = buildDefaultBytecodeProvider().getEnhancer(enhancementContext)

            // 3. Process files iteratively (In-place enhancement safe for Gradle's tracking engines)
            src.eachFileRecurse { file ->
                if (file.isFile() && file.name.endsWith('.class')) {
                    def relativePath = src.toPath().relativize(file.toPath()).toString()
                    def className = relativePath.replace(File.separator, '.')
                    def outputFile = dst.toPath().resolve(relativePath).toFile()
                    outputFile.parentFile.mkdirs()

                    def originalBytes = file.bytes
                    // Discover types prior enhancement
                    enhancer.discoverTypes(className, originalBytes)
                    // Perform the transformation
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
