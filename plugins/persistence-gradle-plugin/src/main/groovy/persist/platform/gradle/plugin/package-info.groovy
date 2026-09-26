/**
 * Platform alignment plugin for centralized JPA dependency version management.
 * <p>
 * This package contains the {@link persist.platform.gradle.plugin.PersistencePlatformPlugin}
 * which introduces a dedicated {@code persistence} configuration that allows users to declare
 * a JPA implementation platform (BOM) dependency. Version constraints from the declared platform
 * are automatically propagated into all standard Java configurations ({@code implementation},
 * {@code compileOnly}, {@code annotationProcessor}, {@code runtimeOnly}, and
 * {@code testImplementation}), ensuring consistent version alignment across the entire project.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * plugins {
 *     id 'io.github.jpersist.persistence-platform'
 * }
 *
 * dependencies {
 *     persistence platform('org.hibernate.orm:hibernate-platform:6.6.5.Final')
 * }
 * </pre>
 *
 * @since 1.1.0
 */
package persist.platform.gradle.plugin
