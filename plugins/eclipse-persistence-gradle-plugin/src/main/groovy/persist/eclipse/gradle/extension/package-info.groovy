/**
 * DSL extension classes for configuring EclipseLink Gradle plugin behavior.
 * <p>
 * This package contains the configuration model exposed to build scripts via the
 * {@code eclipselink} extension block. It provides per-source-set configuration
 * through a {@link org.gradle.api.NamedDomainObjectContainer}, allowing independent
 * settings for {@code main}, {@code test}, and custom source set variants.
 * </p>
 * <p>
 * Key classes:
 * </p>
 * <ul>
 *     <li>{@link persist.eclipse.gradle.extension.EclipselinkExtension} &mdash;
 *         Source-set-scoped configuration container for EclipseLink tools.</li>
 *     <li>{@link persist.eclipse.gradle.extension.JpaModelgenExtension} &mdash;
 *         Configuration attributes for the EclipseLink Canonical Model Generator,
 *         including the {@code persistence.xml} file location.</li>
 * </ul>
 *
 * @since 1.0.0
 */
package persist.eclipse.gradle.extension
