package persist.jakarta.gradle.task.registry

import org.gradle.api.GradleException
import persist.jakarta.gradle.task.delegate.DescriptorProcessorDelegate
import persist.jakarta.gradle.task.delegate.JPA20DescriptorProcessorDelegate
import persist.jakarta.gradle.task.delegate.JPA21DescriptorProcessorDelegate
import persist.jakarta.gradle.task.delegate.JPA30DescriptorProcessorDelegate
import persist.jakarta.gradle.task.delegate.JPA32DescriptorProcessorDelegate
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry factory that maps JPA specification version strings to their
 * corresponding XML namespace, schema location, and
 * {@link DescriptorProcessorDelegate} strategy implementation.
 * <p>
 * All supported versions are registered eagerly in a static initializer
 * block. At task execution time, {@link JPAVersionStrategyRegistry#resolve(String)} performs a
 * thread-safe lookup and returns the fully configured strategy metadata
 * or throws a {@link org.gradle.api.GradleException} for unsupported
 * versions.
 * </p>
 *
 * @since 1.4.0
 */
class JPAVersionStrategyRegistry {

    /** The XML namespace URI for the {@code <persistence>} root element. */
    final String namespace

    /** The full URL of the XSD schema used in {@code xsi:schemaLocation}. */
    final String schemaLocation

    /** The version-specific strategy that performs descriptor processing. */
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
     * Registers a delegate for each of its supported versions, associating
     * the given namespace and schema location with the version key.
     *
     * @param delegate        The strategy implementation to register.
     * @param namespace       The XML namespace URI.
     * @param schemaFileName  The XSD schema location URL.
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
     * Resolves the complete strategy metadata for the requested JPA
     * specification version.
     *
     * @param version The JPA version string (e.g. {@code "3.2"}).
     * @return The matching registry entry containing namespace, schema, and delegate.
     * @throws org.gradle.api.GradleException if the version is not supported.
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
