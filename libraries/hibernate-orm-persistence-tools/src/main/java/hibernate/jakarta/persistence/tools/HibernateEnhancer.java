package hibernate.jakarta.persistence.tools;

import hibernate.base.persistence.tools.HibernateEnhancerOption;
import org.hibernate.bytecode.enhance.spi.*;
import hibernate.jakarta.persistence.tools.provider.HibernateEnhancerProvider;
import persistence.tools.Enhancer;

import java.io.File;
import java.util.*;

import static hibernate.base.persistence.tools.HibernateUtil.*;
import static org.hibernate.bytecode.internal.BytecodeProviderInitiator.buildDefaultBytecodeProvider;

/**
 * {@link Enhancer} implementation for the Jakarta Persistence
 * namespace ({@code "jakarta"}).
 *
 * <p>Uses the Hibernate ORM&nbsp;6 bytecode enhancement SPI to transform
 * compiled entity classes. The enhancer is configured with a set of
 * {@link HibernateEnhancerOption} flags that control which
 * transformations — lazy initialization, dirty tracking, association
 * management, or extended enhancement — are applied.</p>
 *
 * <p>Instances are created through the {@link #from(ClassLoader, java.util.Map)}
 * or {@link #from(java.util.Collection, java.util.Map)} factory
 * methods.</p>
 *
 * @since 1.0.0
 * @see HibernateEnhancerProvider
 * @see Enhancer
 * @see HibernateEnhancerOption
 */
public class HibernateEnhancer implements Enhancer {

    private final org.hibernate.bytecode.enhance.spi.Enhancer enhancer;

    private HibernateEnhancer(ClassLoader classLoader, Map<HibernateEnhancerOption, Boolean> options) {
        EnhancementContext enhancementContext = createEnhancementContext(classLoader, options);
        enhancer = buildDefaultBytecodeProvider().getEnhancer(enhancementContext);
    }

    /**
     * Creates a new {@code HibernateEnhancer} using the supplied
     * class loader and enhancement options.
     *
     * @param classLoader the class loader used to resolve entity classes
     * @param settings    enhancement settings
     * @return a new enhancer instance
     */
    public static HibernateEnhancer from(ClassLoader classLoader,
                                         Map<String, Object> settings) {
        return new HibernateEnhancer(classLoader, convertSettings(settings));
    }

    /**
     * Creates a new {@code HibernateEnhancer} by building a
     * {@link java.net.URLClassLoader} from the supplied classpath
     * file collections.
     *
     * @param classpathFilesCollection  additional classpath entries (JARs or directories)
     * @param settings                  enhancement settings
     * @return a new enhancer instance
     */
    public static HibernateEnhancer from(Collection<Set<File>> classpathFilesCollection,
                                         Map<String, Object> settings) {
        ClassLoader classLoader = createURLClassLoader(org.hibernate.bytecode.enhance.spi.Enhancer.class.getClassLoader(), classpathFilesCollection);
        return new HibernateEnhancer(classLoader, convertSettings(settings));
    }

    /** {@inheritDoc} */
    @Override
    public byte[] enhance(String className, byte[] classBytes) {
        enhancer.discoverTypes(className, classBytes);
        return enhancer.enhance(className, classBytes);
    }

    private static EnhancementContext createEnhancementContext(ClassLoader classLoader,
                                                               Map<HibernateEnhancerOption, Boolean> options) {
        return new DefaultEnhancementContext() {

            @Override
            public ClassLoader getLoadingClassLoader() {
                return classLoader;
            }

            @Override
            public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
                return options.getOrDefault(HibernateEnhancerOption.ASSOCIATION_MANAGEMENT, false);
            }

            @Override
            public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
                return options.getOrDefault(HibernateEnhancerOption.DIRTY_TRACKING, false);
            }

            @Override
            public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
                return options.getOrDefault(HibernateEnhancerOption.LAZY_INITIALIZATION, false);
            }

            @Override
            public boolean isLazyLoadable(UnloadedField field) {
                return options.getOrDefault(HibernateEnhancerOption.LAZY_INITIALIZATION, false);
            }

            @Override
            public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
                return options.getOrDefault(HibernateEnhancerOption.EXTENDED_ENHANCEMENT, false);
            }

        };
    }

}
