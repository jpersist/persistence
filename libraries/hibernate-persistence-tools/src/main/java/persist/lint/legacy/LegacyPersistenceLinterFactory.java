package persist.lint.legacy;

import persist.lint.api.spi.PersistenceLinterFactory;

/**
 * Abstract base class for legacy (javax) Persistence linter factories.
 *
 * <p>Provides the common {@link #getSupportedNamespace()} implementation
 * that returns {@code "javax"}, shared by all legacy-specific factory
 * types such as {@link LegacyNamedObjectParserFactory} and
 * {@link LegacySchemaValidatorFactory}.</p>
 *
 * @since 2.1.0
 * @see PersistenceLinterFactory
 * @see LegacyNamedObjectParserFactory
 * @see LegacySchemaValidatorFactory
 */
public abstract class LegacyPersistenceLinterFactory implements PersistenceLinterFactory {

    /** Creates a new {@code LegacyPersistenceLinterFactory}. */
    protected LegacyPersistenceLinterFactory() {}

    /** {@inheritDoc} */
    @Override
    public String getSupportedNamespace() {
        return "javax";
    }

}
