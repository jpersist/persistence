/**
 * Gradle plugin implementation for Jakarta Persistence (JPA) integration.
 * <p>
 * This package provides the core plugin that automates JPA descriptor management
 * at build time:
 * </p>
 * <ul>
 *     <li>{@link persist.jakarta.gradle.plugin.JakartaPersistencePlugin}
 *         ({@code io.github.jpersist.jpa}) &mdash;
 *         Registers the {@code persistence} extension DSL, the {@code jpa} and
 *         {@code jarFile} dependency configurations, and the
 *         {@code processPersistenceDescriptor} task that generates or merges the
 *         final {@code persistence.xml} into the build output. Supports
 *         skipping generation of individual persistence units and customizing
 *         the XML output formatting via transformer properties.</li>
 * </ul>
 * <p>
 * The plugin is designed as a replacement for the deprecated
 * {@code persistence-gradle-plugin} and is fully compliant with Gradle's
 * Configuration Cache.
 * </p>
 *
 * @since 1.1.0
 */
package persist.jakarta.gradle.plugin
