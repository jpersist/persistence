package persist.api.spi;

import persist.lint.api.spi.PersistenceLinterFactory;

import java.util.ServiceLoader;

/**
 * Root service-provider interface for all persistence tool providers.
 *
 * <p>Each provider declares the JPA namespace it supports (e.g.&nbsp;{@code "jakarta"}
 * or {@code "javax"}) via {@link #getSupportedNamespace()}. Concrete provider
 * factory interfaces such as {@link persist.hibernate.enhance.spi.HibernateEnhancerProvider},
 * {@link persist.lint.api.spi.NamedObjectParserFactory}, and
 * {@link persist.lint.api.spi.SchemaValidatorFactory} extend this interface (or
 * its subtype {@link persist.lint.api.spi.PersistenceLinterFactory}) and are
 * discovered at runtime through the {@link java.util.ServiceLoader} mechanism.</p>
 *
 * <p>In JPMS modular environments, provider modules register implementations
 * for specific factory interfaces via {@code provides ... with ...} in their
 * {@code module-info.java}, while consumer/API modules declare corresponding
 * {@code uses} directives. Service discovery queries the concrete factory
 * interface type rather than the root base interface.</p>
 *
 * <p>The static helper {@link #getServiceProvider(String, Class)} and its overloads
 * map a JPA specification version string to the corresponding persistence namespace
 * and locate the matching provider using the thread context class loader, a specified
 * {@link ClassLoader}, or a {@link ModuleLayer}.</p>
 *
 * @since 1.0.0
 * @see persist.lint.api.spi.PersistenceLinterFactory
 * @see persist.lint.api.spi.NamedObjectParserFactory
 * @see persist.lint.api.spi.SchemaValidatorFactory
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
     * matches the given JPA specification version using the thread context
     * class loader.
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
        return getServiceProvider(jpaVersion, providerClass, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Discovers and returns the {@code PersistenceServiceProvider} of the
     * requested type whose {@linkplain #getSupportedNamespace() namespace}
     * matches the given JPA specification version using the specified
     * {@link ClassLoader}.
     *
     * @param jpaVersion    the JPA specification version (e.g.&nbsp;{@code "3.1"})
     * @param providerClass the provider subtype to look up
     * @param classLoader   the class loader to use for service loading
     * @param <T>           the provider type
     * @return the matching provider instance
     * @throws IllegalArgumentException if {@code jpaVersion} is not recognised
     * @throws IllegalStateException    if no registered provider matches the
     *                                  resolved namespace
     */
    static <T extends PersistenceServiceProvider> T getServiceProvider(String jpaVersion, Class<T> providerClass, ClassLoader classLoader) {
        return findProvider(jpaVersion, providerClass, ServiceLoader.load(providerClass, classLoader));
    }

    /**
     * Discovers and returns the {@code PersistenceServiceProvider} of the
     * requested type whose {@linkplain #getSupportedNamespace() namespace}
     * matches the given JPA specification version using the specified
     * {@link ModuleLayer}.
     *
     * @param jpaVersion    the JPA specification version (e.g.&nbsp;{@code "3.1"})
     * @param providerClass the provider subtype to look up
     * @param layer         the module layer to use for service loading
     * @param <T>           the provider type
     * @return the matching provider instance
     * @throws IllegalArgumentException if {@code jpaVersion} is not recognised
     * @throws IllegalStateException    if no registered provider matches the
     *                                  resolved namespace
     */
    static <T extends PersistenceServiceProvider> T getServiceProvider(String jpaVersion, Class<T> providerClass, ModuleLayer layer) {
        return findProvider(jpaVersion, providerClass, ServiceLoader.load(layer, providerClass));
    }

    private static <T extends PersistenceServiceProvider> T findProvider(String jpaVersion, Class<T> providerClass, ServiceLoader<T> loader) {
        // Resolve target namespace based on the semantic spec version map token
        final String targetNamespace = switch (jpaVersion) {
            case "2.0", "2.1", "2.2" -> "javax";
            case "3.0", "3.1", "3.2" -> "jakarta";
            default -> throw new IllegalArgumentException("Unsupported JPA version: " + jpaVersion);
        };

        // Get the simple name of the provider class
        final String providerClassSimpleName = providerClass.getSimpleName();

        return loader.stream()
            .map(ServiceLoader.Provider::get)
            .filter(provider -> provider.getSupportedNamespace().equals(targetNamespace))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Failed to discover a registered " + providerClassSimpleName + " provider for namespace: " + targetNamespace));
    }

}
