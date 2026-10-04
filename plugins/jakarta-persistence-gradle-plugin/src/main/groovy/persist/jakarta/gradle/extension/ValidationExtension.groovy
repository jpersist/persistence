package persist.jakarta.gradle.extension

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * DSL extension interface for configuring database connection properties used
 * during JPA schema validation.
 * <p>
 * An instance of this interface is embedded inside each
 * {@link PersistenceExtension} and can be configured via the {@code validation}
 * closure in the build script. All properties are optional; when omitted, the
 * {@link persist.jakarta.gradle.task.ValidatePersistenceSchema} task falls back
 * to an in-memory H2 database with sensible defaults.
 * </p>
 * <p>Example usage:</p>
 * <pre>
 * persistence {
 *     main {
 *         validation {
 *             url = 'jdbc:h2:mem:my_validation_db;DB_CLOSE_DELAY=-1'
 *             driver = 'org.h2.Driver'
 *             user = 'sa'
 *             password = ''
 *         }
 *     }
 * }
 * </pre>
 *
 * @since 1.5.0
 * @see PersistenceExtension#validation
 * @see persist.jakarta.gradle.task.ValidatePersistenceSchema
 */
interface ValidationExtension {

    /**
     * The JDBC connection URL used for schema validation.
     * <p>
     * Defaults to {@code "jdbc:h2:mem:schema_validate_db;DB_CLOSE_DELAY=-1"}
     * when not specified.
     * </p>
     *
     * @return The lazy property tracking the JDBC URL string.
     */
    @Input
    @Optional
    Property<String> getUrl()

    /**
     * The fully qualified JDBC driver class name used for schema validation.
     * <p>
     * Defaults to {@code "org.h2.Driver"} when not specified.
     * </p>
     *
     * @return The lazy property tracking the JDBC driver class name.
     */
    @Input
    @Optional
    Property<String> getDriver()

    /**
     * The database user name used for schema validation.
     * <p>
     * Defaults to {@code "sa"} when not specified.
     * </p>
     *
     * @return The lazy property tracking the database user name.
     */
    @Input
    @Optional
    Property<String> getUser()

    /**
     * The database password used for schema validation.
     * <p>
     * Defaults to an empty string when not specified.
     * </p>
     *
     * @return The lazy property tracking the database password.
     */
    @Input
    @Optional
    Property<String> getPassword()

}
