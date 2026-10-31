package persist.openjpa.gradle.plugin

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import persist.openjpa.gradle.extension.OpenJPAExtension

abstract class OpenJPABasePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)

        NamedDomainObjectContainer<OpenJPAExtension> container = project.extensions.findByName('openjpa') as NamedDomainObjectContainer<OpenJPAExtension>
        if (container == null) {
            container = project.objects.domainObjectContainer(OpenJPAExtension)
            project.extensions.add('openjpa', container)
        }
    }

}
