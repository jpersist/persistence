package persist.jakarta.gradle.extension

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

import javax.inject.Inject

abstract class PersistenceExtension {

    // Lazy property for future JPA specification versions
    abstract Property<String> getVersion()

    // Container for dynamic persistence-unit naming blocks
    final NamedDomainObjectContainer<PersistenceUnitExtension> persistenceUnits

    @Inject
    PersistenceExtension(ObjectFactory objects) {
        this.persistenceUnits = objects.domainObjectContainer(PersistenceUnitExtension)
        // Default to spec version 3.0
        this.version.convention("3.0")
    }

    // Configurer block to allow standard nested closure notation
    void persistenceUnits(Action<? super NamedDomainObjectContainer<PersistenceUnitExtension>> action) {
        action.execute(persistenceUnits)
    }

}
