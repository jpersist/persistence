package persist.lint.api.spi;

import java.util.List;

/**
 * Factory for creating {@link SchemaValidator} instances.
 *
 * <p>Implementations are discovered at runtime through the
 * {@link java.util.ServiceLoader} mechanism. Each implementation declares
 * the persistence namespace it supports (e.g.&nbsp;{@code "javax"} or
 * {@code "jakarta"}) so that the correct validation strategy can be
 * resolved for the target JPA specification version.</p>
 *
 * @since 2.1.0
 * @see SchemaValidator
 * @see PersistenceLinterFactory
 */
public interface SchemaValidatorFactory extends PersistenceLinterFactory {

    /**
     * Creates a new {@link SchemaValidator} configured with the given
     * entity class names.
     *
     * @param discoveredClassNames fully qualified class names of the JPA
     *                             entities to register in the metamodel
     * @return a ready-to-use schema validator
     */
    SchemaValidator newSchemaValidator(List<String> discoveredClassNames);

    /**
     * Discovers and returns the {@link SchemaValidatorFactory}
     * implementation that matches the given JPA specification version.
     *
     * <p>The version string is mapped to a persistence namespace
     * ({@code "javax"} for versions 2.0–2.2, {@code "jakarta"} for
     * versions 3.0–3.2) and the matching factory is located via the
     * {@link java.util.ServiceLoader} API.</p>
     *
     * @param jpaVersion the JPA specification version (e.g.&nbsp;{@code "2.2"}
     *                   or {@code "3.1"})
     * @return the factory implementation for the requested version
     * @throws IllegalArgumentException if the version is not recognized
     * @throws IllegalStateException    if no provider is registered for
     *                                  the resolved namespace
     */
    static SchemaValidatorFactory newSchemaValidatorFactory(String jpaVersion) {
        return PersistenceLinterFactory.newPersistenceLinterFactory(jpaVersion, SchemaValidatorFactory.class);
    }

}
