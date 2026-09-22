package persist.eclipse.gradle.extension

import org.gradle.api.Action
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory

import javax.inject.Inject

abstract class JpaModelgenExtension {

    RegularFileProperty persistenceXml

    private final ProjectLayout layout

    @Inject
    JpaModelgenExtension(ObjectFactory objects, ProjectLayout layout) {
        this.layout = layout
        this.persistenceXml = objects.fileProperty()
    }

    void setPersistenceXml(String path) {
        persistenceXml.set(layout.projectDirectory.file(path))
    }

}

abstract class EclipselinkExtension {

    JpaModelgenExtension jpaModelgen

    @Inject
    EclipselinkExtension(ObjectFactory objects) {
        this.jpaModelgen = objects.newInstance(JpaModelgenExtension)
    }

    void jpaModelgen(Action<? super JpaModelgenExtension> action) {
        action.execute(jpaModelgen)
    }

}
