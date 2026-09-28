package persist.jakarta.gradle.extension

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import javax.inject.Inject

/**
 * Per-unit DSL configuration for a single {@code <persistence-unit>} element.
 * <p>
 * Instances are created automatically by the
 * {@link org.gradle.api.NamedDomainObjectContainer} in
 * {@link PersistenceExtension#persistenceUnits}. The container entry name
 * becomes the {@code name} attribute of the persistence unit.
 * </p>
 * <p>
 * Declared as {@code abstract} so that Gradle's managed-property engine can
 * generate the backing implementation for lazy
 * {@link org.gradle.api.provider.Property} fields.
 * </p>
 *
 * @since 1.1.0
 */
abstract class PersistenceUnitExtension {

    private final String name

    /**
     * Creates a new persistence-unit configuration with the given name.
     *
     * @param name The persistence-unit name used as the container key and the
     *             {@code name} attribute in the generated XML.
     */
    @Inject
    PersistenceUnitExtension(String name) {
        this.name = name
        // Set sane defaults aligning with JPA specification standards
        this.transactionType.convention("RESOURCE_LOCAL")
        this.excludedUnlistedClasses.convention(false)
        this.includeAllClasses.convention(true)
        this.sharedCacheMode.convention(false)
        this.validationMode.convention("AUTO")
    }

    /**
     * Returns the persistence-unit name.
     * <p>
     * Read-only because it serves as the key for the
     * {@link org.gradle.api.NamedDomainObjectContainer}.
     * </p>
     *
     * @return The unit name.
     */
    @Input
    String getName() {
        return this.name
    }

    /**
     * The JPA transaction type ({@code RESOURCE_LOCAL} or {@code JTA}).
     * <p>
     * Defaults to {@code "RESOURCE_LOCAL"}.
     * </p>
     *
     * @return The lazy property tracking the transaction type.
     */
    @Input
    abstract Property<String> getTransactionType()

    /**
     * An optional human-readable description for the persistence unit.
     *
     * @return The lazy property tracking the description.
     */
    @Input
    @Optional
    abstract Property<String> getDescription()

    /**
     * The fully qualified class name of the JPA persistence provider.
     *
     * @return The lazy property tracking the provider class name.
     */
    @Input
    @Optional
    abstract Property<String> getProvider()

    /**
     * The JNDI name of the data source for this persistence unit.
     * <p>
     * Whether the data source is written as {@code <jta-data-source>} or
     * {@code <non-jta-data-source>} depends on the {@link #getJta()} flag.
     * </p>
     *
     * @return The lazy property tracking the data source JNDI name.
     */
    @Input
    @Optional
    abstract Property<String> getDataSource()

    /**
     * Whether the {@link #getDataSource() data source} is JTA-managed.
     * <p>
     * When {@code true}, the data source is emitted as
     * {@code <jta-data-source>}; otherwise as {@code <non-jta-data-source>}.
     * Defaults to {@code false}.
     * </p>
     *
     * @return The lazy property tracking the JTA flag.
     */
    @Input
    @Optional
    abstract Property<Boolean> getJta()

    /**
     * A list of ORM mapping file paths to include in the persistence unit.
     *
     * @return The lazy list property tracking the mapping file paths.
     */
    @Input
    @Optional
    abstract ListProperty<String> getMappingFiles()

    /**
     * Whether unlisted entity classes should be excluded from the persistence unit.
     * <p>
     * Defaults to {@code false}.
     * </p>
     *
     * @return The lazy property tracking the exclude-unlisted-classes flag.
     */
    @Input
    abstract Property<Boolean> getExcludedUnlistedClasses()

    /**
     * Whether all discovered entity classes should be included.
     * <p>
     * Defaults to {@code false}.
     * </p>
     *
     * @return The lazy property tracking the include-all-classes flag.
     */
    @Input
    abstract Property<Boolean> getIncludeAllClasses()

    /**
     * Whether the shared (second-level) cache is enabled.
     * <p>
     * Defaults to {@code false}.
     * </p>
     *
     * @return The lazy property tracking the shared-cache-mode flag.
     */
    @Input
    abstract Property<Boolean> getSharedCacheMode()

    /**
     * The Bean Validation mode ({@code AUTO}, {@code CALLBACK}, or {@code NONE}).
     * <p>
     * Defaults to {@code "AUTO"}.
     * </p>
     *
     * @return The lazy property tracking the validation mode.
     */
    @Input
    abstract Property<String> getValidationMode()

    /**
     * Vendor-specific JPA properties to include in the
     * {@code <properties>} block of the persistence unit.
     *
     * @return The lazy map property tracking the JPA properties.
     */
    @Input
    abstract MapProperty<String, String> getProperties()

    /**
     * DSL Helper to add a single property:
     * property 'eclipselink.weaving', 'static'
     */
    void property(String key, String value) {
        this.properties.put(key, value)
    }

    /**
     * DSL Helper to configure a block of properties:
     * properties {
     *     property 'eclipselink.weaving', 'static'
     * }
     */
    void properties(Closure<?> closure) {
        closure.setDelegate(this)
        closure.setResolveStrategy(Closure.DELEGATE_FIRST)
        closure.call()
    }
}
