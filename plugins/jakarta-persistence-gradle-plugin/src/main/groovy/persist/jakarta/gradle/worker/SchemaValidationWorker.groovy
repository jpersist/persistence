package persist.jakarta.gradle.worker

import jakarta.persistence.Persistence
import jakarta.persistence.EntityManagerFactory
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

/**
 * Parameter interface for the {@link SchemaValidationWorker} work action.
 * <p>
 * Carries the persistence unit names and JDBC connection properties from the
 * {@link persist.jakarta.gradle.task.ValidatePersistenceSchema} task into the
 * isolated worker process. All values are resolved eagerly by the task before
 * being passed to the worker.
 * </p>
 *
 * @since 1.5.0
 * @see SchemaValidationWorker
 */
interface SchemaValidationParameters extends WorkParameters {

    /**
     * Comma-separated persistence unit names to validate.
     *
     * @return The property tracking the unit names string.
     */
    Property<String> getPersistenceUnitNames()

    /**
     * The JDBC connection URL for the validation database.
     *
     * @return The property tracking the JDBC URL.
     */
    Property<String> getValidationUrl()

    /**
     * The fully qualified JDBC driver class name.
     *
     * @return The property tracking the driver class name.
     */
    Property<String> getValidationDriver()

    /**
     * The database user name.
     *
     * @return The property tracking the user name.
     */
    Property<String> getValidationUser()

    /**
     * The database password.
     *
     * @return The property tracking the password.
     */
    Property<String> getValidationPassword()

}

/**
 * Gradle {@link WorkAction} that validates JPA entity mappings against a
 * database schema inside an isolated classloader.
 * <p>
 * For each persistence unit name supplied via
 * {@link SchemaValidationParameters#getPersistenceUnitNames()}, this worker
 * boots a standard JPA {@link jakarta.persistence.EntityManagerFactory} with
 * the {@code validate} schema generation action. If the entity metadata does
 * not match the database schema, the JPA provider throws an exception and the
 * enclosing Gradle task fails.
 * </p>
 * <p>
 * The worker is submitted by
 * {@link persist.jakarta.gradle.task.ValidatePersistenceSchema} using
 * classloader isolation so that the project's JPA provider, entity classes,
 * and JDBC driver are loaded in a separate classloader, avoiding conflicts
 * with the Gradle daemon's own classpath.
 * </p>
 *
 * @since 1.5.0
 * @see SchemaValidationParameters
 * @see persist.jakarta.gradle.task.ValidatePersistenceSchema
 */
abstract class SchemaValidationWorker implements WorkAction<SchemaValidationParameters> {

    @Override
    void execute() {
        String rawUnitNames = parameters.persistenceUnitNames.get()
        List<String> unitNames = rawUnitNames.split(',').collect { it.trim() }.findAll { !it.empty }

        // Setup ephemeral H2 in-memory properties
        Map<String, String> properties = [
            "jakarta.persistence.jdbc.driver"              : parameters.validationDriver.get(),
            "jakarta.persistence.jdbc.url"                 : parameters.validationUrl.get(),
            "jakarta.persistence.jdbc.user"                : parameters.validationUser.get(),
            "jakarta.persistence.jdbc.password"            : parameters.validationPassword.get(),
            "jakarta.persistence.schema-generation.database.action": "validate",
            "hibernate.hbm2ddl.auto"                       : "validate"
        ]

        unitNames.each { unitName ->
            println "⏳ Initiating automated schema validation guard for unit: '${unitName}'..."
            try {
                // Pure, strongly-typed standard JPA boot call
                EntityManagerFactory emf = Persistence.createEntityManagerFactory(unitName, properties)
                emf.close()
            } catch (Exception e) {
                System.err.println("❌ SCHEMA VALIDATION CRASHED FOR UNIT '${unitName}':")
                throw e // Propagate to fail the Gradle task execution cleanly
            }
        }

        println "✅ All schemas validated successfully!"
    }

}
