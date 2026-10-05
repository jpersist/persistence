package persist.jakarta.gradle.task.delegate

import groovy.xml.MarkupBuilder
import groovy.xml.XmlParser
import groovy.xml.XmlUtil
import persist.jakarta.gradle.extension.PersistenceUnitExtension
import persist.jakarta.gradle.task.registry.JPAVersionStrategyRegistry

import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

/**
 * Base implementation of {@link DescriptorProcessorDelegate} that provides the
 * common merge and generate logic for all JPA specification versions.
 * <p>
 * Concrete subclasses only need to declare which specification versions they
 * support via {@link DescriptorProcessorDelegate#getSupportedVersions()}. All XML node manipulation is
 * isolated inside the static {@code Helper} inner class to prevent Groovy's
 * dynamic method dispatch from routing calls through Gradle's decorated task
 * proxy.
 * </p>
 *
 * @since 1.4.0
 */
abstract class AbstractDescriptorProcessorDelegate implements DescriptorProcessorDelegate {

    @Override
    void process(File source, File target, List<String> managedClasses, List<String> resolvedJars,
                 List<PersistenceUnitExtension> configuredUnits, Map<String, String> settings, String xmlVersion) {
        def registry = JPAVersionStrategyRegistry.resolve(xmlVersion)

        if (source != null && source.exists()) {
            Helper.mergeDescriptor(source, target, managedClasses, resolvedJars, configuredUnits, settings, registry)
        } else {
            Helper.generateDescriptor(target, managedClasses, resolvedJars, configuredUnits, settings, registry)
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
         */
        private static void mergeDescriptor(File source, File target, List<String> managedClasses, List<String> resolvedJars,
                                            List<PersistenceUnitExtension> configuredUnits, Map<String, String> settings,
                                            JPAVersionStrategyRegistry registry) {
            XmlParser parser = new XmlParser(false, false)
            Node persistence = parser.parse(source)

            // Clear and rewrite standard modern schema boundaries
            persistence.attributes().clear()
            persistence.attributes().put("version", registry.version)
            persistence.attributes().put("xmlns", registry.namespace)
            persistence.attributes().put("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
            persistence.attributes().put("xsi:schemaLocation", "${registry.namespace} ${registry.schemaLocation}")

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
         */
        private static void generateDescriptor(File target, List<String> managedClasses, List<String> resolvedJars,
                                               List<PersistenceUnitExtension> configuredUnits, Map<String, String> settings,
                                               JPAVersionStrategyRegistry registry) {
            // Generate via MarkupBuilder into an in-memory string buffer instead of straight to a file writer
            StringWriter rawXmlWriter = new StringWriter()
            MarkupBuilder xml = new MarkupBuilder(rawXmlWriter)
            xml.setDoubleQuotes(true)
            xml.omitEmptyAttributes = true

            Map<String, String> rootAttributes = [
                "version": registry.version,
                "xmlns": registry.namespace,
                "xmlns:xsi": "http://www.w3.org/2001/XMLSchema-instance",
                "xsi:schemaLocation": "${registry.namespace} ${registry.schemaLocation}"
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
         * supplied via the {@link persist.jakarta.gradle.task.ProcessPersistenceDescriptor#getTransformerSettings()} map.
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
