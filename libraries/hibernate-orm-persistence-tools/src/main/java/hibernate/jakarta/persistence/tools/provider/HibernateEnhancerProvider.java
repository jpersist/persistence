package hibernate.jakarta.persistence.tools.provider;

import hibernate.jakarta.persistence.tools.HibernateEnhancer;
import persistence.tools.Enhancer;
import persistence.tools.spi.EnhancerProvider;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * {@link EnhancerProvider} implementation for the Jakarta
 * Persistence namespace ({@code "jakarta"}).
 *
 * <p>This provider is registered as a {@link java.util.ServiceLoader}
 * provider and delegates to {@link HibernateEnhancer} for
 * bytecode enhancement using Hibernate ORM&nbsp;6.</p>
 *
 * @since 1.0.0
 * @see HibernateEnhancer
 * @see EnhancerProvider
 */
public class HibernateEnhancerProvider extends HibernatePersistenceServiceProvider implements EnhancerProvider {

    /** Creates a new {@code HibernateEnhancerProvider}. */
    public HibernateEnhancerProvider() {
    }

    /** {@inheritDoc} */
    @Override
    public Enhancer getEnhancer(Collection<Set<File>> classpathFilesCollection,
                                Map<String, Object> settings) {
        return HibernateEnhancer.from(classpathFilesCollection, settings);
    }

}
