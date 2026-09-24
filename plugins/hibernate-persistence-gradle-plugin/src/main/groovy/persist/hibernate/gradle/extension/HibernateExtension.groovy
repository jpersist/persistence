package persist.hibernate.gradle.extension

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory

import javax.inject.Inject

/**
 * Root extension configuration block for Hibernate Persistence Utilities.
 * <p>
 * This extension serves as the entry-point DSL namespace {@code hibernate { ... }}
 * inside your build scripts, exposing nested configuration sub-blocks to customize
 * programmatic bytecode enhancement parameters.
 * </p>
 * <p>
 * Example usage in build.gradle:
 * <pre>
 * hibernate {
 *     enhancement {
 *         enableLazyInitialization = true
 *         enableDirtyTracking = true
 *     }
 * }
 * </pre>
 * </p>
 *
 * @since 1.0.0
 */
class HibernateExtension {

    /**
     * The underlying enhancement configuration extension instance.
     * <p>
     * Manages specific bytecode transformation attributes such as lazy loading,
     * dirty tracking, and relationship handling rules.
     * </p>
     *
     * @return The nested enhancement extension configuration instance.
     */
    final EnhancementExtension enhancement

    /**
     * Constructs a new {@code HibernateExtension} instance.
     * <p>
     * Employs Gradle dependency injection to pass an {@link ObjectFactory} service,
     * which instantiates the nested {@link EnhancementExtension} interface dynamically
     * at runtime while remaining compliant with the Configuration Cache rules.
     * </p>
     *
     * @param objects The injected object factory used to instantiate nested extension types.
     */
    @Inject
    HibernateExtension(ObjectFactory objects) {
        this.enhancement = objects.newInstance(EnhancementExtension)
    }

    /**
     * Configures the bytecode enhancement sub-block parameters via a DSL configuration closure.
     * <p>
     * For example, inside a target subproject build script:
     * <pre>
     * hibernate {
     *     enhancement {
     *         enableAssociationManagement = true
     *     }
     * }
     * </pre>
     * </p>
     *
     * @param action The configuration action mapping execution targets to {@link EnhancementExtension}.
     */
    void enhancement(Action<? super EnhancementExtension> action) {
        action.execute(this.enhancement)
    }
}
