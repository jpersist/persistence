package persist.eclipse.gradle.extension

import org.gradle.api.Action
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory

import javax.inject.Inject

/**
 * Holds specific configuration attributes for the EclipseLink Canonical Model Generator.
 * <p>
 * This class exposes lazy properties to manage the location of the {@code persistence.xml} file.
 * It is declared as an {@code abstract} class to allow Gradle's runtime orchestration engine
 * to intercept property tracking and decoration APIs natively.
 * </p>
 *
 * @since 1.0.0
 */
abstract class JpaModelgenExtension {

    /**
     * Configures the lazy file property location mapping to the target {@code persistence.xml}.
     * <p>
     * Managed as a {@link RegularFileProperty} to delay actual file handle resolution until
     * execution phase, keeping build operations safe for the Gradle Configuration Cache.
     * </p>
     *
     * @return The lazy file property tracking the persistence configuration.
     */
    final RegularFileProperty persistenceXml

    /**
     * Internal tracking reference to the layout structure of the host Gradle project module.
     */
    private final ProjectLayout layout

    /**
     * Constructs a new {@code JpaModelgenExtension} instance.
     * <p>
     * Uses dependency injection to provision critical Gradle infrastructure services safely
     * without leaking temporary or mutable build state handles.
     * </p>
     *
     * @param objects The injected object factory used to instantiate tracking properties.
     * @param layout  The injected project layout used to resolve file pathways.
     */
    @Inject
    JpaModelgenExtension(ObjectFactory objects, ProjectLayout layout) {
        this.layout = layout
        this.persistenceXml = objects.fileProperty()
    }

    /**
     * Operator overload shortcut allowing developers to assign the {@code persistence.xml} location
     * using a simple Groovy String path syntax.
     * <p>
     * For example, in a consumer build script:
     * <pre>
     * jpaModelgen {
     *     persistenceXml = 'src/custom/resources/META-INF/persistence.xml'
     * }
     * </pre>
     * </p>
     *
     * @param path The project-relative file pathway configuration string.
     */
    void setPersistenceXml(String path) {
        persistenceXml.set(layout.projectDirectory.file(path))
    }

}

/**
 * Source set specific configuration container for EclipseLink tools.
 * <p>
 * This class is managed inside a {@link org.gradle.api.NamedDomainObjectContainer}, which automatically
 * instantiates and seeds a distinct configuration sandbox context for every active project source set layout
 * (such as {@code main}, {@code test}, or custom variants like {@code integrationTest}).
 * </p>
 *
 * @since 1.0.0
 */
abstract class EclipselinkExtension {

    /**
     * The name of the tracking source set associated with this specific domain configuration block.
     * <p>
     * Populated automatically by Gradle's container instantiation engine matching the container key.
     * </p>
     *
     * @return The name string identifying the associated source set.
     */
    final String name

    /**
     * The JPA Modelgen processing configuration sub-block parameters allocated to this source set layer.
     *
     * @return The canonical model generation configuration block instance.
     */
    final JpaModelgenExtension jpaModelgen

    /**
     * Constructs a new {@code EclipselinkExtension} instance.
     * <p>
     * Configures the named sub-scope context and instantiates structural inner configuration extensions
     * using the injected {@link ObjectFactory} service wrapper.
     * </p>
     *
     * @param name    The name of the source set block.
     * @param objects The injected object factory for creating nested extension abstractions.
     */
    @Inject
    EclipselinkExtension(String name, ObjectFactory objects) {
        this.name = name
        this.jpaModelgen = objects.newInstance(JpaModelgenExtension)
    }

    /**
     * Configures the JPA static metamodel generation paths for this source set scope via a DSL configuration closure.
     * <p>
     * For example, inside a multi-source set configuration build:
     * <pre>
     * eclipselink {
     *     main {
     *         jpaModelgen {
     *             persistenceXml = 'src/main/resources/META-INF/persistence.xml'
     *         }
     *     }
     * }
     * </pre>
     * </p>
     *
     * @param action The configuration action mapping execution targets to {@link JpaModelgenExtension}.
     */
    void jpaModelgen(Action<? super JpaModelgenExtension> action) {
        action.execute(jpaModelgen)
    }

}
