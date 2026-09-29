package persist.jakarta.gradle.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

import javax.inject.Inject

/**
 * Named DSL extension for the Jakarta Persistence plugin, scoped per Java source set.
 * <p>
 * Instances of this class are managed inside a
 * {@link org.gradle.api.NamedDomainObjectContainer} registered under the
 * {@code persistence} project extension. The plugin automatically creates one
 * instance per Java source set (e.g.&nbsp;{@code main}, {@code test}), using
 * the source set name as the container key. Each instance exposes the JPA
 * specification version and a nested
 * {@link org.gradle.api.NamedDomainObjectContainer} of
 * {@link PersistenceUnitExtension} instances, allowing build scripts to
 * declaratively define one or more persistence units per source set.
 * </p>
 * <p>
 * Declared as {@code abstract} so that Gradle's managed-property engine can
 * generate the backing implementation for lazy {@link org.gradle.api.provider.Property}
 * fields.
 * </p>
 * <p>Example usage:</p>
 * <pre>
 * persistence {
 *     main {
 *         persistenceUnits {
 *             'my-unit' {
 *                 provider = 'org.hibernate.jpa.HibernatePersistenceProvider'
 *             }
 *         }
 *     }
 * }
 * </pre>
 *
 * @since 1.1.0
 */
abstract class PersistenceExtension {

    private final String name;

    /**
     * The JPA specification version written into the generated {@code persistence.xml}.
     * <p>
     * Defaults to {@code "3.0"} (Jakarta Persistence 3.0).
     * </p>
     *
     * @return The lazy property tracking the specification version string.
     */
    @Input
    abstract Property<String> getVersion()

    /**
     * Container of named {@link PersistenceUnitExtension} instances.
     * <p>
     * Each entry maps to a {@code <persistence-unit>} element in the generated
     * or merged {@code persistence.xml}. The container name becomes the
     * {@code name} attribute of the unit.
     * </p>
     */
    final NamedDomainObjectContainer<PersistenceUnitExtension> persistenceUnits

    /**
     * Key-value pairs passed to the XML {@link javax.xml.transform.Transformer}
     * that formats the generated {@code persistence.xml}.
     * <p>
     * Defaults include {@code indent=yes}, {@code omit-xml-declaration=no},
     * {@code encoding=UTF-8}, and an indent amount of {@code 4} spaces.
     * Entries can be overridden via the {@link #transformer(Closure)} DSL block
     * or the {@link #outputProperty(String, String)} method.
     * </p>
     *
     * @return The lazy map property tracking the transformer output properties.
     * @since 1.2.2
     */
    @Input
    abstract MapProperty<String, String> getOutputProperties()

    /**
     * Creates a new extension instance.
     *
     * @param name    The source set name used as the container key.
     * @param objects The Gradle {@link ObjectFactory} used to create the
     *                {@link NamedDomainObjectContainer}.
     */
    @Inject
    PersistenceExtension(String name, ObjectFactory objects) {
        this.name = name
        // Default to spec version 3.0
        this.version.convention("3.0")
        this.persistenceUnits = objects.domainObjectContainer(PersistenceUnitExtension)

        // Set up the default transformer properties exactly as required
        this.outputProperties.put("indent", "yes")
        this.outputProperties.put("omit-xml-declaration", "no")
        this.outputProperties.put("encoding", "UTF-8")
        this.outputProperties.put("{http://xml.apache.org/xslt}indent-amount", "4")
    }

    /**
     * Returns the source set persistence name.
     * <p>
     * Read-only because it serves as the key for the
     * {@link org.gradle.api.NamedDomainObjectContainer}.
     * </p>
     *
     * @return The source set name.
     */
    @Input
    String getName() {
        return this.name
    }

    /**
     * Configures the {@link #persistenceUnits} container using the given action.
     * <p>
     * This method enables the standard Gradle nested-closure notation in build
     * scripts:
     * </p>
     * <pre>
     * persistence {
     *     main {
     *         persistenceUnits {
     *             myUnit { ... }
     *         }
     *     }
     * }
     * </pre>
     *
     * @param action The configuration action to apply to the container.
     */
    void persistenceUnits(Action<? super NamedDomainObjectContainer<PersistenceUnitExtension>> action) {
        action.execute(persistenceUnits)
    }

    /**
     * Sets a single XML transformer output property.
     * <p>
     * Convenience method typically called inside a {@link #transformer(Closure)}
     * block:
     * </p>
     * <pre>
     * persistence {
     *     main {
     *         transformer {
     *             outputProperty 'indent', 'yes'
     *             outputProperty '{http://xml.apache.org/xslt}indent-amount', '2'
     *         }
     *     }
     * }
     * </pre>
     *
     * @param key   The transformer output property key.
     * @param value The transformer output property value.
     * @since 1.2.2
     */
    void outputProperty(String key, String value) {
        outputProperties.put(key, value)
    }

    /**
     * Configures the XML transformer output properties using a closure.
     * <p>
     * Inside the closure, calls to {@link #outputProperty(String, String)} are
     * delegated to this extension instance, allowing a clean DSL syntax:
     * </p>
     * <pre>
     * persistence {
     *     main {
     *         transformer {
     *             outputProperty 'indent', 'yes'
     *             outputProperty '{http://xml.apache.org/xslt}indent-amount', '2'
     *         }
     *     }
     * }
     * </pre>
     *
     * @param closure The configuration closure applied with delegate-first strategy.
     * @since 1.2.2
     */
    void transformer(Closure<?> closure) {
        // Redirect execution scope to an isolated helper inside the extension execution matrix
        closure.setDelegate(this)
        closure.setResolveStrategy(Closure.DELEGATE_FIRST)
        closure.call()
    }

}
