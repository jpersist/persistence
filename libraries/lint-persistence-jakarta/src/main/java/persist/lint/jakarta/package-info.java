/**
 * Jakarta Persistence named object parser implementation.
 *
 * <p>This package provides a {@link persist.lint.api.spi.NamedObjectParser}
 * implementation that targets the {@code jakarta.persistence} namespace
 * (JPA&nbsp;3.x). It uses Hibernate ORM&nbsp;6 to compile JPQL query
 * strings into Semantic Query Model (SQM) trees, verifying grammar,
 * entity mappings, and property paths entirely offline.</p>
 *
 * <p>The {@link persist.lint.jakarta.JakartaNamedObjectParserFactory} is
 * registered as a {@link java.util.ServiceLoader} provider for
 * {@link persist.lint.api.spi.NamedObjectParserFactory}.</p>
 *
 * @since 2.0.0
 * @see persist.lint.api.spi.NamedObjectParser
 * @see persist.lint.api.spi.NamedObjectParserFactory
 */
package persist.lint.jakarta;
