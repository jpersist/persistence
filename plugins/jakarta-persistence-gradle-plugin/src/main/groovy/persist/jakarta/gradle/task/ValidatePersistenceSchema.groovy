package persist.jakarta.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import persist.jakarta.gradle.worker.SchemaValidationWorker

import javax.inject.Inject

/**
 * Boots an isolated JVM worker process to validate that the JPA entity configurations
 * perfectly match the generated schema layout using an in-memory database baseline.
 *
 * @since 1.5.0
 */
@DisableCachingByDefault(because = "Validates active database schema state at execution time")
abstract class ValidatePersistenceSchema extends DefaultTask {

    @Input
    abstract Property<String> getPersistenceUnitNames()

    /**
     * The built-in resource destination directory containing the META-INF/persistence.xml structure.
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getResourcesDir()

    /**
     * The complete runtime and compilation classpath containing the compiled entity classes,
     * the JPA provider implementation binaries (Hibernate/EclipseLink), and the H2 driver.
     */
    @CompileClasspath
    abstract ConfigurableFileCollection getClasspath()

    private final WorkerExecutor workerExecutor

    @Inject
    ValidatePersistenceSchema(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void validate() {
        // FIX: Unpack the string primitive inside @TaskAction
        // This cuts off the entire Gradle object snapshotting graph chain!
        def unitNames = persistenceUnitNames.get()
        def classpathFiles = classpath.files
        def resourcesDirAsFile = resourcesDir.get().asFile

        // Submit the action with isolated ClassLoader bindings managed natively by Gradle
        workerExecutor.classLoaderIsolation { workerSpec ->
            // Feed the project classpath, the dynamic resources root folder, and H2 elements
            workerSpec.classpath.from(classpathFiles)
            workerSpec.classpath.from(resourcesDirAsFile)
        }.submit(SchemaValidationWorker) { params ->
            params.persistenceUnitNames.set(unitNames)
        }
    }

}
