package persist.eclipse.gradle.task

import groovy.xml.XmlParser
import org.gradle.api.DefaultTask
import org.gradle.work.DisableCachingByDefault
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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
@DisableCachingByDefault(because = "Weaving modifies classes in place")
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
     * The input collection of resolved JAR file dependencies declared via {@code <jar-file>} entries
     * in the {@code persistence.xml} descriptor.
     * <p>
     * These JARs are matched by name and staged into a temporary sandbox directory at their expected
     * relative paths before weaving, allowing the EclipseLink {@code StaticWeave} processor to
     * correctly resolve cross-module entity references in multi-module projects.
     * </p>
     *
     * @return The file collection managing additional jar file dependencies.
     * @since 1.5.1
     */
    @Internal
    abstract ConfigurableFileCollection getJarFileClasspath()

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
     * Creates a temporary staging sandbox directory, copies processed resources into it, then
     * dynamically parses the {@code persistence.xml} to discover {@code <jar-file>} entries.
     * Matching JAR files from the {@link #getJarFileClasspath() jarFileClasspath} are resolved
     * by name and copied into their expected relative paths within the sandbox, ensuring the
     * EclipseLink {@code StaticWeave} processor can locate cross-module dependencies correctly.
     * </p>
     * <p>
     * Finally, spawns an isolated Java process running the
     * {@code org.eclipse.persistence.tools.weaving.jpa.StaticWeave} main class, using the
     * sandbox as the {@code -persistenceinfo} source to map byte structures cleanly from
     * the raw source to the enhanced output target.
     * </p>
     *
     * @since 1.5.1 Added staging sandbox with dynamic JAR resolution from persistence.xml
     */
    @TaskAction
    void weave() {
        def resourcesFolder = getResourcesDir().get().asFile
        def sourceFolder = getSourceClassesDir().get().asFile
        def targetFolder = getTargetClassesDir().get().asFile

        // Create a temporary staging sandbox directory inside the build directory
        // to collect all referenced dependencies in a single flat path layout
        File stagingSandboxDir = new File(temporaryDir, "persistence-info-sandbox")
        if (stagingSandboxDir.exists()) {
            stagingSandboxDir.deleteDir()
        }
        stagingSandboxDir.mkdirs()

        // 1. Sync over the base processed meta resources assets hierarchy (META-INF/persistence.xml)
        project.copy { spec ->
            spec.from(resourcesFolder)
            spec.into(stagingSandboxDir)
        }

        // 2. Parse the generated persistence.xml dynamically
        // This discovers custom paths (e.g. 'lib/', '../../') and maps them perfectly
        File pXmlFile = new File(stagingSandboxDir, "META-INF/persistence.xml")
        List<String> expectedJarPaths = []

        if (pXmlFile.exists()) {
            try {
                def xmlParser = new XmlParser(false, false)
                def rootNode = xmlParser.parse(pXmlFile)
                List<?> unitNodes = (List<?>) rootNode.get("persistence-unit")

                unitNodes.each { Object unitObj ->
                    List<?> jarNodes = (List<?>) ((Node) unitObj).get("jar-file")
                    jarNodes.each { Object jarNode ->
                        String jarPathText = ((Node) jarNode).text()?.trim()
                        if (jarPathText) {
                            expectedJarPaths.add(jarPathText)
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ Warning: Failed to parse persistence.xml for dynamic JAR routing layouts: " + e.message)
            }
        }

        // 3. Match and copy your runtime JAR files into their expected relative locations
        Set<File> availableJars = jarFileClasspath.getFiles()

        expectedJarPaths.each { String expectedPath ->
            // Extract the simple file name from the expected path string (e.g. "lib/shared.jar" -> "shared.jar")
            String simpleJarName = expectedPath.contains('/') ? expectedPath.substring(expectedPath.lastIndexOf('/') + 1) : expectedPath

            // Find the matching file in our available project configurations classpath pool
            File matchingJar = availableJars.find { it.name == simpleJarName }

            if (matchingJar && matchingJar.exists()) {
                // Resolve the exact destination path relative to our sandbox directory root
                File targetStagedFile = new File(stagingSandboxDir, expectedPath)

                // Automatically create any nested sub-directories (like lib/) if required!
                targetStagedFile.parentFile.mkdirs()

                Files.copy(matchingJar.toPath(), targetStagedFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }

        execOperations.javaexec { spec ->
            spec.mainClass.set('org.eclipse.persistence.tools.weaving.jpa.StaticWeave')
            spec.classpath = weaveClasspath
            spec.args(
                '-persistenceinfo', stagingSandboxDir.absolutePath,
                '-classpath', compileClasspath.asPath,
                '-loglevel', 'FINE',
                sourceFolder.absolutePath,
                targetFolder.absolutePath
            )
        }
    }

}
