package persist.jakarta.gradle.task.registry

import org.gradle.api.GradleException
import persist.jakarta.gradle.task.delegate.DescriptorProcessorDelegate
import persist.jakarta.gradle.task.delegate.JPA20DescriptorProcessorDelegate
import persist.jakarta.gradle.task.delegate.JPA21DescriptorProcessorDelegate
import persist.jakarta.gradle.task.delegate.JPA30DescriptorProcessorDelegate
import persist.jakarta.gradle.task.delegate.JPA32DescriptorProcessorDelegate
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry Factory coordinating schema coordinates and mapping
 * specification version inputs to specialized Strategy Delegates.
 */
class JPAVersionStrategyRegistry {

    final String namespace
    final String schemaLocation
    final DescriptorProcessorDelegate delegate

    private static final Map<String, JPAVersionStrategyRegistry> REGISTRY = new ConcurrentHashMap<>()

    private JPAVersionStrategyRegistry(String namespace, String schemaLocation, DescriptorProcessorDelegate delegate) {
        this.namespace = namespace
        this.schemaLocation = schemaLocation
        this.delegate = delegate
    }

    // Centralize registration here to ensure JVM initializes all delegates immediately
    static {
        // 1. JPA 2.0 (Javax Old Era)
        registerDelegate(
            new JPA20DescriptorProcessorDelegate(),
            "http://java.sun.com/xml/ns/persistence",
            "http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd"
        )

        // 2. JPA 2.1 & 2.2 (Javax Modern Era)
        def jpa21Delegate = new JPA21DescriptorProcessorDelegate()
        jpa21Delegate.getSupportedVersions().forEach { version ->
            registerDelegate(
                jpa21Delegate,
                "http://xmlns.jcp.org/xml/ns/persistence",
                "http://xmlns.jcp.org/xml/ns/persistence/persistence_${version.replace('.', '_')}.xsd"
            )
        }

        // 3. JPA 3.0 & 3.1 (Jakarta Core)
        // We handle individual schemas mapping dynamically inside registration
        registerDelegate(new JPA30DescriptorProcessorDelegate(), "https://jakarta.ee/xml/ns/persistence", "https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd")
        registerDelegate(new JPA30DescriptorProcessorDelegate(), "https://jakarta.ee/xml/ns/persistence", "https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd")

        // 4. JPA 3.2 (Jakarta Modern)
        registerDelegate(
            new JPA32DescriptorProcessorDelegate(),
            "https://jakarta.ee/xml/ns/persistence",
            "https://jakarta.ee/xml/ns/persistence/persistence_3_2.xsd"
        )
    }

    /**
     * Special helper for registering versions where the schema name matches the version string.
     */
    private static void registerDelegate(DescriptorProcessorDelegate delegate, String namespace, String schemaFileName) {
        delegate.getSupportedVersions().forEach { version ->
            // If we passed an explicit .xsd target file structure string, apply it, else generate dynamically
            String finalSchemaLoc = schemaFileName.endsWith(".xsd")
                ? schemaFileName
                : "https://jakarta.ee/persistence_${version.replace('.', '_')}.xsd"

            REGISTRY.put(version, new JPAVersionStrategyRegistry(namespace, finalSchemaLoc, delegate))
        }
    }

    /**
     * Resolves the complete strategy metadata matrix based on the requested version.
     */
    static JPAVersionStrategyRegistry resolve(String version) {
        JPAVersionStrategyRegistry strategy = REGISTRY.get(version)

        if (strategy == null) {
            List<String> supportedVersionsList = new ArrayList<>(REGISTRY.keySet()).sort()
            throw new GradleException(
                "Unsupported JPA specification version '${version}' declared in build configuration. " +
                "Supported versions are: ${supportedVersionsList.join(', ')}"
            )
        }

        return strategy
    }

}
