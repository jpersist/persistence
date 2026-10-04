package persist.jakarta.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import persist.jakarta.gradle.extension.ValidationExtension
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

    @Nested
    abstract Property<ValidationExtension> getValidation()

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

        // Extract credentials lazily, fallback safely to standard H2 constants if absent
        def extension = validation.get()
        def dbUrl = extension.url.getOrElse("jdbc:h2:mem:schema_validate_db;DB_CLOSE_DELAY=-1")
        def dbDriver = extension.driver.getOrElse("org.h2.Driver")
        def dbUser = extension.user.getOrElse("sa")
        def dbPassword = extension.password.getOrElse("")

        // Submit the action with isolated ClassLoader bindings managed natively by Gradle
        workerExecutor.classLoaderIsolation { workerSpec ->
            // Feed the project classpath, the dynamic resources root folder, and H2 elements
            workerSpec.classpath.from(classpathFiles)
            workerSpec.classpath.from(resourcesDirAsFile)
        }.submit(SchemaValidationWorker) { params ->
            params.persistenceUnitNames.set(unitNames)

            // Map the configured database properties straight into the Worker parameter model
            params.validationUrl.set(dbUrl)
            params.validationDriver.set(dbDriver)
            params.validationUser.set(dbUser)
            params.validationPassword.set(dbPassword)
        }
    }

}
