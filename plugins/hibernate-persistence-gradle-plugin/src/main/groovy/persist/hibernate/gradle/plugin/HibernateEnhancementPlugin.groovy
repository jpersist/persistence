package persist.hibernate.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import persist.hibernate.gradle.extension.HibernateExtension
import persist.hibernate.gradle.task.HibernateEnhancementTask

class HibernateEnhancementPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)

        def hibernate = project.extensions.create('hibernate', HibernateExtension)
        hibernate.enhancement { enhancement ->
            enhancement.enableLazyInitialization.convention(true)
            enhancement.enableDirtyTracking.convention(true)
            enhancement.enableAssociationManagement.convention(true)
            enhancement.enableExtendedEnhancement.convention(false)
        }

        project.getExtensions().getByType(SourceSetContainer).configureEach { sourceSet ->
            // Dynamic task naming based on the source set context (e.g., main -> compileJava)
            String compileTaskName = sourceSet.getCompileJavaTaskName()
            String enhanceTaskName = "hibernateEnhance${compileTaskName.capitalize()}"
            def enhancement = hibernate.enhancement

            // Fetch the compilation task lazily using provider mappings to prevent premature instantiation
            def compileTaskProvider = project.tasks.named(compileTaskName, JavaCompile)

            // Map to an explicitly isolated enhanced directory
            def enhancedClassesDir = project.layout.buildDirectory.dir("classes/java/enhanced/${sourceSet.getName()}")

            // Register the task completely outside the compile task configuration execution context
            def enhanceTaskProvider = project.tasks.register(enhanceTaskName, HibernateEnhancementTask) { enhanceTask ->
                // Pass the complete compilation classpath directly to the task input property
                enhanceTask.compileClasspath.from(sourceSet.compileClasspath)

                // Properly map input/output boundaries to maintain task correctness
                enhanceTask.sourceClassesDir.set(compileTaskProvider.flatMap { it.getDestinationDirectory() })
                enhanceTask.targetClassesDir.set(enhancedClassesDir)

                // Feed extension configuration states safely into the Task fields
                enhanceTask.lazyInitializationEnabled.set(enhancement.enableLazyInitialization)
                enhanceTask.dirtyTrackingEnabled.set(enhancement.enableDirtyTracking)
                enhanceTask.associationManagementEnabled.set(enhancement.enableAssociationManagement)
                enhanceTask.extendedEnhancementEnabled.set(enhancement.enableExtendedEnhancement)
            }

            // Hook the lifecycle dependencies safely at the top-level project scope
            compileTaskProvider.configure { compileTask ->
                compileTask.finalizedBy(enhanceTaskProvider)
            }

            // Enforce that any task packaging classes (like :jar) must wait for enhancement to finish
            // Re-route the packaging steps to collect classes from the enhanced directory
            // This guarantees both standard classes AND generated metamodel classes (Person_) are present.
            project.tasks.withType(Jar).configureEach { jarTask ->
                jarTask.mustRunAfter(enhanceTaskProvider)

                // 1. Tell Gradle how to resolve duplicate file conflicts
                jarTask.duplicatesStrategy = DuplicatesStrategy.EXCLUDE

                // 2. Add the ENHANCED classes FIRST so they take precedence
                jarTask.from(enhanceTaskProvider.flatMap { task -> task.getTargetClassesDir() })

                // 3. Add the ORIGINAL classes SECOND (Duplicates matching woven classes will be ignored)
                jarTask.from(compileTaskProvider.flatMap { task -> task.destinationDirectory })
            }
        }
    }

}
