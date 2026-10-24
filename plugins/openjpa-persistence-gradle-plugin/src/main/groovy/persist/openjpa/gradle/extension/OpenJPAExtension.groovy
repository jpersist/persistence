package persist.openjpa.gradle.extension

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Nested

import javax.inject.Inject

/**
 * Source-set-scoped configuration container for OpenJPA tools.
 * <p>
 * This class is managed inside a {@link org.gradle.api.NamedDomainObjectContainer}, which automatically
 * instantiates and seeds a distinct configuration sandbox context for every active project source set
 * (such as {@code main}, {@code test}, or custom variants like {@code integrationTest}).
 * It aggregates both the metamodel generation and bytecode enhancement configuration sub-blocks.
 * </p>
 *
 * @since 1.8.0
 */
class OpenJPAExtension {

    /**
     * The name of the source set associated with this specific domain configuration block.
     * <p>
     * Populated automatically by Gradle's container instantiation engine matching the container key.
     * </p>
     *
     * @return The name string identifying the associated source set.
     */
    final String name;

    /**
     * The metamodel generation configuration sub-block parameters allocated to this source set layer.
     *
     * @return The canonical metamodel generation configuration block instance.
     */
    @Nested
    final MetaModelExtension metaModel

    /**
     * The bytecode enhancement configuration sub-block parameters allocated to this source set layer.
     *
     * @return The enhancement configuration block instance.
     */
    @Nested
    final EnhancementExtension enhancement

    /**
     * Constructs a new {@code OpenJPAExtension} instance.
     * <p>
     * Configures the named sub-scope context and instantiates structural inner configuration extensions
     * using the injected {@link ObjectFactory} service wrapper. Default convention values are applied
     * for metamodel generation (enabled) and enhancement options (addDefaultConstructor = true,
     * enforcePropertyRestrictions = false).
     * </p>
     *
     * @param name    The name of the source set block.
     * @param objects The injected object factory for creating nested extension abstractions.
     */
    @Inject
    OpenJPAExtension(String name, ObjectFactory objects) {
        this.name = name
        this.metaModel = objects.newInstance(MetaModelExtension)
        this.enhancement = objects.newInstance(EnhancementExtension)

        this.metaModel.enabled.convention(true)

        this.enhancement.addDefaultConstructor.convention(true)
        this.enhancement.enforcePropertyRestrictions.convention(false)
    }

    /**
     * Configures the OpenJPA metamodel generation options for this source set scope via a DSL configuration closure.
     * <p>
     * For example, inside a build script:
     * <pre>
     * openjpa {
     *     main {
     *         metamodel {
     *             enabled = true
     *             logLevel = 'TRACE'
     *         }
     *     }
     * }
     * </pre>
     * </p>
     *
     * @param action The configuration action mapping execution targets to {@link MetaModelExtension}.
     */
    void metamodel(Action<? super MetaModelExtension> action) {
        action.execute(metaModel)
    }

    /**
     * Configures the OpenJPA bytecode enhancement options for this source set scope via a DSL configuration closure.
     * <p>
     * For example, inside a build script:
     * <pre>
     * openjpa {
     *     main {
     *         enhancement {
     *             addDefaultConstructor = true
     *             enforcePropertyRestrictions = false
     *         }
     *     }
     * }
     * </pre>
     * </p>
     *
     * @param action The configuration action mapping execution targets to {@link EnhancementExtension}.
     */
    void enhancement(Action<? super EnhancementExtension> action) {
        action.execute(enhancement)
    }

}
