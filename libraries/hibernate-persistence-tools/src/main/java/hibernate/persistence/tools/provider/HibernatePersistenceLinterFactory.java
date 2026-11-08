package hibernate.persistence.tools.provider;

import persistence.tools.spi.PersistenceLinterFactory;

/**
 * Abstract base class for legacy (javax) Persistence linter factories.
 *
 * <p>Provides the common {@link #getSupportedNamespace()} implementation
 * that returns {@code "javax"}, shared by all legacy-specific factory
 * types such as {@link HibernateNamedObjectParserFactory} and
 * {@link HibernateSchemaValidatorFactory}.</p>
 *
 * @since 2.1.0
 * @see PersistenceLinterFactory
 * @see HibernateNamedObjectParserFactory
 * @see HibernateSchemaValidatorFactory
 */
public abstract class HibernatePersistenceLinterFactory implements PersistenceLinterFactory {

    /** Creates a new {@code LegacyPersistenceLinterFactory}. */
    protected HibernatePersistenceLinterFactory() {}

    /** {@inheritDoc} */
    @Override
    public String getSupportedNamespace() {
        return "javax";
    }

}
