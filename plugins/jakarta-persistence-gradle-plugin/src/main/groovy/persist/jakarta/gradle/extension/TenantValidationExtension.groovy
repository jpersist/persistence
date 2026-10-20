package persist.jakarta.gradle.extension

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

import javax.inject.Inject

/**
 * Named DSL extension representing a single database tenant profile for
 * multi-tenant schema validation.
 * <p>
 * Each instance is identified by a unique {@link #getName() name} and carries
 * its own JDBC connection properties. Instances are created inside the
 * {@link ValidationExtension#tenants} named container.
 * </p>
 * <p>
 * All JDBC properties default to an in-memory H2 database whose name is
 * derived from the tenant name.
 * </p>
 *
 * @since 1.7.0
 * @see ValidationExtension#tenants
 */
abstract class TenantValidationExtension {

    private final String name

    /**
     * Creates a new tenant profile with sensible H2 defaults derived from the
     * given tenant name.
     *
     * @param name The unique identifier for this tenant profile.
     */
    @Inject
    TenantValidationExtension(String name) {
        this.name = name
        // Convention parameters fallback to standard safe H2 defaults natively
        this.url.convention("jdbc:h2:mem:${name}_validate_db;DB_CLOSE_DELAY=-1")
        this.driver.convention("org.h2.Driver")
        this.user.convention("sa")
        this.password.convention("")
    }

    /**
     * The unique identifier for this tenant profile.
     *
     * @return The tenant name.
     */
    @Input
    String getName() {
        return this.name
    }

    /**
     * The JDBC connection URL for this tenant.
     * <p>Defaults to {@code "jdbc:h2:mem:<name>_validate_db;DB_CLOSE_DELAY=-1"}.</p>
     *
     * @return The lazy property tracking the JDBC URL.
     */
    @Input
    abstract Property<String> getUrl()

    /**
     * The fully qualified JDBC driver class name for this tenant.
     * <p>Defaults to {@code "org.h2.Driver"}.</p>
     *
     * @return The lazy property tracking the driver class name.
     */
    @Input
    abstract Property<String> getDriver()

    /**
     * The database user name for this tenant.
     * <p>Defaults to {@code "sa"}.</p>
     *
     * @return The lazy property tracking the user name.
     */
    @Input
    abstract Property<String> getUser()

    /**
     * The database password for this tenant.
     * <p>Defaults to an empty string.</p>
     *
     * @return The lazy property tracking the password.
     */
    @Input
    abstract Property<String> getPassword()

}
