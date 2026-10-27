package persist.lint.jakarta;

import persist.jakarta.spi.JakartaPersistenceServiceProvider;
import persist.lint.api.spi.PersistenceLinterFactory;

/**
 * Abstract base class for Jakarta Persistence linter factories.
 *
 * <p>Provides the common {@link #getSupportedNamespace()} implementation
 * that returns {@code "jakarta"}, shared by all Jakarta-specific factory
 * types such as {@link JakartaNamedObjectParserFactory} and
 * {@link JakartaSchemaValidatorFactory}.</p>
 *
 * @since 2.1.0
 * @see PersistenceLinterFactory
 * @see JakartaNamedObjectParserFactory
 * @see JakartaSchemaValidatorFactory
 */
public abstract class JakartaPersistenceLinterFactory extends JakartaPersistenceServiceProvider implements PersistenceLinterFactory {

    /** Creates a new {@code JakartaPersistenceLinterFactory}. */
    protected JakartaPersistenceLinterFactory() {}

}
