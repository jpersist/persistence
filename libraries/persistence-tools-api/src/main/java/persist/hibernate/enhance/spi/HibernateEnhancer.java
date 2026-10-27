package persist.hibernate.enhance.spi;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SPI for Hibernate bytecode enhancement of JPA entity classes.
 *
 * <p>Implementations transform compiled entity class bytes to add
 * Hibernate-specific instrumentation such as lazy-loading proxies,
 * inline dirty tracking, bi-directional association management, and
 * extended enhancement. The transformation is performed entirely at
 * build time so that enhanced classes are ready for deployment without
 * runtime bytecode manipulation.</p>
 *
 * <p>Concrete implementations are namespace-specific: Jakarta
 * Persistence classes are enhanced by
 * {@code JakartaHibernateEnhancer} (Hibernate&nbsp;6), while legacy
 * {@code javax.persistence} classes are handled by
 * {@code LegacyHibernateEnhancer} (Hibernate&nbsp;5). Instances are
 * typically obtained through a {@link HibernateEnhancerProvider}.</p>
 *
 * @since 1.0.0
 * @see HibernateEnhancerProvider
 * @see Option
 */
public interface HibernateEnhancer {

    /**
     * Enhances the bytecode of the given entity class.
     *
     * @param className  the fully qualified name of the class being enhanced
     * @param classBytes the original class bytes read from disk
     * @return the enhanced class bytes, or the original bytes if no
     *         enhancement was applied
     */
    byte[] enhance(String className, byte[] classBytes);

    /**
     * Converts a {@link File} to a {@link URL}.
     *
     * @param file the file to convert
     * @return the corresponding URL
     * @throws RuntimeException if the file URI cannot be converted to a URL
     */
    static URL toURL(File file) {
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
     * @param classesDir     the directory containing compiled class files
     * @param classpathFiles additional classpath entries (JARs or directories)
     * @param parent         the parent class loader to delegate to
     * @return a new class loader covering the specified classpath
     */
    static URLClassLoader createURLClassLoader(File classesDir, Set<File> classpathFiles, ClassLoader parent) {
        List<URL> urls = new ArrayList<>();
        urls.add(toURL(classesDir));
        urls.addAll(classpathFiles.stream().map(HibernateEnhancer::toURL).toList());
        return new URLClassLoader(urls.toArray(new URL[0]), parent);
    }

    /**
     * Enhancement options that control which bytecode transformations
     * are applied during entity enhancement.
     */
    enum Option {

        /** Enable lazy-loading of persistent attributes. */
        LAZY_INITIALIZATION,

        /** Enable inline dirty tracking to avoid snapshot comparisons. */
        DIRTY_TRACKING,

        /** Enable automatic bi-directional association management. */
        ASSOCIATION_MANAGEMENT,

        /** Enable extended enhancement beyond the standard JPA model. */
        EXTENDED_ENHANCEMENT

    }

}
