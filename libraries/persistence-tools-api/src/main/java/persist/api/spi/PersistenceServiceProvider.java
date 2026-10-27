package persist.api.spi;

import persist.lint.api.spi.PersistenceLinterFactory;

import java.util.ServiceLoader;

/**
 * Root service-provider interface for all persistence tool providers.
 *
 * <p>Each provider declares the JPA namespace it supports (e.g.&nbsp;{@code "jakarta"}
 * or {@code "javax"}) via {@link #getSupportedNamespace()}. Concrete subtypes
 * such as {@link persist.hibernate.enhance.spi.HibernateEnhancerProvider} and
 * {@link persist.lint.api.spi.PersistenceLinterFactory} are discovered at runtime
 * through the {@link java.util.ServiceLoader} mechanism.</p>
 *
 * <p>The static helper {@link #getServiceProvider(String, Class)} maps a JPA
 * specification version string to the corresponding namespace and locates the
 * matching provider from the service loader.</p>
 *
 * @since 1.0.0
 * @see persist.lint.api.spi.PersistenceLinterFactory
 * @see persist.hibernate.enhance.spi.HibernateEnhancerProvider
 */
public interface PersistenceServiceProvider {

    /**
     * Returns the persistence namespace supported by this provider.
     *
     * @return the namespace identifier, for example {@code "javax"} for
     *         JPA&nbsp;2.x or {@code "jakarta"} for JPA&nbsp;3.x
     */
    String getSupportedNamespace();

    /**
     * Discovers and returns the {@code PersistenceServiceProvider} of the
     * requested type whose {@linkplain #getSupportedNamespace() namespace}
     * matches the given JPA specification version.
     *
     * <p>The version string is mapped to a namespace as follows:</p>
     * <ul>
     *   <li>{@code "2.0"}, {@code "2.1"}, {@code "2.2"} &rarr; {@code "javax"}</li>
     *   <li>{@code "3.0"}, {@code "3.1"}, {@code "3.2"} &rarr; {@code "jakarta"}</li>
     * </ul>
     *
     * @param jpaVersion    the JPA specification version (e.g.&nbsp;{@code "3.1"})
     * @param providerClass the provider subtype to look up
     * @param <T>           the provider type
     * @return the matching provider instance
     * @throws IllegalArgumentException if {@code jpaVersion} is not recognised
     * @throws IllegalStateException    if no registered provider matches the
     *                                  resolved namespace
     */
    static <T extends PersistenceServiceProvider> T getServiceProvider(String jpaVersion, Class<T> providerClass) {
        // Resolve target namespace based on the semantic spec version map token
        final String targetNamespace = switch (jpaVersion) {
            case "2.0", "2.1", "2.2" -> "javax";
            case "3.0", "3.1", "3.2" -> "jakarta";
            default -> throw new IllegalArgumentException("Unsupported JPA version: " + jpaVersion);
        };

        // Get the simple name of the provider class
        final String providerClassSimpleName = providerClass.getSimpleName();

        // Drive JVM ServiceLoader API to dynamically discover strategies loaded into the isolated classloader
        ServiceLoader<T> loader = ServiceLoader.load(providerClass);

        return loader.stream()
            .map(ServiceLoader.Provider::get)
            .filter(provider -> provider.getSupportedNamespace().equals(targetNamespace))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Failed to discover a registered " + providerClassSimpleName + " provider for namespace: " + targetNamespace));
    }

}
