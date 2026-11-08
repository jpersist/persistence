package persist.hibernate.enhance.jakarta;

import persist.hibernate.enhance.spi.HibernateEnhancer;
import persist.hibernate.enhance.spi.HibernateEnhancerProvider;
import persist.jakarta.spi.JakartaPersistenceServiceProvider;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * {@link HibernateEnhancerProvider} implementation for the Jakarta
 * Persistence namespace ({@code "jakarta"}).
 *
 * <p>This provider is registered as a {@link java.util.ServiceLoader}
 * provider and delegates to {@link JakartaHibernateEnhancer} for
 * bytecode enhancement using Hibernate ORM&nbsp;6.</p>
 *
 * @since 1.0.0
 * @see JakartaHibernateEnhancer
 * @see HibernateEnhancerProvider
 */
public class JakartaHibernateEnhancerProvider extends JakartaPersistenceServiceProvider implements HibernateEnhancerProvider {

    /** Creates a new {@code JakartaHibernateEnhancerProvider}. */
    public JakartaHibernateEnhancerProvider() {
    }

    /** {@inheritDoc} */
    @Override
    public HibernateEnhancer getEnhancer(File classesDir, Set<File> classpathFiles,
                                         Map<HibernateEnhancer.Option, Boolean> options) {
        return JakartaHibernateEnhancer.from(classesDir, classpathFiles, options);
    }

}
