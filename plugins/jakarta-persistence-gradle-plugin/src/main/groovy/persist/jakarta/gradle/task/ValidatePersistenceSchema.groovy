package persist.jakarta.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
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

    /**
     * The JPA specification version used to determine the annotation namespace
     * ({@code jakarta.persistence} vs {@code javax.persistence}) during bytecode scanning.
     * <p>Defaults to {@code "3.0"}.</p>
     *
     * @return The lazy property tracking the JPA version string.
     */
    @Input
    abstract Property<String> getJpaVersion()

    /**
     * A comma-separated list of persistence unit names to validate.
     * <p>
     * Each name must correspond to a {@code <persistence-unit>} declared
     * in the project's {@code persistence.xml}.
     * </p>
     *
     * @return The lazy property tracking the comma-separated unit names.
     */
    @Input
    abstract Property<String> getPersistenceUnitNames()

    /**
     * Nested database connection configuration used during schema validation.
     *
     * @return The lazy property wrapping the {@link ValidationExtension}.
     * @see ValidationExtension
     */
    @Nested
    abstract Property<ValidationExtension> getValidation()

    /**
     * The serialized list of tenant database connection profiles to validate
     * against. Each entry is a pipe-delimited string of the form
     * {@code Name|URL|Driver|User|Password}.
     * <p>
     * When no explicit tenants are configured in the
     * {@link ValidationExtension#tenants} container, a single {@code default}
     * profile is synthesized from the base validation properties.
     * </p>
     *
     * @return The lazy list property tracking the serialized tenant profiles.
     * @since 1.7.0
     */
    @Input
    abstract ListProperty<String> getTenantProfiles()

    /**
     * The list of fully qualified entity class names discovered by the ASM
     * bytecode scanner during the compile phase.
     * <p>
     * These names are forwarded to the {@link SchemaValidationWorker} so the
     * SPI-loaded {@link persist.lint.api.spi.SchemaValidator} can resolve the
     * entity mappings without scanning the classpath itself.
     * </p>
     *
     * @return The lazy list property tracking the discovered entity class names.
     * @since 1.7.0
     */
    @Input
    abstract ListProperty<String> getDiscoveredEntityClasses()

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

    /**
     * Creates a new task instance.
     *
     * @param workerExecutor The Gradle {@link WorkerExecutor} used to submit
     *                       the isolated schema validation work action.
     */
    @Inject
    ValidatePersistenceSchema(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    /**
     * Executes schema validation by submitting a
     * {@link SchemaValidationWorker} to an isolated classloader worker process.
     * <p>
     * The worker receives the persistence unit names, the serialized
     * multi-tenant connection profiles, the discovered entity class names,
     * and the full runtime classpath. For each tenant profile, every active
     * persistence unit is validated via the SPI-loaded
     * {@link persist.lint.api.spi.SchemaValidator}.
     * </p>
     */
    @TaskAction
    void validate() {
        def version = jpaVersion.get()
        def unitNames = persistenceUnitNames.get()
        def classpathFiles = classpath.files
        def resourcesFolder = resourcesDir.get().asFile
        def entityClasses = discoveredEntityClasses.get()

        // Extract credentials lazily, fallback safely to standard H2 constants if absent
        def extension = validation.get()

        // Serialize tenant configurations (Name|URL|Driver|User|Password) into a safe transport list
        List<String> profiles = []
        extension.tenants.each { tenant ->
            profiles.add("${tenant.name}|${tenant.url.get()}|${tenant.driver.get()}|${tenant.user.get()}|${tenant.password.get()}".toString())
        }

        // Backward compatibility fallback: if no custom tenants are declared, wrap the base configuration
        if (profiles.isEmpty()) {
            profiles.add("default|${extension.url.get()}|${extension.driver.get()}|${extension.user.get()}|${extension.password.get()}".toString())
        }

        // Submit the action with isolated ClassLoader bindings managed natively by Gradle
        workerExecutor.classLoaderIsolation { workerSpec ->
            // Feed the project classpath, the dynamic resources root folder, and H2 elements
            workerSpec.classpath.from(classpathFiles)
            workerSpec.classpath.from(resourcesFolder)
        }.submit(SchemaValidationWorker) { params ->
            params.jpaVersion.set(version)
            params.persistenceUnitNames.set(unitNames)

            // Pass the serialized matrix string array to the validation worker parameters queue
            params.tenantProfiles.set(profiles)
            // Forward the entity class name tokens safely to the isolated worker container
            params.discoveredEntityClasses.set(entityClasses)
        }
    }

}
