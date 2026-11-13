/**
 * Gradle Worker API implementations for Jakarta Persistence build-time analysis.
 * <p>
 * This package contains the worker actions and their parameter interfaces used to
 * perform isolated build-time analysis of JPA entity mappings inside worker processes
 * managed by Gradle's {@link org.gradle.workers.WorkerExecutor}.
 * </p>
 * <ul>
 *     <li>{@link persist.jakarta.gradle.worker.SchemaValidationParameters} &mdash;
 *         Parameter interface carrying the persistence unit names, multi-tenant
 *         connection profiles, and discovered entity class names into the isolated
 *         worker process.</li>
 *     <li>{@link persist.jakarta.gradle.worker.SchemaValidationWorker} &mdash;
 *         {@link org.gradle.workers.WorkAction} that validates JPA entity
 *         mappings against the target database schemas across a multi-tenant matrix
 *         using an SPI-loaded {@link persistence.tools.SchemaValidator}.</li>
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
