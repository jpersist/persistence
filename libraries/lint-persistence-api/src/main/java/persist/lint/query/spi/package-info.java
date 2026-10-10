/**
 * Service Provider Interface (SPI) for build-time JPQL query validation.
 *
 * <p>This package defines the core abstractions used to parse and validate
 * named JPQL queries at build time, without requiring a live database
 * connection:</p>
 *
 * <ul>
 *   <li>{@link persist.lint.query.spi.QueryParser} — parses and
 *       semantically validates a single JPQL query string against the
 *       project's entity metamodel.</li>
 *   <li>{@link persist.lint.query.spi.QueryParserFactory} — factory
 *       for creating {@code QueryParser} instances; implementations are
 *       discovered at runtime via the {@link java.util.ServiceLoader}
 *       mechanism.</li>
 * </ul>
 *
 * <p>Provider implementations for specific JPA namespaces are shipped in
 * separate modules (e.g.&nbsp;{@code lint-persistence-jakarta} and
 * {@code lint-persistence-legacy}).</p>
 */
package persist.lint.query.spi;
