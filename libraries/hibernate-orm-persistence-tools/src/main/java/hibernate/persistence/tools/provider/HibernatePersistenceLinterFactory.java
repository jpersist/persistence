package hibernate.persistence.tools.provider;

import persistence.tools.spi.PersistenceLinterFactory;

/**
 * Abstract base class for Jakarta Persistence linter factories.
 *
 * <p>Provides the common {@link #getSupportedNamespace()} implementation
 * that returns {@code "jakarta"}, shared by all Jakarta-specific factory
 * types such as {@link HibernateNamedObjectParserFactory} and
 * {@link HibernateSchemaValidatorFactory}.</p>
 *
 * @since 2.1.0
 * @see PersistenceLinterFactory
 * @see HibernateNamedObjectParserFactory
 * @see HibernateSchemaValidatorFactory
 */
public abstract class HibernatePersistenceLinterFactory extends HibernatePersistenceServiceProvider implements PersistenceLinterFactory {

    /** Creates a new {@code JakartaPersistenceLinterFactory}. */
    protected HibernatePersistenceLinterFactory() {}

}
