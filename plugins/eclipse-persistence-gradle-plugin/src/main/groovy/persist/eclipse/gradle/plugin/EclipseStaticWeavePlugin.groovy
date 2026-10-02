package persist.eclipse.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.language.base.plugins.LifecycleBasePlugin
import persist.eclipse.gradle.task.EclipseWeaveTask
import persist.jakarta.gradle.plugin.JakartaPersistencePlugin
import persist.jakarta.gradle.task.ProcessPersistenceDescriptor

/**
 * Main entry point for the EclipseLink Static Weaving Plugin.
 * <p>
 * This plugin registers a high-performance, incremental bytecode modification task for every
 * discovered source set container in the project. It programmatically triggers EclipseLink's
 * <code>StaticWeave</code> execution framework post-compilation, transforming standard JPA
 * entities in-place within the compilation pipeline output boundaries.
 * </p>
 * <p>
 * This enables performance optimizations such as strict lazy loading, fetch graph mechanics,
 * and advanced inline dirty tracking without requiring an active <code>-javaagent</code> JVM argument at runtime.
 * </p>
 *
 * @see EclipseWeaveTask
 */
class EclipseStaticWeavePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(JakartaPersistencePlugin)

        def weaveConfig = project.configurations.maybeCreate('weave')
        project.configurations.named('jpa').configure {
            weaveConfig.extendsFrom(it)
        }

        project.dependencies.add('weave', 'org.eclipse.persistence:org.eclipse.persistence.jpa')

        project.extensions.getByType(SourceSetContainer).configureEach { sourceSet ->
            // Dynamic task naming based on the source set context (e.g., main -> compileJava)
            String classesTaskName = sourceSet.getClassesTaskName()
            String compileTaskName = sourceSet.getCompileJavaTaskName()
            String weaveTaskName = "eclipseWeave${classesTaskName.capitalize()}"
            String processPersistenceDescriptorTaskName = sourceSet.name == 'main' ? "processPersistenceDescriptor" : "process${sourceSet.name.capitalize()}PersistenceDescriptor"

            // Fetch the compileJava task provider safely
            def compileJavaProvider = project.tasks.named(compileTaskName, JavaCompile)

            // Locate the descriptor generation task registered by our core plugin
            def processDescriptorTaskProvider = project.tasks.named(processPersistenceDescriptorTaskName, ProcessPersistenceDescriptor)

            // Map to an explicitly isolated woven directory
            def wovenClassesDir = project.layout.buildDirectory.dir("woven/classes/java/${sourceSet.name}")

            // Identify the root directory containing our dynamically generated META-INF folder
            def generatedResourcesRootDir = project.layout.buildDirectory.dir("generated/resources/${sourceSet.name}")

            // Natively register the generated directory as a resource path inside the source set
            // This ensures Gradle and IDEs index your dynamic persistence.xml perfectly.
            sourceSet.resources.srcDir(generatedResourcesRootDir)

            // Register your weaving task at the PROJECT level (Fixed Context!)
            def weaveTaskProvider = project.tasks.register(weaveTaskName, EclipseWeaveTask) { task ->
                task.group = LifecycleBasePlugin.BUILD_GROUP
                task.description = "Performs EclipseLink static weaving of entity ${classesTaskName}"

                // Source comes directly from the compile output directory
                task.sourceClassesDir.set(compileJavaProvider.flatMap { it.destinationDirectory })

                // Target goes to a brand new isolated directory
                task.targetClassesDir.set(wovenClassesDir)

                // Pass the ROOT generated resources directory where META-INF/persistence.xml lives
                // This gives EclipseLink the exact path layout structure it expects to find.
                task.resourcesDir.set(generatedResourcesRootDir)

                // Wire the descriptor task output directly as an input to the weaving task!
                // This instantly establishes an implicit task dependency graph tracking rule.
                task.persistenceXml.set(processDescriptorTaskProvider.flatMap { it.destinationFile })

                // Wire the classpaths safely using lazy FileCollections
                task.weaveClasspath.from(weaveConfig)
                task.compileClasspath.from(sourceSet.compileClasspath)

                // Explicitly dictate that weaving runs after compilation
                task.mustRunAfter(compileJavaProvider)
            }

            // Safely feed both directories to the jar task and prioritize woven outputs
            project.tasks.withType(Jar).configureEach { jarTask ->

                // 1. Tell Gradle how to resolve duplicate file conflicts
                jarTask.duplicatesStrategy = DuplicatesStrategy.EXCLUDE

                // 2. Add the WOVEN classes FIRST so they take precedence
                jarTask.from(weaveTaskProvider.flatMap { task -> task.getTargetClassesDir() })

                // 3. Add the ORIGINAL classes SECOND (Duplicates matching woven classes will be ignored)
                jarTask.from(compileJavaProvider.flatMap { task -> task.destinationDirectory })
            }

            // Hook the weaving task back into the build lifecycle
            project.tasks.named(classesTaskName) { classesTask ->
                classesTask.dependsOn(weaveTaskProvider)
            }
        }
    }

}
