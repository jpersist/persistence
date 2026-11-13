/**
 * Internal utilities for Gradle worker execution isolation and persistence SPI resolution.
 * <p>
 * This package provides helper facilities for configuring isolated worker executions
 * and discovering persistence provider SPI implementations across classloader and
 * JPMS modular runtime environments.
 * </p>
 * <ul>
 *     <li>{@link persist.jakarta.gradle.util.JPMSWorkerSupport} &mdash;
 *         Configures Gradle {@link org.gradle.workers.WorkerExecutor} work queues using either
 *         modular JPMS process isolation (when {@code module-info.class} is detected) or
 *         standard classloader isolation, dynamically configuring JVM module arguments,
 *         module paths, and reflective {@code --add-opens} flags.</li>
 *     <li>{@link persist.jakarta.gradle.util.PersistenceServiceResolver} &mdash;
 *         Resolves versioned {@link persistence.tools.spi.PersistenceServiceProvider}
 *         instances via {@link java.util.ServiceLoader}, checking both modular JPMS boot layers
 *         and the current thread's context classloader.</li>
 * </ul>
 *
 * @since 1.9.0
 */
package persist.jakarta.gradle.util
