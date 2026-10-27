package persist.hibernate.enhance.legacy;

import persist.hibernate.enhance.spi.HibernateEnhancer;
import persist.hibernate.enhance.spi.HibernateEnhancerProvider;
import persist.legacy.spi.LegacyPersistenceServiceProvider;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * {@link HibernateEnhancerProvider} implementation for the legacy
 * {@code javax.persistence} namespace ({@code "javax"}).
 *
 * <p>This provider is registered as a {@link java.util.ServiceLoader}
 * provider and delegates to {@link LegacyHibernateEnhancer} for
 * bytecode enhancement using Hibernate&nbsp;5.</p>
 *
 * @since 1.0.0
 * @see LegacyHibernateEnhancer
 * @see HibernateEnhancerProvider
 */
public class LegacyHibernateEnhancerProvider extends LegacyPersistenceServiceProvider implements HibernateEnhancerProvider {

    /** Creates a new {@code LegacyHibernateEnhancerProvider}. */
    public LegacyHibernateEnhancerProvider() {
    }

    /** {@inheritDoc} */
    @Override
    public HibernateEnhancer getEnhancer(File classesDir, Set<File> classpathFiles,
                                         Map<HibernateEnhancer.Option, Boolean> options) {
        return LegacyHibernateEnhancer.from(classesDir, classpathFiles, options);
    }

}
