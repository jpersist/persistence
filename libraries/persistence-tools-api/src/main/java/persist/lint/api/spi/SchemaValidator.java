package persist.lint.api.spi;

import java.util.Map;

/**
 * Service provider interface for validating a JPA persistence schema
 * against a live database at build time.
 *
 * <p>Implementations use the entity metamodel built from the discovered
 * annotated classes and compare it with the actual database schema
 * reachable through the supplied connection properties.</p>
 *
 * @since 2.1.0
 * @see SchemaValidatorFactory
 * @see PersistenceLinter
 */
public interface SchemaValidator extends PersistenceLinter {

    /**
     * Validates the persistence schema against the database described by
     * the given connection properties.
     *
     * <p>The map is expected to contain at least the keys
     * {@code "driver"}, {@code "url"}, {@code "username"}, and
     * {@code "password"}.</p>
     *
     * @param connectionProperties JDBC connection properties used to
     *                             reach the target database
     * @throws RuntimeException if the schema does not match the entity
     *                          metamodel
     */
    void validateSchema(Map<String, String> connectionProperties);

}
