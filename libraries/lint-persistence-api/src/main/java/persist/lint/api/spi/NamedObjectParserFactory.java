package persist.lint.api.spi;

import java.util.List;
import java.util.ServiceLoader;

/**
 * Factory for creating {@link NamedObjectParser} instances.
 *
 * <p>Implementations are discovered at runtime through the
 * {@link ServiceLoader} mechanism. Each implementation declares the
 * persistence namespace it supports (e.g.&nbsp;{@code "javax"} or
 * {@code "jakarta"}) so that the correct parser strategy can be
 * resolved for the target JPA specification version.</p>
 *
 * @since 2.0.0
 * @see NamedObjectParser
 */
public interface NamedObjectParserFactory extends PersistenceLinterFactory {

    /**
     * Creates a new {@link NamedObjectParser} configured with the given entity
     * class names.
     *
     * @param discoveredClassNames fully qualified class names of the JPA
     *                             entities to register in the metamodel
     * @return a ready-to-use named object parser
     */
    NamedObjectParser newNamedObjectParser(List<String> discoveredClassNames);

    /**
     * Discovers and returns the {@link NamedObjectParserFactory} implementation
     * that matches the given JPA specification version.
     *
     * <p>The version string is mapped to a persistence namespace
     * ({@code "javax"} for versions 2.0–2.2, {@code "jakarta"} for
     * versions 3.0–3.2) and the matching factory is located via the
     * {@link ServiceLoader} API.</p>
     *
     * @param jpaVersion the JPA specification version (e.g.&nbsp;{@code "2.2"}
     *                   or {@code "3.1"})
     * @return the factory implementation for the requested version
     * @throws IllegalArgumentException if the version is not recognized
     * @throws IllegalStateException    if no provider is registered for
     *                                  the resolved namespace
     */
    static NamedObjectParserFactory newNamedObjectParserFactory(String jpaVersion) {
        return PersistenceLinterFactory.newPersistenceLinterFactory(jpaVersion, NamedObjectParserFactory.class);
    }

}
