package persist.hibernate.enhance.jakarta;

import persistence.tools.Enhancer;
import persistence.tools.spi.EnhancerProvider;
import persist.jakarta.spi.JakartaPersistenceServiceProvider;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * {@link EnhancerProvider} implementation for the Jakarta
 * Persistence namespace ({@code "jakarta"}).
 *
 * <p>This provider is registered as a {@link java.util.ServiceLoader}
 * provider and delegates to {@link JakartaHibernateEnhancer} for
 * bytecode enhancement using Hibernate ORM&nbsp;6.</p>
 *
 * @since 1.0.0
 * @see JakartaHibernateEnhancer
 * @see EnhancerProvider
 */
public class JakartaHibernateEnhancerProvider extends JakartaPersistenceServiceProvider implements EnhancerProvider {

    /** Creates a new {@code JakartaHibernateEnhancerProvider}. */
    public JakartaHibernateEnhancerProvider() {
    }

    /** {@inheritDoc} */
    @Override
    public Enhancer getEnhancer(File classesDir, Set<File> classpathFiles,
                                Map<Enhancer.Option, Boolean> options) {
        return JakartaHibernateEnhancer.from(classesDir, classpathFiles, options);
    }

}
