package persist.eclipse.gradle.plugin

import persist.eclipse.gradle.task.EclipseWeaveTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile

class EclipseStaticWeavePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply('java')

        def weaveConfig = project.configurations.register('weave')

        project.dependencies.add('weave', 'jakarta.persistence:jakarta.persistence-api')
        project.dependencies.add('weave', 'org.eclipse.persistence:org.eclipse.persistence.jpa')

        def compileClasspathConfig = project.configurations.named('compileClasspath')

        // Fetch the compileJava task provider safely
        def compileJavaProvider = project.tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME, JavaCompile)

        // Register your weaving task at the PROJECT level (Fixed Context!)
        def weaveTaskProvider = project.tasks.register('weaveEntityClasses', EclipseWeaveTask) { task ->
            task.description = 'Performs EclipseLink static weaving of entity classes'
            task.group = 'build'

            // Source comes directly from the compile output directory
            task.getSourceClassesDir().set(compileJavaProvider.flatMap { it.destinationDirectory })

            // Target goes to a brand new isolated directory
            task.getTargetClassesDir().set(project.layout.buildDirectory.dir('classes/java/woven'))

            // Wire the resources directory using layout properties
            task.getResourcesDir().set(project.layout.projectDirectory.dir('src/main/resources'))

            // Wire the classpaths safely using lazy FileCollections
            task.getWeaveClasspath().from(weaveConfig)
            task.getCompileClasspath().from(compileClasspathConfig)

            // Explicitly dictate that weaving runs after compilation
            task.mustRunAfter(compileJavaProvider)
        }

        // Safely feed both directories to the jar task and prioritize woven outputs
        project.tasks.named(JavaPlugin.JAR_TASK_NAME, Jar) { jarTask ->

            // 1. Tell Gradle how to resolve duplicate file conflicts
            jarTask.duplicatesStrategy = DuplicatesStrategy.EXCLUDE

            // 2. Add the WOVEN classes FIRST so they take precedence
            jarTask.from(weaveTaskProvider.flatMap { task -> task.getTargetClassesDir() })

            // 3. Add the ORIGINAL classes SECOND (Duplicates matching woven classes will be ignored)
            jarTask.from(compileJavaProvider.flatMap { task -> task.destinationDirectory })
        }

        // Hook the weaving task back into the build lifecycle
        project.tasks.named(JavaPlugin.CLASSES_TASK_NAME) { classesTask ->
            classesTask.dependsOn(weaveTaskProvider)
        }
    }

}
