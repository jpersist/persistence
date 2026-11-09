package hibernate.javax.persistence.tools.provider;

import hibernate.javax.persistence.tools.HibernateEnhancer;
import persistence.tools.Enhancer;
import persistence.tools.spi.EnhancerProvider;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * {@link EnhancerProvider} implementation for the legacy
 * {@code javax.persistence} namespace ({@code "javax"}).
 *
 * <p>This provider is registered as a {@link java.util.ServiceLoader}
 * provider and delegates to {@link HibernateEnhancer} for
 * bytecode enhancement using Hibernate&nbsp;5.</p>
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
