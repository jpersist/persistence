/**
 * Service Provider Interface (SPI) for build-time validation of named
 * JPA objects.
 *
 * <p>This package defines the core abstractions used to parse and validate
 * named JPQL queries and stored procedures at build time, without requiring
 * a live database connection:</p>
 *
 * <ul>
 *   <li>{@link persist.lint.api.spi.NamedObjectParser} — parses and
 *       semantically validates a named JPQL query or stored procedure
 *       declaration against the project's entity metamodel.</li>
 *   <li>{@link persist.lint.api.spi.NamedObjectParserFactory} — factory
 *       for creating {@code NamedObjectParser} instances; implementations
 *       are discovered at runtime via the {@link java.util.ServiceLoader}
 *       mechanism.</li>
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
