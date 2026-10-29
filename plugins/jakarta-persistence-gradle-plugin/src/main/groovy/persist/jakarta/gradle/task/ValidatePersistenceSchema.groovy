package persist.jakarta.gradle.task

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
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
     * The input source files collection used to calculate incremental deltas
     * immune to compiler erasures.
     *
     * @return The configurable file collection of tracked source files.
     */
    @Incremental
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getSourceFiles()

    /**
     * The built-in resource destination directory containing the META-INF/persistence.xml structure.
     *
     * @return The lazy directory property tracking the processed resources directory.
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getResourcesDir()

    /**
     * The generated JSON schema validation audit metrics report.
     * Acts as the mandatory {@code @OutputFile} checkpoint to satisfy Gradle's
     * incremental task contract.
     *
     * @return The lazy file property tracking the validation report output location.
     */
    @OutputFile
    abstract RegularFileProperty getValidationReportFile()

    /**
     * The complete runtime and compilation classpath containing the compiled entity classes,
     * the JPA provider implementation binaries (Hibernate/EclipseLink), and the H2 driver.
     *
     * @return The file collection managing the schema validation runtime classpath.
     */
    @CompileClasspath
    abstract ConfigurableFileCollection getClasspath()

    /** The Gradle worker executor used to submit isolated schema validation work actions. */
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
     * When executed incrementally, only entities whose source files have been
     * added or modified are forwarded to the worker. If no tracked entity
     * source files have changed, the task skips database bootstrapping entirely.
     * A non-incremental (full) pass validates every discovered entity class.
     * </p>
     * <p>
     * The worker receives the persistence unit names, the serialized
     * multi-tenant connection profiles, the discovered entity class names,
     * and the full runtime classpath. For each tenant profile, every active
     * persistence unit is validated via the SPI-loaded
     * {@link persist.lint.api.spi.SchemaValidator}.
     * </p>
     *
     * @param inputChanges The Gradle incremental input changes descriptor.
     */
    @TaskAction
    void validate(InputChanges inputChanges) {
        def version = jpaVersion.get()
        def unitNames = persistenceUnitNames.get()
        def classpathFiles = classpath.files
        def resourcesFolder = resourcesDir.get().asFile
        def fullEntityClasses = discoveredEntityClasses.get()
        def isIncrementalPass = inputChanges.incremental

        List<String> affectedEntities = []
        List<String> removedEntities = []

        if (isIncrementalPass) {
            // Track source adjustments lazily from the immutable source directories stream
            inputChanges.getFileChanges(getSourceFiles()).each { change ->
                if (change.file.name.endsWith('.java')) {
                    String relativePath = change.normalizedPath
                    // Translate source file path (com/example/model/User.java) to binary class name
                    String className = relativePath.substring(0, relativePath.lastIndexOf('.java')).replace('/', '.')

                    if (change.changeType == ChangeType.REMOVED) {
                        removedEntities.add(className)
                    } else if (fullEntityClasses.contains(className)) {
                        affectedEntities.add(className)
                    }
                }
            }

            // If an incremental step registers no active entity modifications,
            // the task can gracefully skip heavy database bootstrapping entirely.
            if (affectedEntities.isEmpty() && removedEntities.isEmpty()) {
                logger.lifecycle("✅ JPersist Schema Validation up-to-date. No tracked entity file modifications detected.")
                return
            }
        } else {
            // Full pass execution flow: process every discovered entity class mapping natively
            affectedEntities.addAll(fullEntityClasses)
        }

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

        logger.lifecycle("⏳ Starting JPersist Multi-Tenant Schema Validation pipeline pass...")
        logger.lifecycle("🔍 Auditing ${affectedEntities.size()} active entities across ${profiles.size()} tenant profiles (Incremental: ${isIncrementalPass}).")

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
            params.discoveredEntityClasses.set(affectedEntities)
        }

        // 🌟 Write execution analytics details to the target report file
        writeValidationMetricsReport(isIncrementalPass, affectedEntities.size(), fullEntityClasses.size() - affectedEntities.size())
    }

    /**
     * Writes a JSON execution metrics report summarizing the validation pass.
     *
     * @param incremental    Whether the pass was incremental.
     * @param processedCount The number of entity classes that were validated.
     * @param skippedCount   The number of entity classes that were skipped.
     */
    private void writeValidationMetricsReport(boolean incremental, int processedCount, int skippedCount) {
        Map<String, ?> reportMetadata = [
            taskName          : name,
            timestamp         : System.currentTimeMillis(),
            incremental       : incremental,
            processedEntities : processedCount,
            skippedEntities   : skippedCount,
            status            : "SUCCESS"
        ]
        File reportFile = validationReportFile.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(reportMetadata))
    }

}
