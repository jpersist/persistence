/**
 * Service Provider Interface (SPI) for build-time persistence validation.
 *
 * <p>This package defines the core abstractions used to parse and validate
 * named JPQL queries, stored procedures, and database schemas at build
 * time, without requiring a live database connection (except for schema
 * validation):</p>
 *
 * <ul>
 *   <li>{@link persist.lint.api.spi.PersistenceLinter} &ndash; marker
 *       interface for all persistence linting operations.</li>
 *   <li>{@link persist.lint.api.spi.PersistenceLinterFactory} &ndash; base
 *       factory interface providing namespace resolution and generic
 *       {@link java.util.ServiceLoader}-based discovery of linter
 *       factories.</li>
 *   <li>{@link persist.lint.api.spi.NamedObjectParser} &ndash; parses and
 *       semantically validates a named JPQL query or stored procedure
 *       declaration against the project's entity metamodel.</li>
 *   <li>{@link persist.lint.api.spi.NamedObjectParserFactory} &ndash;
 *       factory for creating {@code NamedObjectParser} instances.</li>
 *   <li>{@link persist.lint.api.spi.SchemaValidator} &ndash; validates the
 *       entity metamodel against a live database schema.</li>
 *   <li>{@link persist.lint.api.spi.SchemaValidatorFactory} &ndash;
 *       factory for creating {@code SchemaValidator} instances.</li>
 * </ul>
 *
 * <p>Provider implementations for specific JPA namespaces are shipped in
 * separate modules (e.g.&nbsp;{@code lint-persistence-jakarta} and
 * {@code lint-persistence-legacy}).</p>
 *
 * @since 2.0.0
 * @see persist.lint.api
 */
package persist.lint.api.spi;
