package persist.jakarta.gradle.task

import groovy.xml.MarkupBuilder
import groovy.xml.XmlParser
import groovy.xml.XmlUtil
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

import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

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

        if (sourceFile != null && sourceFile.exists()) {
            Helper.mergeDescriptor(sourceFile, destination, managedClasses, resolvedJars, configuredUnits, settings, versionStr)
        } else {
            Helper.generateDescriptor(destination, managedClasses, resolvedJars, configuredUnits, settings, versionStr)
        }
    }

    /**
     * Static helper that encapsulates all XML node manipulation for
     * descriptor generation and merging.
     * <p>
     * Isolating these methods in a plain static inner class prevents Groovy's
     * dynamic method dispatch from routing calls through Gradle's decorated
     * task proxy, which can cause unexpected {@code MissingMethodException}
     * errors at runtime.
     * </p>
     */
    private static class Helper {

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

        /**
         * Merges extension-defined overrides and resolved JAR entries into an
         * existing {@code persistence.xml}, maintaining strict XSD schema element ordering rules.
         *
         * @param source          The user-provided source descriptor.
         * @param target          The destination file to write.
         * @param managedClasses  The list of managed class names.
         * @param resolvedJars    The list of resolved JAR file names.
         * @param configuredUnits The persistence-unit extension configurations.
         * @param settings        The XML transformer output property overrides.
         * @param xmlVersion      The JPA specification version for the root element.
         */
        private static void mergeDescriptor(File source, File target, List<String> managedClasses, List<String> resolvedJars,
                                            List<PersistenceUnitExtension> configuredUnits, Map<String, String> settings,
                                            String xmlVersion) {
            XmlParser parser = new XmlParser(false, false)
            Node persistence = parser.parse(source)

            persistence.attributes().put("version", xmlVersion)

            List<?> unitNodes = (List<?>) persistence.get("persistence-unit")
            unitNodes.each { Object unitObj ->
                Node unitNode = (Node) unitObj
                String unitName = (String) unitNode.attribute("name")

                PersistenceUnitExtension extensionConfig = configuredUnits.find { it.name == unitName }

                // 1. Description & Provider Scalars
                mergeScalarNode(unitNode, "provider", extensionConfig?.provider?.orNull)
                mergeScalarNode(unitNode, "description", extensionConfig?.description?.orNull)

                // 2. Data Sources (JTA vs non-JTA)
                if (extensionConfig?.dataSource?.isPresent()) {
                    boolean jta = extensionConfig.jta.getOrElse(false)
                    String targetNode = jta ? "jta-data-source" : "non-jta-data-source"
                    String alternativeNode = jta ? "non-jta-data-source" : "jta-data-source"

                    List<?> contradictingNodes = (List<?>) unitNode.get(alternativeNode)
                    new ArrayList<>(contradictingNodes).each { Object oldNode ->
                        unitNode.remove((Node) oldNode)
                    }
                    mergeScalarNode(unitNode, targetNode, extensionConfig.dataSource.get())
                }

                // 3. Mapping Files Collection
                if (extensionConfig?.mappingFiles?.isPresent() && !extensionConfig.mappingFiles.get().isEmpty()) {
                    List<?> existingMappingFiles = (List<?>) unitNode.get("mapping-file")
                    // Only fall back to extension defaults if the template layout contains no mapping file definitions
                    if (existingMappingFiles.isEmpty()) {
                        extensionConfig.mappingFiles.get().each { String mappingFile ->
                            unitNode.appendNode("mapping-file", mappingFile)
                        }
                    }
                }

                // 4. Jar Files (Always override completely if our gradle configuration contains active elements)
                if (!resolvedJars.isEmpty()) {
                    List<?> jarFilesList = (List<?>) unitNode.get("jar-file")
                    new ArrayList<>(jarFilesList).each { Object jarNode ->
                        unitNode.remove((Node) jarNode)
                    }
                    resolvedJars.each { String jarName ->
                        unitNode.appendNode("jar-file", jarName)
                    }
                }

                // 5. Classes (Always override completely if includeAllClasses is toggled on)
                if (extensionConfig?.includeAllClasses?.orElse(false)) {
                    if (!managedClasses.isEmpty()) {
                        List<?> classesList = (List<?>) unitNode.get("class")
                        new ArrayList<>(classesList).each { Object jarNode ->
                            unitNode.remove((Node) jarNode)
                        }
                        managedClasses.each { String className ->
                            unitNode.appendNode("class", className)
                        }
                    }
                }

                // 6. Exclude Unlisted Classes (Emits empty structural marker tag if flag matches true)
                if (extensionConfig?.excludedUnlistedClasses?.isPresent()) {
                    List<?> existingExcludeNode = (List<?>) unitNode.get("exclude-unlisted-classes")
                    if (existingExcludeNode.isEmpty() && extensionConfig.excludedUnlistedClasses.get()) {
                        unitNode.appendNode("exclude-unlisted-classes")
                    }
                }

                // 7. Shared Cache Mode Scalar
                if (extensionConfig?.sharedCacheMode?.isPresent()) {
                    List<?> existingCacheNode = (List<?>) unitNode.get("shared-cache-mode")
                    if (existingCacheNode.isEmpty()) {
                        // Standardizes string/boolean configurations into required UPPERCASE XSD enum tokens (e.g., ENABLE_SELECTIVE)
                        String mode = extensionConfig.sharedCacheMode.get().toString().toUpperCase()
                        unitNode.appendNode("shared-cache-mode", mode)
                    }
                }

                // 8. Validation Mode Scalar
                if (extensionConfig?.validationMode?.isPresent()) {
                    List<?> existingValidationNode = (List<?>) unitNode.get("validation-mode")
                    if (existingValidationNode.isEmpty()) {
                        // Standardizes configurations into required UPPERCASE XSD tokens (e.g., AUTO, CALLBACK, NONE)
                        String mode = extensionConfig.validationMode.get().toString().toUpperCase()
                        unitNode.appendNode("validation-mode", mode)
                    }
                }

                // 9. Vendor Specific Properties Block
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

                // 10. Strict Schema XSD Ordering Enforcement
                Map<String, Integer> elementOrderWeights = [
                    "description"              : 1,
                    "provider"                 : 2,
                    "jta-data-source"          : 3,
                    "non-jta-data-source"      : 3,
                    "mapping-file"             : 4,
                    "jar-file"                 : 5,
                    "class"                    : 6,
                    "exclude-unlisted-classes" : 7,
                    "shared-cache-mode"        : 8,
                    "validation-mode"          : 9,
                    "properties"               : 10
                ]

                List<?> originalChildren = new ArrayList<>(unitNode.children())
                originalChildren.each { Object child -> unitNode.remove((Node) child) }

                originalChildren.sort { Object a, Object b ->
                    int weightA = elementOrderWeights.get(((Node) a).name().toString(), 99)
                    int weightB = elementOrderWeights.get(((Node) b).name().toString(), 99)
                    return weightA <=> weightB
                }.each { Object child ->
                    unitNode.append((Node) child)
                }
            }

            StringWriter rawXmlWriter = new StringWriter()
            XmlUtil.serialize(persistence, rawXmlWriter)

            prettyPrint(rawXmlWriter.toString(), target, settings)
        }

        /**
         * Generates a complete {@code persistence.xml} from the extension DSL
         * configuration.
         *
         * @param target          The destination file to write.
         * @param managedClasses  The list of managed class names.
         * @param resolvedJars    The list of resolved JAR file names.
         * @param configuredUnits The persistence-unit extension configurations.
         * @param settings        The XML transformer output property overrides.
         * @param xmlVersion      The JPA specification version for the root element.
         */
        private static void generateDescriptor(File target, List<String> managedClasses, List<String> resolvedJars,
                                               List<PersistenceUnitExtension> configuredUnits, Map<String, String> settings,
                                               String xmlVersion) {
            // Generate via MarkupBuilder into an in-memory string buffer instead of straight to a file writer
            StringWriter rawXmlWriter = new StringWriter()
            MarkupBuilder xml = new MarkupBuilder(rawXmlWriter)
            xml.setDoubleQuotes(true)
            xml.omitEmptyAttributes = true

            Map<String, String> rootAttributes = [
                "version": xmlVersion,
                "xmlns": "https://jakarta.ee/xml/ns/persistence",
                "xmlns:xsi": "http://www.w3.org/2001/XMLSchema-instance",
                "xsi:schemaLocation": "https://jakarta.ee/xml/ns/persistence https://jakarta.ee/xml/ns/persistence/persistence_${xmlVersion.replace('.', '_')}.xsd"
            ]

            xml.persistence(rootAttributes) {
                configuredUnits.each { PersistenceUnitExtension unit ->
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

                        if (unit.mappingFiles.isPresent()) {
                            unit.mappingFiles.get().each { String mappingFile ->
                                "mapping-file"(mappingFile)
                            }
                        }

                        resolvedJars.each { String jarName ->
                            "jar-file"(jarName)
                        }

                        if (unit.includeAllClasses.getOrElse(false)) {
                            managedClasses.each { String className ->
                                "class"(className)
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

            prettyPrint(rawXmlWriter.toString(), target, settings)
        }

        /**
         * Normalizes whitespace in the raw XML string and writes a cleanly
         * formatted {@code persistence.xml} to the target file.
         * <p>
         * The method strips extraneous whitespace between XML tags, then applies
         * the transformer output properties (indentation, encoding, XML declaration)
         * supplied via the {@link ProcessPersistenceDescriptor#getTransformerSettings()} map.
         * </p>
         *
         * @param rawXml     The raw XML string to format.
         * @param targetFile The destination file to write.
         * @param settings   The transformer output property overrides.
         * @since 1.2.2
         */
        private static void prettyPrint(String rawXml, File targetFile, Map<String, String> settings) {
            // Remove blank line wraps, multi-whitespaces gaps, and tab fragments
            String sanitizedXml = rawXml
                .replaceAll(/>\s+</, '><')
                .replaceAll(/xsi:schemaLocation="\s+/, 'xsi:schemaLocation="')
                .trim()

            TransformerFactory factory = TransformerFactory.newInstance()

            // Dynamic lookups for setting indentation spaces seamlessly
            String indentAmount = settings.get("indent-amount") ?: settings.get("{http://xml.apache.org/xslt}indent-amount") ?: "4"
            try {
                factory.setAttribute("indent-number", Integer.parseInt(indentAmount))
            } catch (Exception ignored) {}

            Transformer transformer = factory.newTransformer()

            // Dynamically apply every configured property passed via the extension DSL mapping array
            settings.each { String key, String value ->
                // Certain legacy implementations use custom key mappings; translate or inject them directly
                if (key == "indent-amount") {
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", value)
                } else if (key == "omit-xml-declaration") {
                    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, value)
                } else if (key == "encoding") {
                    transformer.setOutputProperty(OutputKeys.ENCODING, value)
                } else if (key == "indent") {
                    transformer.setOutputProperty(OutputKeys.INDENT, value)
                } else {
                    // Fallback for custom namespace parameters
                    transformer.setOutputProperty(key, value)
                }
            }

            targetFile.withWriter("UTF-8") { Writer writer ->
                transformer.transform(
                    new StreamSource(new StringReader(sanitizedXml)),
                    new StreamResult(writer)
                )
            }
        }

    }

}
