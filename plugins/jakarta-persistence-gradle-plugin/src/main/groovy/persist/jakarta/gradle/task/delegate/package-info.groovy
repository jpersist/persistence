/**
 * Version-specific strategy delegates for JPA persistence descriptor processing.
 * <p>
 * This package contains the {@link persist.jakarta.gradle.task.delegate.DescriptorProcessorDelegate}
 * strategy interface and its implementations, each handling descriptor generation
 * and merging for a specific set of JPA specification versions.
 * </p>
 * <ul>
 *     <li>{@link persist.jakarta.gradle.task.delegate.DescriptorProcessorDelegate} &mdash;
 *         Strategy interface governing the descriptor generation and merge phases.</li>
 *     <li>{@link persist.jakarta.gradle.task.delegate.AbstractDescriptorProcessorDelegate} &mdash;
 *         Base implementation providing common merge and generate logic for all
 *         JPA specification versions.</li>
 *     <li>{@link persist.jakarta.gradle.task.delegate.JPA20DescriptorProcessorDelegate} &mdash;
 *         Delegate for the JPA 2.0 specification.</li>
 *     <li>{@link persist.jakarta.gradle.task.delegate.JPA21DescriptorProcessorDelegate} &mdash;
 *         Delegate for the JPA 2.1 and 2.2 specifications.</li>
 *     <li>{@link persist.jakarta.gradle.task.delegate.JPA30DescriptorProcessorDelegate} &mdash;
 *         Delegate for the JPA 3.0 and 3.1 specifications.</li>
 *     <li>{@link persist.jakarta.gradle.task.delegate.JPA32DescriptorProcessorDelegate} &mdash;
 *         Delegate for the JPA 3.2 specification.</li>
 * </ul>
 *
 * @since 1.4.0
 * @see persist.jakarta.gradle.task.registry.JPAVersionStrategyRegistry
 */
package persist.jakarta.gradle.task.delegate
