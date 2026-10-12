/**
 * Legacy (javax) Persistence named object parser implementation.
 *
 * <p>This package provides a {@link persist.lint.api.spi.NamedObjectParser}
 * implementation that targets the {@code javax.persistence} namespace
 * (JPA&nbsp;2.x). It uses Hibernate&nbsp;5 and its AST-based
 * {@code QueryTranslatorFactory} to compile JPQL query strings,
 * verifying HQL grammar, entity mappings, and property paths entirely
 * offline.</p>
 *
 * <p>The {@link persist.lint.legacy.LegacyNamedObjectParserFactory} is
 * registered as a {@link java.util.ServiceLoader} provider for
 * {@link persist.lint.api.spi.NamedObjectParserFactory}.</p>
 *
 * @since 2.0.0
 * @see persist.lint.api.spi.NamedObjectParser
 * @see persist.lint.api.spi.NamedObjectParserFactory
 */
package persist.lint.legacy;
