package persist.hibernate.enhance.spi;

import persist.api.spi.PersistenceServiceProvider;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Service-provider interface for obtaining a namespace-specific
 * {@link HibernateEnhancer}.
 *
 * <p>Implementations are discovered at runtime via
 * {@link java.util.ServiceLoader}. The convenience method
 * {@link #getHibernateEnhancerProvider(String)} maps a JPA specification
 * version to the correct provider.</p>
 *
 * @since 1.0.0
 * @see HibernateEnhancer
 * @see PersistenceServiceProvider
 */
public interface HibernateEnhancerProvider extends PersistenceServiceProvider {

    /**
     * Creates a {@link HibernateEnhancer} configured with the given
     * classpath and enhancement options.
     *
     * @param classesDir     the directory containing compiled entity class files
     * @param classpathFiles additional classpath entries required for
     *                       enhancement (e.g.&nbsp;JPA API and Hibernate JARs)
     * @param options        a map of {@link HibernateEnhancer.Option} flags
     *                       controlling which transformations are applied
     * @return a new enhancer instance
     */
    HibernateEnhancer getEnhancer(File classesDir, Set<File> classpathFiles,
                                  Map<HibernateEnhancer.Option, Boolean> options);

    /**
     * Discovers and returns the {@code HibernateEnhancerProvider} for the
     * given JPA specification version.
     *
     * @param jpaVersion the JPA specification version (e.g.&nbsp;{@code "3.1"})
     * @return the matching provider
     * @throws IllegalArgumentException if {@code jpaVersion} is not recognised
     * @throws IllegalStateException    if no provider is registered for the
     *                                  resolved namespace
     * @see PersistenceServiceProvider#getServiceProvider(String, Class)
     */
    static HibernateEnhancerProvider getHibernateEnhancerProvider(String jpaVersion) {
        return PersistenceServiceProvider.getServiceProvider(jpaVersion, HibernateEnhancerProvider.class);
    }

}
