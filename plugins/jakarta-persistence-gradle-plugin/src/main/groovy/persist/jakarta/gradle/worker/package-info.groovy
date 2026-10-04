/**
 * Gradle Worker API implementations for Jakarta Persistence schema validation.
 * <p>
 * This package contains the worker action and its parameter interface used to
 * validate JPA entity mappings against a database schema inside an isolated
 * classloader managed by Gradle's {@link org.gradle.workers.WorkerExecutor}.
 * </p>
 * <ul>
 *     <li>{@link persist.jakarta.gradle.worker.SchemaValidationParameters} &mdash;
 *         Parameter interface carrying the persistence unit names and JDBC
 *         connection properties into the isolated worker process.</li>
 *     <li>{@link persist.jakarta.gradle.worker.SchemaValidationWorker} &mdash;
 *         {@link org.gradle.workers.WorkAction} that boots a standard JPA
 *         {@link jakarta.persistence.EntityManagerFactory} per persistence unit
 *         with the {@code validate} schema generation action, failing the build
 *         if entity metadata does not match the database schema.</li>
 * </ul>
 *
 * @since 1.5.0
 * @see persist.jakarta.gradle.task.ValidatePersistenceSchema
 */
package persist.jakarta.gradle.worker
