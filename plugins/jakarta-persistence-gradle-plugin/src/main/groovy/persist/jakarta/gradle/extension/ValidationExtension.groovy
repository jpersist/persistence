package persist.jakarta.gradle.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

import javax.inject.Inject

/**
 * DSL extension class for configuring database connection properties used
 * during JPA schema validation.
 * <p>
 * An instance of this class is embedded inside each
 * {@link PersistenceExtension} and can be configured via the {@code validation}
 * closure in the build script. All properties are optional; when omitted, the
 * {@link persist.jakarta.gradle.task.ValidatePersistenceSchema} task falls back
 * to an in-memory H2 database with sensible defaults.
 * </p>
 * <p>
 * Since version 1.7.0, the extension supports <strong>multi-tenant</strong>
 * validation via a named {@code tenants} container. Each tenant profile
 * declares its own JDBC connection properties. When no tenants are configured,
 * the base properties are used as a single {@code default} profile for
 * backward compatibility.
 * </p>
 * <p>Example — single database (backward compatible):</p>
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
 * <p>Example — multi-tenant validation:</p>
 * <pre>
 * persistence {
 *     main {
 *         validation {
 *             tenants {
 *                 primary {
 *                     url = 'jdbc:h2:mem:primary_db;DB_CLOSE_DELAY=-1'
 *                 }
 *                 secondary {
 *                     url = 'jdbc:h2:mem:secondary_db;DB_CLOSE_DELAY=-1'
 *                 }
 *             }
 *         }
 *     }
 * }
 * </pre>
 *
 * @since 1.5.0
 * @see PersistenceExtension#validation
 * @see TenantValidationExtension
 * @see persist.jakarta.gradle.task.ValidatePersistenceSchema
 */
abstract class ValidationExtension {

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
    abstract Property<String> getUrl()

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
    abstract Property<String> getDriver()

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
    abstract Property<String> getUser()

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
    abstract Property<String> getPassword()

    /**
     * The named container of multi-tenant database connection profiles.
     * <p>
     * Each entry defines a separate tenant whose schema will be validated
     * independently during the
     * {@link persist.jakarta.gradle.task.ValidatePersistenceSchema} task.
     * When this container is empty, the base {@link #getUrl() url},
     * {@link #getDriver() driver}, {@link #getUser() user}, and
     * {@link #getPassword() password} properties are wrapped into a single
     * {@code default} profile automatically.
     * </p>
     *
     * @since 1.7.0
     * @see TenantValidationExtension
     */
    @Nested
    final NamedDomainObjectContainer<TenantValidationExtension> tenants

    /**
     * Creates a new extension instance with sensible H2 defaults and an empty
     * tenants container.
     *
     * @param objects The Gradle {@link ObjectFactory} used to create the
     *                {@link NamedDomainObjectContainer} for tenant profiles.
     */
    @Inject
    ValidationExtension(ObjectFactory objects) {
        // Retain standard monolithic convention pointers for backward compatibility
        this.url.convention("jdbc:h2:mem:schema_validate_db;DB_CLOSE_DELAY=-1")
        this.driver.convention("org.h2.Driver")
        this.user.convention("sa")
        this.password.convention("")

        // Initialize the new managed domain container
        this.tenants = objects.domainObjectContainer(TenantValidationExtension)
    }

    /**
     * Configures the multi-tenant database connection profiles.
     *
     * @param action The configuration action to apply to the tenants registry.
     * @since 1.7.0
     */
    void tenants(Action<? super NamedDomainObjectContainer<TenantValidationExtension>> action) {
        action.execute(tenants)
    }

}
