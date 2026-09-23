package persist.hibernate.gradle.extension

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory

import javax.inject.Inject

class HibernateExtension {
    EnhancementExtension enhancement

    @Inject
    HibernateExtension(ObjectFactory objects) {
        this.enhancement = objects.newInstance(EnhancementExtension)
    }

    void enhancement(Action<? super EnhancementExtension> action) {
        action.execute(this.enhancement)
    }
}
