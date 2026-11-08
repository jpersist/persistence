package hibernate.persistence.tools.provider;

import hibernate.persistence.tools.HibernateEnhancer;
import persistence.tools.Enhancer;
import persistence.tools.spi.EnhancerProvider;

import java.io.File;
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

    /** Creates a new {@code LegacyHibernateEnhancerProvider}. */
    public HibernateEnhancerProvider() {
    }

    /** {@inheritDoc} */
    @Override
    public Enhancer getEnhancer(File classesDir, Set<File> classpathFiles,
                                Map<Enhancer.Option, Boolean> options) {
        return HibernateEnhancer.from(classesDir, classpathFiles, options);
    }

}
