/**
 * Custom Gradle task implementations for EclipseLink build-time processing.
 * <p>
 * This package contains the task classes that perform the actual bytecode
 * transformation work orchestrated by the EclipseLink plugins.
 * </p>
 * <ul>
 *     <li>{@link persist.eclipse.gradle.task.EclipseWeaveTask} &mdash;
 *         Executes the EclipseLink {@code StaticWeave} command-line processor
 *         in an isolated JVM process, transforming compiled entity classes to
 *         support lazy loading, fetch graph optimizations, and inline dirty
 *         tracking at the bytecode level.</li>
 * </ul>
 *
 * @since 1.0.0
 */
package persist.eclipse.gradle.task
