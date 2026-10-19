package persist.lint.api.spi;

import java.util.ServiceLoader;

/**
 * Base factory interface for all persistence linter factories.
 *
 * <p>This interface defines the common contract shared by all linter
 * factory types ({@link NamedObjectParserFactory},
 * {@link SchemaValidatorFactory}). Each implementation declares the
 * persistence namespace it supports (e.g.&nbsp;{@code "javax"} or
 * {@code "jakarta"}) so that the correct strategy can be resolved for
 * the target JPA specification version.</p>
 *
 * <p>Concrete factory implementations are discovered at runtime through
 * the {@link java.util.ServiceLoader} mechanism via the generic
 * {@link #newPersistenceLinterFactory(String, Class)} method.</p>
 *
 * @since 2.1.0
 * @see PersistenceLinter
 * @see NamedObjectParserFactory
 * @see SchemaValidatorFactory
 */
public interface PersistenceLinterFactory {

    /**
     * Returns the persistence namespace supported by this factory.
     *
     * @return the namespace identifier, for example {@code "javax"} for
     *         JPA&nbsp;2.x or {@code "jakarta"} for JPA&nbsp;3.x
     */
    String getSupportedNamespace();

    /**
     * Convenience method that discovers and returns the
     * {@link NamedObjectParserFactory} for the given JPA version.
     *
     * @param jpaVersion the JPA specification version (e.g.&nbsp;{@code "3.1"})
     * @return the matching {@link NamedObjectParserFactory}
     * @throws IllegalArgumentException if the version is not recognized
     * @throws IllegalStateException    if no provider is registered for
     *                                  the resolved namespace
     * @since 2.1.0
     */
    static NamedObjectParserFactory newNamedObjectParserFactory(String jpaVersion) {
        return newPersistenceLinterFactory(jpaVersion, NamedObjectParserFactory.class);
    }

    /**
     * Convenience method that discovers and returns the
     * {@link SchemaValidatorFactory} for the given JPA version.
     *
     * @param jpaVersion the JPA specification version (e.g.&nbsp;{@code "3.1"})
     * @return the matching {@link SchemaValidatorFactory}
     * @throws IllegalArgumentException if the version is not recognized
     * @throws IllegalStateException    if no provider is registered for
     *                                  the resolved namespace
     * @since 2.1.0
     */
    static SchemaValidatorFactory newSchemaValidatorFactory(String jpaVersion) {
        return newPersistenceLinterFactory(jpaVersion, SchemaValidatorFactory.class);
    }

    /**
     * Generic factory-discovery method that resolves a
     * {@link PersistenceLinterFactory} subtype for the given JPA version.
     *
     * <p>The version string is mapped to a persistence namespace
     * ({@code "javax"} for versions 2.0–2.2, {@code "jakarta"} for
     * versions 3.0–3.2) and the matching factory is located via the
     * {@link java.util.ServiceLoader} API.</p>
     *
     * @param <T>          the factory subtype to resolve
     * @param jpaVersion   the JPA specification version
     *                     (e.g.&nbsp;{@code "2.2"} or {@code "3.1"})
     * @param factoryClass the {@link Class} object of the desired factory type
     * @return the factory implementation for the requested version
     * @throws IllegalArgumentException if the version is not recognized
     * @throws IllegalStateException    if no provider is registered for
     *                                  the resolved namespace
     * @since 2.1.0
     */
    static <T extends PersistenceLinterFactory> T newPersistenceLinterFactory(String jpaVersion, Class<T> factoryClass) {
        // Resolve target namespace based on the semantic spec version map token
        final String targetNamespace = switch (jpaVersion) {
            case "2.0", "2.1", "2.2" -> "javax";
            case "3.0", "3.1", "3.2" -> "jakarta";
            default -> throw new IllegalArgumentException("Unsupported JPA version: " + jpaVersion);
        };

        // Get the simple name of the factory class
        final String factoryClassSimpleName = factoryClass.getSimpleName();

        // Drive JVM ServiceLoader API to dynamically discover strategies loaded into the isolated classloader
        ServiceLoader<T> loader = ServiceLoader.load(factoryClass);

        return loader.stream()
            .map(ServiceLoader.Provider::get)
            .filter(factory -> factory.getSupportedNamespace().equals(targetNamespace))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Failed to discover a registered " + factoryClassSimpleName + " provider for namespace: " + targetNamespace));
    }

}
