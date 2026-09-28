package persist.jakarta.gradle.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
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

}
