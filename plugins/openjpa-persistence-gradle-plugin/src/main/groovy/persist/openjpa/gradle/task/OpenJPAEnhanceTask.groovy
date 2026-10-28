package persist.openjpa.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

/**
 * A custom Gradle task that handles compile-time bytecode enhancement for Apache OpenJPA entities.
 * <p>
 * This task forks an isolated JVM process running OpenJPA's official {@code PCEnhancer} tool
 * to inject persistence capability hooks directly into the compiled class binaries.
 * </p>
 *
 * @since 1.8.0
 */
@DisableCachingByDefault(because = "OpenJPA enhancement modifies class files in place")
abstract class OpenJPAEnhanceTask extends DefaultTask {

    /**
     * Toggles whether the enhancer should add a default no-argument constructor
     * to entity classes that lack one.
     * <p>
     * Defaults to {@code true}.
     * </p>
     *
     * @return A lazy property controlling default constructor generation.
     */
    @Optional
    @Input
    abstract Property<Boolean> getAddDefaultConstructor()

    /**
     * Toggles whether the enhancer should enforce property access restrictions
     * on persistent entity fields.
     * <p>
     * Defaults to {@code false}.
     * </p>
     *
     * @return A lazy property controlling property restriction enforcement.
     */
    @Optional
    @Input
    abstract Property<Boolean> getEnforcePropertyRestrictions()

    /**
     * The directory containing the processed resources (specifically META-INF/persistence.xml).
     *
     * @return The lazy directory property tracking the processed resources directory.
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getResourcesDir()

    /**
     * The directory containing the clean compiled .class files to be enhanced in place.
     *
     * @return The lazy directory property tracking the source classes directory.
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    abstract DirectoryProperty getSourceClassesDir()

    /**
     * The isolated output directory where the enhanced class files are written.
     *
     * @return The lazy directory property tracking the enhanced output directory.
     */
    @OutputDirectory
    abstract DirectoryProperty getTargetClassesDir()

    /**
     * The isolated runtime classpath carrying OpenJPA core toolset binaries.
     *
     * @return The file collection managing the OpenJPA enhancer toolset dependencies.
     */
    @CompileClasspath
    abstract ConfigurableFileCollection getEnhancerClasspath()

    /**
     * The complete compile dependencies of the module, required by OpenJPA to inspect relationship types.
     *
     * @return The file collection managing the project's compilation dependencies.
     */
    @CompileClasspath
    abstract ConfigurableFileCollection getCompileClasspath()

    /** The injected Gradle execution operations service for forking isolated JVM processes. */
    private final ExecOperations execOperations

    /**
     * Constructs a new {@code OpenJPAEnhanceTask} instance.
     *
     * @param execOperations The injected execution operations service for forking JVM processes.
     */
    @Inject
    OpenJPAEnhanceTask(ExecOperations execOperations) {
        this.execOperations = execOperations
    }

    /**
     * Executes the OpenJPA bytecode enhancement process.
     * <p>
     * Copies the clean compiled classes into the isolated target directory, assembles
     * the execution classpath, and forks the OpenJPA {@code PCEnhancer} tool in an
     * isolated JVM process to perform in-place enhancement.
     * </p>
     */
    @TaskAction
    void enhance() {
        boolean epr = enforcePropertyRestrictions.getOrElse(false)
        boolean adc = addDefaultConstructor.getOrElse(true)

        File inputClassesFolder = sourceClassesDir.get().asFile
        File resourcesFolder = resourcesDir.get().asFile
        File outputDir = targetClassesDir.get().asFile

        // Before running the enhancer, copy the clean unenhanced compiled classes
        // into our isolated target output folder so OpenJPA can modify them in-place safely.
        project.copy { spec ->
            spec.from(inputClassesFolder)
            spec.into(outputDir)
        }

        // Clean Execution Runtime View: Combine OpenJPA binaries, external compilation libraries,
        // and the meta-resource persistence.xml directory context into a single isolated file tree loop.
        def executionClasspath = project.files(
            enhancerClasspath,
            compileClasspath,
            inputClassesFolder,
            resourcesFolder
        )

        // Execute OpenJPA's PCEnhancer in an isolated JVM process
        execOperations.javaexec { spec ->
            spec.mainClass.set('org.apache.openjpa.enhance.PCEnhancer')
            spec.classpath = executionClasspath
            spec.args(
                '-directory', outputDir.absolutePath,
                '-addDefaultConstructor', adc,
                '-enforcePropertyRestrictions', epr
            )
        }
    }

}
