package hibernate.javax.persistence.tools;

import hibernate.javax.persistence.tools.provider.HibernateEnhancerProvider;
import org.hibernate.bytecode.enhance.spi.*;
import org.hibernate.cfg.Environment;
import persistence.tools.Enhancer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * {@link Enhancer} implementation for the legacy
 * {@code javax.persistence} namespace ({@code "javax"}).
 *
 * <p>Uses the Hibernate&nbsp;5 bytecode enhancement SPI to transform
 * compiled entity classes. The enhancer is configured with a set of
 * {@link Option} flags that control which
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
 * @see Option
 */
public class HibernateEnhancer implements Enhancer {

    private final org.hibernate.bytecode.enhance.spi.Enhancer enhancer;

    @SuppressWarnings("deprecation")
    private HibernateEnhancer(ClassLoader classLoader, Map<Option, Boolean> options) {
        EnhancementContext enhancementContext = createEnhancementContext(classLoader, options);
        this.enhancer = Environment.getBytecodeProvider().getEnhancer(enhancementContext);
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
                return options.getOrDefault(Option.ASSOCIATION_MANAGEMENT, false);
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

    private static Map<Option, Boolean> convertSettings(Map<String, Object> settings) {
        Map<Option, Boolean> options = new HashMap<>();
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            Option option = Option.from(entry.getKey());
            options.put(option, (Boolean) entry.getValue());
        }
        return options;
    }

    /**
     * Converts a {@link File} to a {@link URL}.
     *
     * @param file the file to convert
     * @return the corresponding URL
     * @throws RuntimeException if the file URI cannot be converted to a URL
     */
    private static URL toURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a {@link URLClassLoader} whose classpath consists of the
     * compiled classes directory followed by the supplied classpath files.
     *
     * @param parent                    the parent class loader to delegate to
     * @param classpathFilesCollection  additional classpath entries (JARs or directories)
     * @return a new class loader covering the specified classpath
     */
    private static URLClassLoader createURLClassLoader(ClassLoader parent, Collection<Set<File>> classpathFilesCollection) {
        List<URL> urls = new ArrayList<>();
        classpathFilesCollection.stream().flatMap(Set::stream).forEach(file -> urls.add(toURL(file)));
        return new URLClassLoader(urls.toArray(new URL[0]), parent);
    }

    /**
     * Enhancement options that control which bytecode transformations
     * are applied during entity enhancement.
     */
    public enum Option {

        /** Enable lazy-loading of persistent attributes. */
        LAZY_INITIALIZATION("hibernate.enhancement.option.lazy-initialization"),

        /** Enable inline dirty tracking to avoid snapshot comparisons. */
        DIRTY_TRACKING("hibernate.enhancement.option.dirty-tracking"),

        /** Enable automatic bidirectional association management. */
        ASSOCIATION_MANAGEMENT("hibernate.enhancement.option.association-management"),

        /** Enable extended enhancement beyond the standard JPA model. */
        EXTENDED_ENHANCEMENT("hibernate.enhancement.option.extended-enhancement");

        private final String value;

        Option(String value) {
            this.value = value;
        }

        /**
         * Returns the string configuration property key associated with this option.
         *
         * @return the option key string
         */
        public String value() {
            return value;
        }

        /**
         * Resolves an {@code Option} from its string configuration property key.
         *
         * @param value the property key (e.g.&nbsp;{@code "hibernate.enhancement.option.lazy-initialization"})
         * @return the matching {@code Option}
         * @throws IllegalArgumentException if {@code value} does not match any known option
         */
        public static Option from(String value) {
            for (Option option : Option.values()) {
                if (option.value().equals(value)) {
                    return option;
                }
            }
            throw new IllegalArgumentException("Unknown option: " + value);
        }

    }

}
