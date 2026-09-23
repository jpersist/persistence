package persist.hibernate.gradle.extension

import org.gradle.api.provider.Property

interface EnhancementExtension {
    Property<Boolean> getEnableLazyInitialization()
    Property<Boolean> getEnableDirtyTracking()
    Property<Boolean> getEnableAssociationManagement()
    Property<Boolean> getEnableExtendedEnhancement()
}
