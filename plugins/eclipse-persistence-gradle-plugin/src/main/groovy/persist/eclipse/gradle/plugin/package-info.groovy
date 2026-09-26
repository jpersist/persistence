/**
 * Gradle plugin implementations for EclipseLink JPA integration.
 * <p>
 * This package provides three plugins that automate EclipseLink build-time tasks:
 * </p>
 * <ul>
 *     <li>{@link persist.eclipse.gradle.plugin.EclipseJpaModelgenPlugin}
 *         ({@code io.github.jpersist.eclipse-jpa-modelgen}) &mdash;
 *         Generates JPA static canonical metamodel classes using the EclipseLink
 *         annotation processor.</li>
 *     <li>{@link persist.eclipse.gradle.plugin.EclipseStaticWeavePlugin}
 *         ({@code io.github.jpersist.eclipse-static-weave}) &mdash;
 *         Performs compile-time bytecode weaving via the EclipseLink
 *         {@code StaticWeave} tool, enabling lazy loading and dirty tracking
 *         without a runtime {@code -javaagent}.</li>
 *     <li>{@link persist.eclipse.gradle.plugin.EclipsePersistencePlugin}
 *         ({@code io.github.jpersist.eclipse-persistence}) &mdash;
 *         Aggregate plugin that applies both the modelgen and static weave
 *         plugins in a single declaration.</li>
 * </ul>
 * <p>
 * All plugins are fully compliant with Gradle's Configuration Cache and support
 * incremental builds with proper {@code UP-TO-DATE} and {@code NO-SOURCE} tracking.
 * </p>
 *
 * @since 1.0.0
 */
package persist.eclipse.gradle.plugin
