package hibernate.persistence.tools.provider;

import hibernate.persistence.tools.HibernateNamedObjectParser;
import persistence.tools.NamedObjectParser;
import persistence.tools.spi.NamedObjectParserFactory;

import java.util.List;

/**
 * {@link NamedObjectParserFactory} implementation for the Jakarta Persistence
 * namespace ({@code "jakarta"}).
 *
 * <p>This factory is registered as a {@link java.util.ServiceLoader}
 * provider and creates {@link HibernateNamedObjectParser} instances backed by
 * Hibernate ORM&nbsp;6.</p>
 *
 * @since 2.0.0
 * @see HibernateNamedObjectParser
 */
public class HibernateNamedObjectParserFactory extends HibernatePersistenceLinterFactory implements NamedObjectParserFactory {

    /** Creates a new {@code JakartaNamedObjectParserFactory}. */
    public HibernateNamedObjectParserFactory() {}

    /** {@inheritDoc} */
    @Override
    public NamedObjectParser newNamedObjectParser(List<String> discoveredClassNames) {
        return HibernateNamedObjectParser.from(discoveredClassNames);
    }

}
