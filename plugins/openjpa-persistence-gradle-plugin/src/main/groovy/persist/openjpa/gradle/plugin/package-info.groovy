/**
 * Gradle plugin implementations for OpenJPA persistence integration.
 * <p>
 * This package provides three plugins that automate OpenJPA build-time tasks:
 * </p>
 * <ul>
 *     <li>{@link persist.openjpa.gradle.plugin.OpenJPAMetaModelGenerationPlugin}
 *         ({@code io.github.jpersist.openjpa-metamodelgen}) &mdash;
 *         Generates JPA static canonical metamodel classes using the OpenJPA
 *         annotation processor.</li>
 *     <li>{@link persist.openjpa.gradle.plugin.OpenJPAEnhancementPlugin}
 *         ({@code io.github.jpersist.openjpa-enhancement}) &mdash;
 *         Performs compile-time bytecode enhancement via the OpenJPA
 *         {@code PCEnhancer} tool, injecting persistence capability hooks
 *         directly into entity class binaries.</li>
 *     <li>{@link persist.openjpa.gradle.plugin.OpenJPAPersistencePlugin}
 *         ({@code io.github.jpersist.openjpa-persistence}) &mdash;
 *         Aggregate plugin that applies both the metamodel generation and
 *         enhancement plugins in a single declaration.</li>
 * </ul>
 * <p>
 * All plugins support per-source-set configuration through the {@code openjpa}
 * extension block and integrate seamlessly with the standard Gradle build lifecycle.
 * </p>
 *
 * @since 1.8.0
 */
package persist.openjpa.gradle.plugin
