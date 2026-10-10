package persist.lint.query.spi;

import java.util.List;
import java.util.ServiceLoader;

/**
 * Factory for creating {@link QueryParser} instances.
 *
 * <p>Implementations are discovered at runtime through the
 * {@link ServiceLoader} mechanism. Each implementation declares the
 * persistence namespace it supports (e.g. {@code "javax"} or
 * {@code "jakarta"}) so that the correct parser strategy can be
 * resolved for the target JPA specification version.</p>
 *
 * @see QueryParser
 */
public interface QueryParserFactory {

    /**
     * Returns the persistence namespace supported by this factory.
     *
     * @return the namespace identifier, for example {@code "javax"} for
     *         JPA&nbsp;2.x or {@code "jakarta"} for JPA&nbsp;3.x
     */
    String getSupportedNamespace();

    /**
     * Creates a new {@link QueryParser} configured with the given entity
     * class names.
     *
     * @param discoveredClassNames fully qualified class names of the JPA
     *                             entities to register in the metamodel
     * @return a ready-to-use query parser
     */
    QueryParser newQueryParser(List<String> discoveredClassNames);

    /**
     * Discovers and returns the {@link QueryParserFactory} implementation
     * that matches the given JPA specification version.
     *
     * <p>The version string is mapped to a persistence namespace
     * ({@code "javax"} for versions 2.0–2.2, {@code "jakarta"} for
     * versions 3.0–3.2) and the matching factory is located via the
     * {@link ServiceLoader} API.</p>
     *
     * @param jpaVersion the JPA specification version (e.g. {@code "2.2"}
     *                   or {@code "3.1"})
     * @return the factory implementation for the requested version
     * @throws IllegalArgumentException if the version is not recognized
     * @throws IllegalStateException    if no provider is registered for
     *                                  the resolved namespace
     */
    static QueryParserFactory newQueryParserFactory(String jpaVersion) {
        // Resolve target namespace based on the semantic spec version map token
        final String targetNamespace = switch (jpaVersion) {
            case "2.0", "2.1", "2.2" -> "javax";
            case "3.0", "3.1", "3.2" -> "jakarta";
            default -> throw new IllegalArgumentException("Unsupported JPA version: " + jpaVersion);
        };

        // Drive JVM ServiceLoader API to dynamically discover strategies loaded into the isolated classloader
        ServiceLoader<QueryParserFactory> loader = ServiceLoader.load(QueryParserFactory.class);

        return loader.stream()
            .map(ServiceLoader.Provider::get)
            .filter(factory -> factory.getSupportedNamespace().equals(targetNamespace))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Failed to discover a registered QueryParserFactory provider for namespace: " + targetNamespace));
    }

}
