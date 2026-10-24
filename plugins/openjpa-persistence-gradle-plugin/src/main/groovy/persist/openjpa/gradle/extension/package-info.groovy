/**
 * DSL extension classes for configuring OpenJPA Gradle plugin behavior.
 * <p>
 * This package contains the configuration model exposed to build scripts via the
 * {@code openjpa} extension block. It provides per-source-set configuration
 * through a {@link org.gradle.api.NamedDomainObjectContainer}, allowing independent
 * settings for {@code main}, {@code test}, and custom source set variants.
 * </p>
 * <p>
 * Key classes:
 * </p>
 * <ul>
 *     <li>{@link persist.openjpa.gradle.extension.OpenJPAExtension} &mdash;
 *         Source-set-scoped configuration container for OpenJPA tools.</li>
 *     <li>{@link persist.openjpa.gradle.extension.MetaModelExtension} &mdash;
 *         Configuration attributes for the OpenJPA canonical metamodel generator,
 *         including logging level, source version, and naming conventions.</li>
 *     <li>{@link persist.openjpa.gradle.extension.EnhancementExtension} &mdash;
 *         Configuration attributes for the OpenJPA bytecode enhancement engine,
 *         controlling default constructor generation and property restriction enforcement.</li>
 * </ul>
 *
 * @since 1.8.0
 */
package persist.openjpa.gradle.extension
