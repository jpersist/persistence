/**
 * Custom Gradle task implementations for OpenJPA build-time processing.
 * <p>
 * This package contains the task classes that perform the actual bytecode
 * transformation work orchestrated by the OpenJPA plugins.
 * </p>
 * <ul>
 *     <li>{@link persist.openjpa.gradle.task.OpenJPAEnhanceTask} &mdash;
 *         Executes the OpenJPA {@code PCEnhancer} command-line processor
 *         in an isolated JVM process, transforming compiled entity classes to
 *         inject persistence capability hooks at the bytecode level, enabling
 *         transparent state management and dirty tracking.</li>
 * </ul>
 *
 * @since 1.8.0
 */
package persist.openjpa.gradle.task
