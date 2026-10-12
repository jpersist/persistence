/**
 * Gradle Worker API implementations for Jakarta Persistence build-time analysis.
 * <p>
 * This package contains the worker actions and their parameter interfaces used to
 * perform isolated build-time analysis of JPA entity mappings inside classloaders
 * managed by Gradle's {@link org.gradle.workers.WorkerExecutor}.
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
 *     <li>{@link persist.jakarta.gradle.worker.LintNamedQueriesParameters} &mdash;
 *         Parameter interface carrying the compiled classes directory, JPA
 *         version, and linter options into the isolated worker process.</li>
 *     <li>{@link persist.jakarta.gradle.worker.LintNamedQueriesWorker} &mdash;
 *         {@link org.gradle.workers.WorkAction} that performs ASM bytecode
 *         scanning to discover {@code @NamedQuery} and
 *         {@code @NamedStoredProcedureQuery} annotations, then validates their
 *         JPQL/HQL syntax and parameter naming conventions using an ANTLR4-based
 *         parser loaded via SPI.</li>
 * </ul>
 *
 * @since 1.5.0
 * @see persist.jakarta.gradle.task.ValidatePersistenceSchema
 * @see persist.jakarta.gradle.task.LintNamedQueries
 */
package persist.jakarta.gradle.worker
