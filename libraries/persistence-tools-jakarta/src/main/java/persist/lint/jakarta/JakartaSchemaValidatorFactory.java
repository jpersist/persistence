package persist.lint.jakarta;

import persist.lint.api.spi.SchemaValidator;
import persist.lint.api.spi.SchemaValidatorFactory;

import java.util.List;

/**
 * {@link SchemaValidatorFactory} implementation for the Jakarta Persistence
 * namespace ({@code "jakarta"}).
 *
 * <p>This factory is registered as a {@link java.util.ServiceLoader}
 * provider and creates {@link JakartaSchemaValidator} instances backed by
 * Hibernate ORM&nbsp;6.</p>
 *
 * @since 2.1.0
 * @see JakartaSchemaValidator
 * @see JakartaPersistenceLinterFactory
 */
public class JakartaSchemaValidatorFactory extends JakartaPersistenceLinterFactory implements SchemaValidatorFactory {

    /** Creates a new {@code JakartaSchemaValidatorFactory}. */
    public JakartaSchemaValidatorFactory() {}

    /** {@inheritDoc} */
    @Override
    public SchemaValidator newSchemaValidator(List<String> discoveredClassNames) {
        return JakartaSchemaValidator.of(discoveredClassNames);
    }

}
