/**
 * DSL extension classes for configuring the Jakarta Persistence Gradle plugin.
 * <p>
 * This package contains the configuration model exposed to build scripts via the
 * {@code persistence} extension block. It allows declarative definition of one or
 * more JPA persistence units through a
 * {@link org.gradle.api.NamedDomainObjectContainer}, enabling the plugin to
 * generate or merge a {@code persistence.xml} descriptor at build time.
 * </p>
 * <p>
 * Key classes:
 * </p>
 * <ul>
 *     <li>{@link persist.jakarta.gradle.extension.PersistenceExtension} &mdash;
 *         Top-level extension providing the JPA specification version, XML
 *         transformer output properties for formatting the generated descriptor,
 *         and a container of named persistence-unit configurations.</li>
 *     <li>{@link persist.jakarta.gradle.extension.PersistenceUnitExtension} &mdash;
 *         Per-unit configuration attributes such as provider, transaction type,
 *         data source, mapping files, JPA properties, and an {@code enabled}
 *         flag to skip generation of individual persistence units.</li>
 * </ul>
 *
 * @since 1.1.0
 */
package persist.jakarta.gradle.extension
