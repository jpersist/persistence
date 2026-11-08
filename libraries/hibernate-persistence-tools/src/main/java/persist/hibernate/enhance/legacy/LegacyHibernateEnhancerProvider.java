package persist.hibernate.enhance.legacy;

import persistence.tools.Enhancer;
import persistence.tools.spi.EnhancerProvider;
import persist.legacy.spi.LegacyPersistenceServiceProvider;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * {@link EnhancerProvider} implementation for the legacy
 * {@code javax.persistence} namespace ({@code "javax"}).
 *
 * <p>This provider is registered as a {@link java.util.ServiceLoader}
 * provider and delegates to {@link LegacyHibernateEnhancer} for
 * bytecode enhancement using Hibernate&nbsp;5.</p>
 *
 * @since 1.0.0
 * @see LegacyHibernateEnhancer
 * @see EnhancerProvider
 */
public class LegacyHibernateEnhancerProvider extends LegacyPersistenceServiceProvider implements EnhancerProvider {

    /** Creates a new {@code LegacyHibernateEnhancerProvider}. */
    public LegacyHibernateEnhancerProvider() {
    }

    /** {@inheritDoc} */
    @Override
    public Enhancer getEnhancer(File classesDir, Set<File> classpathFiles,
                                Map<Enhancer.Option, Boolean> options) {
        return LegacyHibernateEnhancer.from(classesDir, classpathFiles, options);
    }

}
