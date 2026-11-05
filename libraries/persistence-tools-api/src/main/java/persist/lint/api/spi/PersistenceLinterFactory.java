package persist.lint.api.spi;

import persist.api.spi.PersistenceServiceProvider;

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
 * the {@link java.util.ServiceLoader} mechanism via generic discovery
 * methods accepting thread context classloader, custom {@link ClassLoader},
 * or {@link ModuleLayer} instances.</p>
 *
 * <p>In JPMS environments, provider modules declare implementations for
 * specific factory subtypes (e.g.&nbsp;{@code provides persist.lint.api.spi.NamedObjectParserFactory}
 * and {@code provides persist.lint.api.spi.SchemaValidatorFactory}) in
 * their {@code module-info.java}.</p>
 *
 * @since 2.1.0
 * @see PersistenceLinter
 * @see NamedObjectParserFactory
 * @see SchemaValidatorFactory
 */
public interface PersistenceLinterFactory extends PersistenceServiceProvider {

    /**
     * Returns the persistence namespace supported by this factory.
     *
     * @return the namespace identifier, for example {@code "javax"} for
     *         JPA&nbsp;2.x or {@code "jakarta"} for JPA&nbsp;3.x
     */
    String getSupportedNamespace();

    /**
     * Convenience method that discovers and returns the
     * {@link NamedObjectParserFactory} for the given JPA version using
     * the thread context class loader.
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
     * {@link NamedObjectParserFactory} for the given JPA version using
     * the specified {@link ClassLoader}.
     *
     * @param jpaVersion  the JPA specification version (e.g.&nbsp;{@code "3.1"})
     * @param classLoader the class loader used to discover the factory
     * @return the matching {@link NamedObjectParserFactory}
     * @throws IllegalArgumentException if the version is not recognized
     * @throws IllegalStateException    if no provider is registered for
     *                                  the resolved namespace
     * @since 3.1.0
     */
    static NamedObjectParserFactory newNamedObjectParserFactory(String jpaVersion, ClassLoader classLoader) {
        return newPersistenceLinterFactory(jpaVersion, NamedObjectParserFactory.class, classLoader);
    }

    /**
     * Convenience method that discovers and returns the
     * {@link NamedObjectParserFactory} for the given JPA version within
     * the specified {@link ModuleLayer}.
     *
     * @param jpaVersion the JPA specification version (e.g.&nbsp;{@code "3.1"})
     * @param layer      the module layer used to discover the factory
     * @return the matching {@link NamedObjectParserFactory}
     * @throws IllegalArgumentException if the version is not recognized
     * @throws IllegalStateException    if no provider is registered for
     *                                  the resolved namespace
     * @since 3.1.0
     */
    static NamedObjectParserFactory newNamedObjectParserFactory(String jpaVersion, ModuleLayer layer) {
        return newPersistenceLinterFactory(jpaVersion, NamedObjectParserFactory.class, layer);
    }

    /**
     * Convenience method that discovers and returns the
     * {@link SchemaValidatorFactory} for the given JPA version using
     * the thread context class loader.
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
     * Convenience method that discovers and returns the
     * {@link SchemaValidatorFactory} for the given JPA version using
     * the specified {@link ClassLoader}.
     *
     * @param jpaVersion  the JPA specification version (e.g.&nbsp;{@code "3.1"})
     * @param classLoader the class loader used to discover the factory
     * @return the matching {@link SchemaValidatorFactory}
     * @throws IllegalArgumentException if the version is not recognized
     * @throws IllegalStateException    if no provider is registered for
     *                                  the resolved namespace
     * @since 3.1.0
     */
    static SchemaValidatorFactory newSchemaValidatorFactory(String jpaVersion, ClassLoader classLoader) {
        return newPersistenceLinterFactory(jpaVersion, SchemaValidatorFactory.class, classLoader);
    }

    /**
     * Convenience method that discovers and returns the
     * {@link SchemaValidatorFactory} for the given JPA version within
     * the specified {@link ModuleLayer}.
     *
     * @param jpaVersion the JPA specification version (e.g.&nbsp;{@code "3.1"})
     * @param layer      the module layer used to discover the factory
     * @return the matching {@link SchemaValidatorFactory}
     * @throws IllegalArgumentException if the version is not recognized
     * @throws IllegalStateException    if no provider is registered for
     *                                  the resolved namespace
     * @since 3.1.0
     */
    static SchemaValidatorFactory newSchemaValidatorFactory(String jpaVersion, ModuleLayer layer) {
        return newPersistenceLinterFactory(jpaVersion, SchemaValidatorFactory.class, layer);
    }

    /**
     * Generic factory-discovery method that resolves a
     * {@link PersistenceLinterFactory} subtype for the given JPA version using
     * the thread context class loader.
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
        return PersistenceServiceProvider.getServiceProvider(jpaVersion, factoryClass);
    }

    /**
     * Generic factory-discovery method that resolves a
     * {@link PersistenceLinterFactory} subtype for the given JPA version using
     * the specified {@link ClassLoader}.
     *
     * @param <T>          the factory subtype to resolve
     * @param jpaVersion   the JPA specification version
     *                     (e.g.&nbsp;{@code "2.2"} or {@code "3.1"})
     * @param factoryClass the {@link Class} object of the desired factory type
     * @param classLoader  the class loader used to discover the factory
     * @return the factory implementation for the requested version
     * @throws IllegalArgumentException if the version is not recognized
     * @throws IllegalStateException    if no provider is registered for
     *                                  the resolved namespace
     * @since 3.1.0
     */
    static <T extends PersistenceLinterFactory> T newPersistenceLinterFactory(String jpaVersion, Class<T> factoryClass, ClassLoader classLoader) {
        return PersistenceServiceProvider.getServiceProvider(jpaVersion, factoryClass, classLoader);
    }

    /**
     * Generic factory-discovery method that resolves a
     * {@link PersistenceLinterFactory} subtype for the given JPA version within
     * the specified {@link ModuleLayer}.
     *
     * @param <T>          the factory subtype to resolve
     * @param jpaVersion   the JPA specification version
     *                     (e.g.&nbsp;{@code "2.2"} or {@code "3.1"})
     * @param factoryClass the {@link Class} object of the desired factory type
     * @param layer        the module layer used to discover the factory
     * @return the factory implementation for the requested version
     * @throws IllegalArgumentException if the version is not recognized
     * @throws IllegalStateException    if no provider is registered for
     *                                  the resolved namespace
     * @since 3.1.0
     */
    static <T extends PersistenceLinterFactory> T newPersistenceLinterFactory(String jpaVersion, Class<T> factoryClass, ModuleLayer layer) {
        return PersistenceServiceProvider.getServiceProvider(jpaVersion, factoryClass, layer);
    }

}
