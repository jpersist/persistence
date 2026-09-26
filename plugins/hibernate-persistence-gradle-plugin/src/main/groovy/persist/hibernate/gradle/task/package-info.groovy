/**
 * Custom Gradle task implementations for Hibernate build-time processing.
 * <p>
 * This package contains the task classes that perform the actual bytecode
 * transformation work orchestrated by the Hibernate plugins.
 * </p>
 * <ul>
 *     <li>{@link persist.hibernate.gradle.task.HibernateEnhancementTask} &mdash;
 *         Programmatically invokes Hibernate's {@code Enhancer} engine to rewrite
 *         compiled entity classes, enabling field-level lazy initialization,
 *         inline dirty tracking, and bidirectional association management
 *         at the bytecode level.</li>
 * </ul>
 *
 * @since 1.0.0
 */
package persist.hibernate.gradle.task
