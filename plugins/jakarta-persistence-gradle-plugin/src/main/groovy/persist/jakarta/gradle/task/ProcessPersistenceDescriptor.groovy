package persist.jakarta.gradle.task

import groovy.xml.XmlParser
import groovy.xml.XmlUtil
import groovy.xml.MarkupBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
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

/**
 * Gradle task that generates or merges a JPA {@code persistence.xml} descriptor.
 * <p>
 * When a user-provided {@code src/main/resources/META-INF/persistence.xml}
 * exists, the task operates in <em>merge</em> mode: it parses the existing
 * file, applies extension-defined overrides (provider, data source,
 * properties), injects resolved {@code <jar-file>} entries, and writes the
 * result to the {@link #getDestinationFile() destination}.
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
     * Executes the descriptor processing.
     * <p>
     * Delegates to {@code merge} when a source file exists, or to
     * {@code generate} otherwise.
     * </p>
     */
    @TaskAction
    void process() {
        File destination = destinationFile.get().asFile
        File sourceFile = persistenceXml.orNull?.asFile
        List<String> resolvedJars = jarFileNames.get()

        if (sourceFile != null && sourceFile.exists()) {
            merge(sourceFile, destination, resolvedJars)
        } else {
            generate(destination, resolvedJars)
        }
    }

    /**
     * Merges extension-defined overrides and resolved JAR entries into an
     * existing {@code persistence.xml}.
     *
     * @param source       The user-provided source descriptor.
     * @param target       The destination file to write.
     * @param resolvedJars The list of resolved JAR file names.
     */
    private void merge(File source, File target, List<String> resolvedJars) {
        XmlParser parser = new XmlParser(false, false)
        Node persistence = parser.parse(source)

        persistence.attributes().put("version", xmlVersion.get())

        List<?> unitNodes = (List<?>) persistence.get("persistence-unit")
        unitNodes.each { Object unitObj ->
            Node unitNode = (Node) unitObj
            String unitName = (String) unitNode.attribute("name")

            PersistenceUnitExtension extensionConfig = units.get().find { it.name == unitName }

            Helper.mergeScalarNode(unitNode, "provider", extensionConfig?.provider?.orNull)
            Helper.mergeScalarNode(unitNode, "description", extensionConfig?.description?.orNull)

            if (extensionConfig?.dataSource?.isPresent()) {
                boolean jta = extensionConfig.jta.getOrElse(false)
                String targetNode = jta ? "jta-data-source" : "non-jta-data-source"
                String alternativeNode = jta ? "non-jta-data-source" : "jta-data-source"

                List<?> contradictingNodes = (List<?>) unitNode.get(alternativeNode)
                new ArrayList<>(contradictingNodes).each { Object oldNode ->
                    unitNode.remove((Node) oldNode)
                }
                Helper.mergeScalarNode(unitNode, targetNode, extensionConfig.dataSource.get())
            }

            if (extensionConfig?.properties?.isPresent() && !extensionConfig.properties.get().isEmpty()) {
                List<?> propertiesList = (List<?>) unitNode.get("properties")
                Node propertiesNode = propertiesList.isEmpty() ? unitNode.appendNode("properties") : (Node) propertiesList.get(0)

                Map<String, String> extProps = extensionConfig.properties.get()
                extProps.forEach { String key, String val ->
                    List<?> propertyNodes = (List<?>) propertiesNode.get("property")
                    Object existingProp = propertyNodes.find { Object propObj ->
                        ((Node) propObj).attribute("name") == key
                    }
                    if (existingProp == null) {
                        propertiesNode.appendNode("property", [name: key, value: val])
                    }
                }
            }

            if (!resolvedJars.isEmpty()) {
                List<?> jarFilesList = (List<?>) unitNode.get("jar-file")
                new ArrayList<>(jarFilesList).each { Object jarNode ->
                    unitNode.remove((Node) jarNode)
                }
                resolvedJars.each { String jarName ->
                    unitNode.appendNode("jar-file", jarName)
                }
            }
        }

        target.withWriter("UTF-8") { Writer writer ->
            XmlUtil.serialize(persistence, writer)
        }
    }

    /**
     * Generates a complete {@code persistence.xml} from the extension DSL
     * configuration.
     *
     * @param target       The destination file to write.
     * @param resolvedJars The list of resolved JAR file names.
     */
    private void generate(File target, List<String> resolvedJars) {
        target.withWriter("UTF-8") { Writer writer ->
            MarkupBuilder xml = new MarkupBuilder(writer)
            xml.setDoubleQuotes(true)
            xml.omitEmptyAttributes = true

            Map<String, String> rootAttributes = [
                "version": xmlVersion.get(),
                "xmlns": "https://jakarta.ee/xml/ns/persistence",
                "xmlns:xsi": "http://www.w3.org/2001/XMLSchema-instance",
                "xsi:schemaLocation": "https://jakarta.ee/xml/ns/persistence https://jakarta.ee/xml/ns/persistence/persistence_${xmlVersion.get().replace('.', '_')}.xsd"
            ]

            xml.persistence(rootAttributes) {
                units.get().each { PersistenceUnitExtension unit ->
                    "persistence-unit"([name: unit.name, "transaction-type": unit.transactionType.get()]) {
                        if (unit.description.isPresent()) xml.invokeMethod("description", unit.description.get())
                        if (unit.provider.isPresent()) xml.invokeMethod("provider", unit.provider.get())

                        if (unit.dataSource.isPresent()) {
                            boolean jta = unit.jta.getOrElse(false)
                            if (jta) {
                                "jta-data-source"(unit.dataSource.get())
                            } else {
                                "non-jta-data-source"(unit.dataSource.get())
                            }
                        }

                        resolvedJars.each { String jarName ->
                            "jar-file"(jarName)
                        }

                        if (unit.mappingFiles.isPresent()) {
                            unit.mappingFiles.get().each { String mappingFile ->
                                "mapping-file"(mappingFile)
                            }
                        }

                        if (unit.excludedUnlistedClasses.isPresent() && unit.excludedUnlistedClasses.get()) {
                            "exclude-unlisted-classes"()
                        }

                        if (unit.sharedCacheMode.isPresent()) {
                            "shared-cache-mode"(unit.sharedCacheMode.get().toString().toUpperCase())
                        }

                        if (unit.validationMode.isPresent()) {
                            "validation-mode"(unit.validationMode.get().toString().toUpperCase())
                        }

                        if (unit.properties.isPresent() && !unit.properties.get().isEmpty()) {
                            properties {
                                unit.properties.get().each { String propName, String propValue ->
                                    property(name: propName, value: propValue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Internal utility methods for XML node manipulation during merge.
     */
    static class Helper {

        /**
         * Appends a child element with the given tag name and text content to
         * the unit node, but only when no element with that tag already exists
         * and the fallback value is non-{@code null}.
         *
         * @param unitNode      The persistence-unit XML node.
         * @param tagName       The child element tag name.
         * @param fallbackValue The text content to set, or {@code null} to skip.
         */
        private static void mergeScalarNode(Node unitNode, String tagName, String fallbackValue) {
            List<?> matchingNodes = (List<?>) unitNode.get(tagName)
            if (matchingNodes.isEmpty() && fallbackValue != null) {
                unitNode.appendNode(tagName, fallbackValue)
            }
        }

    }

}
