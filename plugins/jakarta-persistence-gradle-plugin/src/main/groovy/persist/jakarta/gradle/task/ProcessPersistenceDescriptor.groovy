package persist.jakarta.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import persist.jakarta.gradle.extension.PersistenceUnitExtension
import persist.jakarta.gradle.task.registry.JPAVersionStrategyRegistry

/**
 * Gradle task that generates or merges a JPA {@code persistence.xml} descriptor.
 * <p>
 * When a user-provided {@code src/main/resources/META-INF/persistence.xml}
 * exists, the task operates in <em>merge</em> mode: it parses the existing
 * file, applies extension-defined overrides (provider, data source,
 * properties), injects resolved {@code <jar-file>} entries, and writes the
 * result to the {@link ProcessPersistenceDescriptor#getDestinationFile() destination}.
 * </p>
 * <p>
 * When no source descriptor is present, the task operates in
 * <em>generate</em> mode: it builds a complete {@code persistence.xml} from
 * scratch using the {@link persist.jakarta.gradle.extension.PersistenceExtension}
 * DSL configuration.
 * </p>
 *
 * @since 1.1.0
 */
@DisableCachingByDefault(because = "Process overwrite persistence.xml in place")
abstract class ProcessPersistenceDescriptor extends DefaultTask {

    /**
     * The JPA specification version to set on the root {@code <persistence>}
     * element.
     *
     * @return The lazy property tracking the XML version attribute.
     */
    @Input
    abstract Property<String> getXmlVersion()

    /**
     * The list of persistence-unit extension configurations to process.
     *
     * @return The lazy list property of {@link PersistenceUnitExtension} instances.
     */
    @Nested
    abstract ListProperty<PersistenceUnitExtension> getUnits()

    /**
     * Managed class names from the current source set to inject as
     * {@code <class>} elements
     *
     * @return The lazy list property of class names.
     */
    @Input
    abstract ListProperty<String> getIncludedClasses()

    /**
     * Resolved JAR file names from the {@code jarFile} configuration to inject
     * as {@code <jar-file>} elements.
     *
     * @return The lazy list property of JAR file name strings.
     */
    @Input
    abstract ListProperty<String> getJarFileNames()

    /**
     * The optional user-provided {@code persistence.xml} source file.
     * <p>
     * When present, the task operates in merge mode; when absent, it
     * generates the descriptor from scratch.
     * </p>
     *
     * @return The lazy file property tracking the source descriptor.
     */
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.ABSOLUTE)
    abstract RegularFileProperty getPersistenceXml()

    /**
     * The output file where the generated or merged {@code persistence.xml}
     * is written.
     *
     * @return The lazy file property tracking the destination file.
     */
    @OutputFile
    abstract RegularFileProperty getDestinationFile()

    /**
     * Custom output properties applied to the XML {@link javax.xml.transform.Transformer}
     * that formats the final {@code persistence.xml}.
     * <p>
     * These settings control indentation, encoding, XML declaration, and other
     * serialization aspects. Values are populated from
     * {@link persist.jakarta.gradle.extension.PersistenceExtension#getOutputProperties()}.
     * </p>
     *
     * @return The lazy map property tracking the transformer output settings.
     * @since 1.2.2
     */
    @Input
    abstract MapProperty<String, String> getTransformerSettings()

    /**
     * Executes the descriptor processing.
     * <p>
     * Delegates to {@link Helper#mergeDescriptor} when a source file exists,
     * or to {@link Helper#generateDescriptor} otherwise. All XML node
     * manipulation is performed inside the static {@link Helper} inner class
     * to avoid Groovy dynamic method resolution conflicts with Gradle's
     * decorated task subclasses.
     * </p>
     */
    @TaskAction
    void process() {
        File destination = destinationFile.get().asFile
        File sourceFile = persistenceXml.orNull?.asFile
        List<String> resolvedJars = jarFileNames.get()
        List<String> managedClasses = includedClasses.get()
        List<PersistenceUnitExtension> configuredUnits = units.get()
        Map<String, String> settings = transformerSettings.get()
        String versionStr = xmlVersion.get()

        // 1. Resolve the explicit strategy routing from the factory registry
        JPAVersionStrategyRegistry strategyConfig = JPAVersionStrategyRegistry.resolve(versionStr)

        // 2. Delegate the execution graph out to the isolated strategy implementation
        strategyConfig.delegate.process(
            sourceFile, destination, managedClasses, resolvedJars, configuredUnits, settings, versionStr
        )
    }

}
