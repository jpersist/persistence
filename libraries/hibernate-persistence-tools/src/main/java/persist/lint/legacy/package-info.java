/**
 * Legacy (javax) Persistence linter implementation.
 *
 * <p>This package provides {@link NamedObjectParser}
 * and {@link SchemaValidator} implementations that
 * target the {@code javax.persistence} namespace (JPA&nbsp;2.x). It
 * uses Hibernate&nbsp;5 and its AST-based {@code QueryTranslatorFactory}
 * to compile JPQL query strings and to validate entity schemas against a
 * live database, verifying HQL grammar, entity mappings, and property
 * paths entirely offline (except for schema validation).</p>
 *
 * <ul>
 *   <li>{@link persist.lint.legacy.LegacyPersistenceLinterFactory} &ndash;
 *       abstract base providing the {@code "javax"} namespace.</li>
 *   <li>{@link persist.lint.legacy.LegacyNamedObjectParserFactory} &ndash;
 *       registered as a {@link java.util.ServiceLoader} provider for
 *       {@link NamedObjectParserFactory}.</li>
 *   <li>{@link persist.lint.legacy.LegacySchemaValidatorFactory} &ndash;
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
package persist.lint.legacy;

import persistence.tools.NamedObjectParser;
import persistence.tools.SchemaValidator;
import persistence.tools.spi.NamedObjectParserFactory;
import persistence.tools.spi.SchemaValidatorFactory;
