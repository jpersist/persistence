/**
 * Custom Gradle task implementations for Jakarta Persistence descriptor processing.
 * <p>
 * This package contains the task classes that perform the actual
 * {@code persistence.xml} generation and merging work orchestrated by the
 * Jakarta Persistence plugin.
 * </p>
 * <ul>
 *     <li>{@link persist.jakarta.gradle.task.ProcessPersistenceDescriptor} &mdash;
 *         Generates a new {@code persistence.xml} from the plugin extension DSL
 *         or merges an existing user-provided descriptor with extension-defined
 *         overrides and resolved {@code jar-file} entries. Disabled persistence
 *         units are automatically excluded, and the final output is formatted
 *         according to configurable transformer properties. Processing is
 *         delegated to version-specific strategy implementations resolved via
 *         {@link persist.jakarta.gradle.task.registry.JPAVersionStrategyRegistry}.</li>
 * </ul>
 *
 * @since 1.1.0
 * @see persist.jakarta.gradle.task.delegate
 * @see persist.jakarta.gradle.task.registry
 */
package persist.jakarta.gradle.task
