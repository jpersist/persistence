package persist.hibernate.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.language.base.plugins.LifecycleBasePlugin
import persist.hibernate.gradle.extension.HibernateExtension
import persist.hibernate.gradle.task.HibernateEnhancementTask

/**
 * Main entry point for the Hibernate Bytecode Enhancement Plugin.
 * <p>
 * This plugin registers a customizable <code>hibernate</code> configuration extension block and
 * dynamically instantiates an independent {@link HibernateEnhancementTask}
 * for every active project source set layout.
 * </p>
 * <p>
 * To ensure absolute compliance with the Gradle Configuration Cache and incremental verification states,
 * this plugin manages separate input/output staging directory targets, safeguarding compilation
 * <code>UP-TO-DATE</code> checking parameters across multi-module build paths.
 * </p>
 *
 * @see HibernateExtension
 */class HibernateEnhancementPlugin implements Plugin<Project> {

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
            String classesTaskName = sourceSet.getClassesTaskName()
            String compileTaskName = sourceSet.getCompileJavaTaskName()
            String enhanceTaskName = "hibernateEnhance${classesTaskName.capitalize()}"
            def enhancement = hibernate.enhancement

            // Fetch the compilation task lazily using provider mappings to prevent premature instantiation
            def compileTaskProvider = project.tasks.named(compileTaskName, JavaCompile)

            // Map to an explicitly isolated enhanced directory
            def enhancedClassesDir = project.layout.buildDirectory.dir("classes/java/enhanced/${sourceSet.getName()}")

            // Register the task completely outside the compile task configuration execution context
            def enhanceTaskProvider = project.tasks.register(enhanceTaskName, HibernateEnhancementTask) { enhanceTask ->
                enhanceTask.group = LifecycleBasePlugin.BUILD_GROUP
                enhanceTask.description = "Enhances entity ${classesTaskName}."

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

                // Hook the lifecycle dependencies safely
                enhanceTask.mustRunAfter(compileTaskProvider)
            }

            project.tasks.named(classesTaskName).configure { task ->
                task.dependsOn(enhanceTaskProvider)
            }

            // Re-route the packaging steps to collect classes from the enhanced directory
            project.tasks.withType(Jar).configureEach { jarTask ->
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
