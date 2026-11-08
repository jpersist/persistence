package persistence.tools.spi;

import persistence.tools.Enhancer;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Service-provider interface for obtaining a namespace-specific
 * {@link Enhancer}.
 *
 * <p>Implementations are discovered at runtime via
 * {@link java.util.ServiceLoader}. The convenience methods
 * {@link #getEnhancerProvider(String)},
 * {@link #getEnhancerProvider(String, ClassLoader)}, and
 * {@link #getEnhancerProvider(String, ModuleLayer)} map a JPA
 * specification version to the correct provider across standard, custom
 * classloader, or modular {@link ModuleLayer} runtime environments.</p>
 *
 * <p>In JPMS environments, provider modules register their implementation
 * with {@code provides persistence.tools.spi.HibernateEnhancerProvider}
 * in {@code module-info.java}.</p>
 *
 * @since 1.0.0
 * @see Enhancer
 * @see PersistenceServiceProvider
 */
public interface EnhancerProvider extends PersistenceServiceProvider {

    /**
     * Creates a {@link Enhancer} configured with the given
     * classpath and enhancement options.
     *
     * @param classesDir     the directory containing compiled entity class files
     * @param classpathFiles additional classpath entries required for
     *                       enhancement (e.g.&nbsp;JPA API and Hibernate JARs)
     * @param options        a map of {@link Enhancer.Option} flags
     *                       controlling which transformations are applied
     * @return a new enhancer instance
     */
    Enhancer getEnhancer(File classesDir, Set<File> classpathFiles,
                         Map<Enhancer.Option, Boolean> options);

    /**
     * Discovers and returns the {@code HibernateEnhancerProvider} for the
     * given JPA specification version using the thread context class loader.
     *
     * @param jpaVersion the JPA specification version (e.g.&nbsp;{@code "3.1"})
     * @return the matching provider
     * @throws IllegalArgumentException if {@code jpaVersion} is not recognized
     * @throws IllegalStateException    if no provider is registered for the
     *                                  resolved namespace
     * @see PersistenceServiceProvider#getServiceProvider(String, Class)
     */
    static EnhancerProvider getEnhancerProvider(String jpaVersion) {
        return PersistenceServiceProvider.getServiceProvider(jpaVersion, EnhancerProvider.class);
    }

    /**
     * Discovers and returns the {@code HibernateEnhancerProvider} for the
     * given JPA specification version using the specified {@link ClassLoader}.
     *
     * @param jpaVersion  the JPA specification version (e.g.&nbsp;{@code "3.1"})
     * @param classLoader the class loader used to discover the provider
     * @return the matching provider
     * @throws IllegalArgumentException if {@code jpaVersion} is not recognized
     * @throws IllegalStateException    if no provider is registered for the
     *                                  resolved namespace
     * @see PersistenceServiceProvider#getServiceProvider(String, Class, ClassLoader)
     */
    static EnhancerProvider getEnhancerProvider(String jpaVersion, ClassLoader classLoader) {
        return PersistenceServiceProvider.getServiceProvider(jpaVersion, EnhancerProvider.class, classLoader);
    }

    /**
     * Discovers and returns the {@code HibernateEnhancerProvider} for the
     * given JPA specification version using the specified {@link ModuleLayer}.
     *
     * @param jpaVersion the JPA specification version (e.g.&nbsp;{@code "3.1"})
     * @param layer      the module layer used to discover the provider
     * @return the matching provider
     * @throws IllegalArgumentException if {@code jpaVersion} is not recognized
     * @throws IllegalStateException    if no provider is registered for the
     *                                  resolved namespace
     * @see PersistenceServiceProvider#getServiceProvider(String, Class, ModuleLayer)
     */
    static EnhancerProvider getEnhancerProvider(String jpaVersion, ModuleLayer layer) {
        return PersistenceServiceProvider.getServiceProvider(jpaVersion, EnhancerProvider.class, layer);
    }

}
