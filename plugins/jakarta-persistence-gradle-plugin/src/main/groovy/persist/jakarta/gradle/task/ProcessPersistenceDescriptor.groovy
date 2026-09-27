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

@DisableCachingByDefault(because = "Process overwrite persistence.xml in place")
abstract class ProcessPersistenceDescriptor extends DefaultTask {

    @Input
    abstract Property<String> getXmlVersion()

    @Nested
    abstract ListProperty<PersistenceUnitExtension> getUnits()

    @Input
    abstract ListProperty<String> getJarFileNames()

    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.ABSOLUTE)
    abstract RegularFileProperty getPersistenceXml()

    @OutputFile
    abstract RegularFileProperty getDestinationFile()

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

    static class Helper {

        private static void mergeScalarNode(Node unitNode, String tagName, String fallbackValue) {
            List<?> matchingNodes = (List<?>) unitNode.get(tagName)
            if (matchingNodes.isEmpty() && fallbackValue != null) {
                unitNode.appendNode(tagName, fallbackValue)
            }
        }

    }

}
