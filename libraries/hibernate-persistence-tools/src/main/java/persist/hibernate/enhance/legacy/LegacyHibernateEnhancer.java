package persist.hibernate.enhance.legacy;

import org.hibernate.bytecode.enhance.spi.*;
import org.hibernate.cfg.Environment;
import persist.hibernate.enhance.spi.HibernateEnhancer;

import java.io.File;
import java.util.Map;
import java.util.Set;

import static persist.hibernate.enhance.spi.HibernateEnhancer.createURLClassLoader;

/**
 * {@link HibernateEnhancer} implementation for the legacy
 * {@code javax.persistence} namespace ({@code "javax"}).
 *
 * <p>Uses the Hibernate&nbsp;5 bytecode enhancement SPI to transform
 * compiled entity classes. The enhancer is configured with a set of
 * {@link HibernateEnhancer.Option} flags that control which
 * transformations — lazy initialisation, dirty tracking, association
 * management, or extended enhancement — are applied.</p>
 *
 * <p>Instances are created through the {@link #from(ClassLoader, java.util.Map)}
 * or {@link #from(java.io.File, java.util.Set, java.util.Map)} factory
 * methods.</p>
 *
 * @since 1.0.0
 * @see LegacyHibernateEnhancerProvider
 * @see HibernateEnhancer
 */
public class LegacyHibernateEnhancer implements HibernateEnhancer {

    private final Enhancer enhancer;

    @SuppressWarnings("deprecation")
    private LegacyHibernateEnhancer(ClassLoader classLoader, Map<Option, Boolean> options) {
        EnhancementContext enhancementContext = createEnhancementContext(classLoader, options);
        this.enhancer = Environment.getBytecodeProvider().getEnhancer(enhancementContext);
    }

    /**
     * Creates a new {@code LegacyHibernateEnhancer} using the supplied
     * class loader and enhancement options.
     *
     * @param classLoader the class loader used to resolve entity classes
     * @param options     enhancement option flags
     * @return a new enhancer instance
     */
    public static LegacyHibernateEnhancer from(ClassLoader classLoader, Map<Option, Boolean> options) {
        return new LegacyHibernateEnhancer(classLoader, options);
    }

    /**
     * Creates a new {@code LegacyHibernateEnhancer} by building a
     * {@link java.net.URLClassLoader} from the given classes directory
     * and classpath files.
     *
     * @param classesDir     the directory containing compiled entity class files
     * @param classpathFiles additional classpath entries (JARs or directories)
     * @param options        enhancement option flags
     * @return a new enhancer instance
     */
    public static LegacyHibernateEnhancer from(File classesDir, Set<File> classpathFiles,
                                               Map<Option, Boolean> options) {
        ClassLoader classLoader = createURLClassLoader(classesDir, classpathFiles, Enhancer.class.getClassLoader());
        return new LegacyHibernateEnhancer(classLoader, options);
    }

    /** {@inheritDoc} */
    @Override
    public byte[] enhance(String className, byte[] classBytes) {
        return enhancer.enhance(className, classBytes);
    }

    private static EnhancementContext createEnhancementContext(ClassLoader classLoader,
                                                               Map<Option, Boolean> options) {
        return new DefaultEnhancementContext() {

            @Override
            public ClassLoader getLoadingClassLoader() {
                return classLoader;
            }

            @Override
            public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
                return options.getOrDefault(HibernateEnhancer.Option.ASSOCIATION_MANAGEMENT, false);
            }

            @Override
            public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
                return options.getOrDefault(Option.DIRTY_TRACKING, false);
            }

            @Override
            public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
                return options.getOrDefault(Option.LAZY_INITIALIZATION, false);
            }

            @Override
            public boolean isLazyLoadable(UnloadedField field) {
                return options.getOrDefault(Option.LAZY_INITIALIZATION, false);
            }

            @Override
            public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
                return options.getOrDefault(Option.EXTENDED_ENHANCEMENT, false);
            }

        };
    }

}
