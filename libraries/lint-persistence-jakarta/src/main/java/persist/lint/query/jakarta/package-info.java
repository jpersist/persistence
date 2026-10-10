/**
 * Jakarta Persistence query parser implementation.
 *
 * <p>This package provides a {@link persist.lint.query.spi.QueryParser}
 * implementation that targets the {@code jakarta.persistence} namespace
 * (JPA&nbsp;3.x). It uses Hibernate ORM&nbsp;6 to compile JPQL query
 * strings into Semantic Query Model (SQM) trees, verifying grammar,
 * entity mappings, and property paths entirely offline.</p>
 *
 * <p>The {@link persist.lint.query.jakarta.JakartaQueryParserFactory} is
 * registered as a {@link java.util.ServiceLoader} provider for
 * {@link persist.lint.query.spi.QueryParserFactory}.</p>
 *
 * @see persist.lint.query.spi.QueryParser
 * @see persist.lint.query.spi.QueryParserFactory
 */
package persist.lint.query.jakarta;
