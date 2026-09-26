/**
 * Gradle plugin implementations for Hibernate JPA integration.
 * <p>
 * This package provides three plugins that automate Hibernate build-time tasks:
 * </p>
 * <ul>
 *     <li>{@link persist.hibernate.gradle.plugin.HibernateJpamodelgenPlugin}
 *         ({@code io.github.jpersist.hibernate-jpamodelgen}) &mdash;
 *         Registers the Hibernate JPA annotation processor to generate static
 *         canonical metamodel classes for type-safe Criteria API queries.</li>
 *     <li>{@link persist.hibernate.gradle.plugin.HibernateEnhancementPlugin}
 *         ({@code io.github.jpersist.hibernate-enhancement}) &mdash;
 *         Performs compile-time bytecode enhancement using Hibernate's
 *         {@code Enhancer} engine, enabling lazy initialization, dirty tracking,
 *         and association management without a runtime agent.</li>
 *     <li>{@link persist.hibernate.gradle.plugin.HibernatePersistencePlugin}
 *         ({@code io.github.jpersist.hibernate-persistence}) &mdash;
 *         Aggregate plugin that applies both the modelgen and enhancement
 *         plugins in a single declaration.</li>
 * </ul>
 * <p>
 * All plugins are fully compliant with Gradle's Configuration Cache and support
 * incremental builds with proper {@code UP-TO-DATE} and {@code NO-SOURCE} tracking.
 * </p>
 *
 * @since 1.0.0
 */
package persist.hibernate.gradle.plugin
