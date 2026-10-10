/**
 * Legacy (javax) Persistence query parser implementation.
 *
 * <p>This package provides a {@link persist.lint.query.spi.QueryParser}
 * implementation that targets the {@code javax.persistence} namespace
 * (JPA&nbsp;2.x). It uses Hibernate&nbsp;5 and its AST-based
 * {@code QueryTranslatorFactory} to compile JPQL query strings,
 * verifying HQL grammar, entity mappings, and property paths entirely
 * offline.</p>
 *
 * <p>The {@link persist.lint.query.legacy.LegacyQueryParserFactory} is
 * registered as a {@link java.util.ServiceLoader} provider for
 * {@link persist.lint.query.spi.QueryParserFactory}.</p>
 *
 * @see persist.lint.query.spi.QueryParser
 * @see persist.lint.query.spi.QueryParserFactory
 */
package persist.lint.query.legacy;
