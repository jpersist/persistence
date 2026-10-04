package persist.jakarta.gradle.extension

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

interface ValidationExtension {

    @Input
    @Optional
    Property<String> getUrl()

    @Input
    @Optional
    Property<String> getDriver()

    @Input
    @Optional
    Property<String> getUser()

    @Input
    @Optional
    Property<String> getPassword()

}
