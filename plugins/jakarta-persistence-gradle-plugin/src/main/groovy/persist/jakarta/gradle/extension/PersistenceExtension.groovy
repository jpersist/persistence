package persist.jakarta.gradle.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

import javax.inject.Inject

/**
 * Top-level DSL extension for the Jakarta Persistence plugin.
 * <p>
 * Registered under the name {@code persistence} in the project, this extension
 * exposes the JPA specification version and a
 * {@link org.gradle.api.NamedDomainObjectContainer} of
 * {@link PersistenceUnitExtension} instances, allowing build scripts to
 * declaratively define one or more persistence units.
 * </p>
 * <p>
 * Declared as {@code abstract} so that Gradle's managed-property engine can
 * generate the backing implementation for lazy {@link org.gradle.api.provider.Property}
 * fields.
 * </p>
 *
 * @since 1.1.0
 */
abstract class PersistenceExtension {

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
     * @param objects The Gradle {@link ObjectFactory} used to create the
     *                {@link NamedDomainObjectContainer}.
     */
    @Inject
    PersistenceExtension(ObjectFactory objects) {
        this.persistenceUnits = objects.domainObjectContainer(PersistenceUnitExtension)
        // Default to spec version 3.0
        this.version.convention("3.0")
    }

    /**
     * Configures the {@link #persistenceUnits} container using the given action.
     * <p>
     * This method enables the standard Gradle nested-closure notation in build
     * scripts:
     * </p>
     * <pre>
     * persistence {
     *     persistenceUnits {
     *         myUnit { ... }
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
