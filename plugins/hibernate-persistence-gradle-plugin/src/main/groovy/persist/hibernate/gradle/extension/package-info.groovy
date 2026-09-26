/**
 * DSL extension classes for configuring Hibernate Gradle plugin behavior.
 * <p>
 * This package contains the configuration model exposed to build scripts via the
 * {@code hibernate} extension block, providing fine-grained control over
 * compile-time bytecode enhancement parameters.
 * </p>
 * <ul>
 *     <li>{@link persist.hibernate.gradle.extension.HibernateExtension} &mdash;
 *         Root extension serving as the {@code hibernate { ... }} DSL namespace,
 *         hosting nested configuration sub-blocks.</li>
 *     <li>{@link persist.hibernate.gradle.extension.EnhancementExtension} &mdash;
 *         Configuration interface exposing toggles for lazy initialization,
 *         dirty tracking, association management, and extended enhancement.</li>
 * </ul>
 *
 * @since 1.0.0
 */
package persist.hibernate.gradle.extension
