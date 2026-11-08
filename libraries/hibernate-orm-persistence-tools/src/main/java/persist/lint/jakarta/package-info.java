/**
 * Jakarta Persistence linter implementation.
 *
 * <p>This package provides {@link persist.lint.api.spi.NamedObjectParser}
 * and {@link persist.lint.api.spi.SchemaValidator} implementations that
 * target the {@code jakarta.persistence} namespace (JPA&nbsp;3.x). It
 * uses Hibernate ORM&nbsp;6 to compile JPQL query strings into Semantic
 * Query Model (SQM) trees and to validate entity schemas against a live
 * database, verifying grammar, entity mappings, and property paths
 * entirely offline (except for schema validation).</p>
 *
 * <ul>
 *   <li>{@link persist.lint.jakarta.JakartaPersistenceLinterFactory} &ndash;
 *       abstract base providing the {@code "jakarta"} namespace.</li>
 *   <li>{@link persist.lint.jakarta.JakartaNamedObjectParserFactory} &ndash;
 *       registered as a {@link java.util.ServiceLoader} provider for
 *       {@link persist.lint.api.spi.NamedObjectParserFactory}.</li>
 *   <li>{@link persist.lint.jakarta.JakartaSchemaValidatorFactory} &ndash;
 *       registered as a {@link java.util.ServiceLoader} provider for
 *       {@link persist.lint.api.spi.SchemaValidatorFactory}.</li>
 * </ul>
 *
 * @since 2.0.0
 * @see persist.lint.api.spi.NamedObjectParser
 * @see persist.lint.api.spi.NamedObjectParserFactory
 * @see persist.lint.api.spi.SchemaValidator
 * @see persist.lint.api.spi.SchemaValidatorFactory
 */
package persist.lint.jakarta;
