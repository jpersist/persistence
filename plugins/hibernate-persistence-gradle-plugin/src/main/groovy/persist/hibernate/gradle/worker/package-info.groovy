/**
 * Gradle Worker API implementations for Hibernate compile-time bytecode enhancement.
 * <p>
 * This package contains the worker action and its parameter interface used to
 * perform classloader-isolated bytecode enhancement of Hibernate entity classes
 * inside a worker process managed by Gradle's
 * {@link org.gradle.workers.WorkerExecutor}.
 * </p>
 * <ul>
 *     <li>{@link persist.hibernate.gradle.worker.EnhancementWorker} &mdash;
 *         {@link org.gradle.workers.WorkAction} that loads the Persistence Tools
 *         SPI factories and the Hibernate bytecode engine, iterates over every
 *         compiled {@code .class} file, applies the two-pass enhancement cycle
 *         (type discovery followed by byte transformation), and writes the
 *         results to an isolated target directory.</li>
 *     <li>{@link persist.hibernate.gradle.worker.EnhancementWorker.Parameters} &mdash;
 *         Parameter interface carrying the JPA version, input/output directories,
 *         the project's compilation classpath, and the four bytecode enhancement
 *         toggle flags from the
 *         {@link persist.hibernate.gradle.task.HibernateEnhancementTask} into the
 *         isolated worker process.</li>
 * </ul>
 *
 * @since 1.8.0
 * @see persist.hibernate.gradle.task.HibernateEnhancementTask
 */
package persist.hibernate.gradle.worker
