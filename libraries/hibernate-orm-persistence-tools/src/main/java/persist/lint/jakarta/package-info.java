/**
 * Jakarta Persistence linter implementation.
 *
 * <p>This package provides {@link NamedObjectParser}
 * and {@link SchemaValidator} implementations that
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
 *       {@link NamedObjectParserFactory}.</li>
 *   <li>{@link persist.lint.jakarta.JakartaSchemaValidatorFactory} &ndash;
 *       registered as a {@link java.util.ServiceLoader} provider for
 *       {@link SchemaValidatorFactory}.</li>
 * </ul>
 *
 * @since 2.0.0
 * @see NamedObjectParser
 * @see NamedObjectParserFactory
 * @see SchemaValidator
 * @see SchemaValidatorFactory
 */
package persist.lint.jakarta;

import persistence.tools.NamedObjectParser;
import persistence.tools.SchemaValidator;
import persistence.tools.spi.NamedObjectParserFactory;
import persistence.tools.spi.SchemaValidatorFactory;
