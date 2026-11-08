package persist.lint.legacy;

import persistence.tools.SchemaValidator;
import persistence.tools.spi.SchemaValidatorFactory;

import java.util.List;

/**
 * {@link SchemaValidatorFactory} implementation for the legacy
 * {@code javax.persistence} namespace ({@code "javax"}).
 *
 * <p>This factory is registered as a {@link java.util.ServiceLoader}
 * provider and creates {@link LegacySchemaValidator} instances backed by
 * Hibernate&nbsp;5.</p>
 *
 * @since 2.1.0
 * @see LegacySchemaValidator
 * @see LegacyPersistenceLinterFactory
 */
public class LegacySchemaValidatorFactory extends LegacyPersistenceLinterFactory implements SchemaValidatorFactory {

    /** Creates a new {@code LegacySchemaValidatorFactory}. */
    public LegacySchemaValidatorFactory() {}

    /** {@inheritDoc} */
    @Override
    public SchemaValidator newSchemaValidator(List<String> discoveredClassNames) {
        return LegacySchemaValidator.of(discoveredClassNames);
    }

}
