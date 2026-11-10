package hibernate.jakarta.persistence.tools.provider;

import hibernate.jakarta.persistence.tools.HibernateSchemaValidator;
import persistence.tools.SchemaValidator;
import persistence.tools.spi.SchemaValidatorFactory;

import java.util.List;

/**
 * {@link SchemaValidatorFactory} implementation for the Jakarta Persistence
 * namespace ({@code "jakarta"}).
 *
 * <p>This factory is registered as a {@link java.util.ServiceLoader}
 * provider and creates {@link HibernateSchemaValidator} instances backed by
 * Hibernate ORM&nbsp;6.</p>
 *
 * @since 1.0.0
 * @see HibernateSchemaValidator
 * @see HibernatePersistenceLinterFactory
 */
public class HibernateSchemaValidatorFactory extends HibernatePersistenceLinterFactory implements SchemaValidatorFactory {

    /** Creates a new {@code HibernateSchemaValidatorFactory}. */
    public HibernateSchemaValidatorFactory() {}

    /** {@inheritDoc} */
    @Override
    public SchemaValidator newSchemaValidator(List<String> discoveredClassNames) {
        return HibernateSchemaValidator.of(discoveredClassNames);
    }

}
