package persist.jakarta.gradle.worker

import jakarta.persistence.Persistence
import jakarta.persistence.EntityManagerFactory
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

/**
 * Parameters required by the Schema Validation Worker.
 */
interface SchemaValidationParameters extends WorkParameters {

    Property<String> getPersistenceUnitNames()

    Property<String> getValidationUrl()

    Property<String> getValidationDriver()

    Property<String> getValidationUser()

    Property<String> getValidationPassword()

}

/**
 * An isolated Worker Action executing standard typed JPA code without reflection.
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
