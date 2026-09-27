package persist.jakarta.gradle.extension

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import javax.inject.Inject

abstract class PersistenceUnitExtension {

    private final String name

    @Inject
    PersistenceUnitExtension(String name) {
        this.name = name
        // Set sane defaults aligning with JPA specification standards
        this.transactionType.convention("RESOURCE_LOCAL")
        this.excludedUnlistedClasses.convention(false)
        this.includeAllClasses.convention(false)
        this.sharedCacheMode.convention(false)
        this.validationMode.convention("AUTO")
    }

    // Name is read-only as it acts as the key for the NamedDomainObjectContainer
    @Input
    String getName() {
        return this.name
    }

    @Input
    abstract Property<String> getTransactionType()

    @Input
    @Optional
    abstract Property<String> getDescription()

    @Input
    @Optional
    abstract Property<String> getProvider()

    @Input
    @Optional
    abstract Property<String> getDataSource()

    @Input
    @Optional
    abstract Property<Boolean> getJta()

    @Input
    @Optional
    abstract ListProperty<String> getMappingFiles()

    @Input
    abstract Property<Boolean> getExcludedUnlistedClasses()

    @Input
    abstract Property<Boolean> getIncludeAllClasses()

    @Input
    abstract Property<Boolean> getSharedCacheMode()

    @Input
    abstract Property<String> getValidationMode()

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
