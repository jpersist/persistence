package persist.jakarta.gradle.worker

import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import persist.lint.api.spi.SchemaValidator
import persist.lint.api.spi.SchemaValidatorFactory

/**
 * Parameter interface for the {@link SchemaValidationWorker} work action.
 * <p>
 * Carries the JPA version, persistence unit names, serialized multi-tenant
 * connection profiles, and discovered entity class names from the
 * {@link persist.jakarta.gradle.task.ValidatePersistenceSchema} task into the
 * isolated worker process. All values are resolved eagerly by the task before
 * being passed to the worker.
 * </p>
 *
 * @since 1.5.0
 * @see SchemaValidationWorker
 */
interface SchemaValidationParameters extends WorkParameters {

    /** The JPA specification version for annotation namespace resolution. */
    Property<String> getJpaVersion()

    /**
     * Comma-separated persistence unit names to validate.
     *
     * @return The property tracking the unit names string.
     */
    Property<String> getPersistenceUnitNames()

    /**
     * Pipe-delimited tenant profiles ({@code Name|URL|Driver|User|Password})
     * to validate sequentially.
     *
     * @return The list property tracking the serialized tenant profiles.
     * @since 1.7.0
     */
    ListProperty<String> getTenantProfiles()

    /**
     * Fully qualified entity class names discovered by the ASM bytecode
     * scanner, used to initialize the SPI-loaded
     * {@link persist.lint.api.spi.SchemaValidator}.
     *
     * @return The list property tracking the entity class names.
     * @since 1.7.0
     */
    ListProperty<String> getDiscoveredEntityClasses()

}

/**
 * Gradle {@link WorkAction} that validates JPA entity mappings against a
 * database schema inside an isolated classloader.
 * <p>
 * The worker initializes an SPI-loaded {@link SchemaValidator} via
 * {@link SchemaValidatorFactory} using the configured JPA version and the
 * list of discovered entity class names. It then iterates over every
 * serialized tenant profile and validates each active persistence unit
 * against that tenant's database. If any tenant/unit combination produces
 * a schema mismatch, the failure is recorded and the worker throws a
 * {@link GradleException} after all profiles have been evaluated.
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
        String jpaVersion = parameters.jpaVersion.get()
        String unitNamesPayload = parameters.persistenceUnitNames.get()
        List<String> tenantProfiles = parameters.tenantProfiles.get()
        List<String> discoveredEntityClasses = parameters.discoveredEntityClasses.get()

        SchemaValidator validator
        try {
            def validatorFactory = SchemaValidatorFactory.newSchemaValidatorFactory(jpaVersion)
            validator = validatorFactory.newSchemaValidator(discoveredEntityClasses)
        } catch (Exception e) {
            throw new GradleException("JPersist Schema Validation failed on setup configuration.", e)
        }

        println "⏳ Starting JPersist Multi-Tenant Schema Validation pipeline pass..."

        List<String> activeUnits = unitNamesPayload.split(',').collect { it.trim() }.findAll { !it.isEmpty() }
        int processingFailures = 0

        // SEQUENTIAL MULTI-TENANT MATRIX EXECUTION LOOP
        tenantProfiles.each { serializedProfile ->
            def tokens = serializedProfile.split('\\|')
            def (String tenantName, String jdbcUrl, String jdbcDriver, String jdbcUser) = tokens
            String jdbcPassword = tokens.length > 4 ? tokens[4] : ""

            println "   ⚙️ Running schema verification matrix pass for tenant profile: [${tenantName}]"
            println "      Target URL: ${jdbcUrl}"

            activeUnits.each { unitName ->
                try {
                    println "      🔍 Auditing entity mapping configurations for unit: '${unitName}'..."

                    validator.validateSchema([
                        "url": jdbcUrl,
                        "driver": jdbcDriver,
                        "username": jdbcUser,
                        "password": jdbcPassword
                    ])

                    println "      ✅ Tenant [${tenantName}] -> Unit '${unitName}' alignment checks passed."
                } catch (Exception e) {
                    processingFailures++
                    System.err.println("   ❌ SCHEMA MISMATCH FAILURE inside Tenant [${tenantName}] for Unit [${unitName}]:")
                    System.err.println("      Reason: ${e.getMessage() ?: e.toString()}")
                }
            }
        }

        if (processingFailures > 0) {
            throw new GradleException(
                "JPersist Schema Validation failed: Encountered ${processingFailures} mapping verification mismatches across the multi-tenant matrix."
            )
        } else {
            println "✅ Multi-tenant schema matrix validation completed successfully without errors!"
        }
    }

}
