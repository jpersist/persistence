package persist.jakarta.gradle.task

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.Incremental
import org.gradle.work.InputChanges

import javax.inject.Inject

/**
 * Generates a GraalVM native image {@code reflect-config.json} file
 * containing all discovered JPA entity metadata classes to support AOT reflection.
 *
 * @since 1.5.0
 */
@DisableCachingByDefault(because = "Writes out structural JSON configuration file mapping directly")
abstract class GenerateJPAGraalVMMetadata extends DefaultTask {

    /**
     * The input source files collection tracking active Java source code files ({@code .java}).
     * Monitored incrementally to detect additions, modifications, and removals
     * without relying on compiler output timestamps.
     *
     * @return The configurable file collection of tracked source files.
     */
    @Incremental
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getSourceFiles()

    /**
     * The compiled, ASM-discovered fully qualified class names from the source set.
     *
     * @return The lazy list property tracking the entity class names.
     */
    @Input
    abstract ListProperty<String> getIncludedClasses()

    /**
     * The location where the final {@code reflect-config.json} will be written.
     *
     * <p>This is the primary output property for the task. It replaces the
     * deprecated {@link #getOutputFile()} property.
     *
     * @return The lazy file property tracking the output file location.
     * @since 1.8.0
     */
    @OutputFile
    abstract RegularFileProperty getReflectConfigFile()

    /**
     * The location where the final {@code reflect-config.json} will be written.
     *
     * @return The lazy file property tracking the output file location.
     * @deprecated Use {@link #getReflectConfigFile()} instead. This property is
     * retained for backward compatibility and will be removed in a future release.
     */
    @Internal
    @Deprecated
    abstract RegularFileProperty getOutputFile()

    @Inject
    GenerateJPAGraalVMMetadata() {
        // 🌟 RE-ROUTING GATEWAY: Wire conventions bi-directionally to ensure absolute backward compatibility
        // 1. If 'reflectConfigFile' is set, 'outputFile' mirrors its target automatically
        getOutputFile().convention(getReflectConfigFile())

        // 2. If a legacy script overrides 'outputFile' directly, propagate that value back to the new property
        getReflectConfigFile().set(getOutputFile())
    }

    /**
     * Generates the {@code reflect-config.json} file from the discovered JPA entity classes.
     * <p>
     * When executed incrementally, only the source files that have changed since the
     * previous run are processed: removed classes are evicted from the existing JSON
     * baseline while added or modified classes are upserted. If the previous output
     * file is missing or corrupt, execution falls back to a full pass automatically.
     * </p>
     * <p>
     * Each class name is emitted as a JSON object with {@code allDeclaredConstructors},
     * {@code allDeclaredFields}, and {@code allDeclaredMethods} set to {@code true},
     * following the GraalVM native-image reflection metadata specification.
     * </p>
     *
     * @param inputChanges The Gradle incremental input changes descriptor.
     */
    @TaskAction
    void generate(InputChanges inputChanges) {
        File jsonFile = reflectConfigFile.get().asFile
        List<String> activeClasses = includedClasses.get()
        boolean isIncrementalPass = inputChanges.incremental

        // Setup baseline structural maps for GraalVM reflection contracts
        List<Map<String, Object>> existingEntries = []

        // If incremental and the file already exists, load current baseline state
        if (isIncrementalPass && jsonFile.exists() && jsonFile.length() > 0) {
            try {
                existingEntries = (List<Map<String, Object>>) new JsonSlurper().parse(jsonFile)
            } catch (Exception ignored) {
                isIncrementalPass = false // Fallback to full pass if json is corrupt
            }
        }

        List<Map<String, Object>> finalEntries = []

        if (isIncrementalPass) {
            // Re-use unchanged entries from baseline state
            finalEntries.addAll(existingEntries)

            inputChanges.getFileChanges(getSourceFiles()).each { change ->
                if (change.file.name.endsWith('.java')) {
                    String relativePath = change.normalizedPath
                    String className = relativePath.substring(0, relativePath.lastIndexOf('.java')).replace('/', '.')

                    if (change.changeType == ChangeType.REMOVED) {
                        finalEntries.removeIf { it.name == className }
                    } else if (activeClasses.contains(className)) {
                        // Evict old mapping if present before appending updated metadata
                        finalEntries.removeIf { it.name == className }
                        finalEntries.add(Helper.buildGraalVMMetadataRecord(className))
                    }
                }
            }
        } else {
            // Full pass execution path: map all active classes directly
            activeClasses.each { className ->
                finalEntries.add(Helper.buildGraalVMMetadataRecord(className))
            }
        }

        // Write the pretty-printed structured JSON metadata file back to disk
        jsonFile.parentFile.mkdirs()
        jsonFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(finalEntries))
    }

    /**
     * Internal helper providing factory methods for GraalVM reflection metadata records.
     */
    private static class Helper {

        /**
         * Builds a single GraalVM {@code reflect-config.json} entry for the given class.
         *
         * @param className The fully qualified class name.
         * @return A map representing the reflection metadata record.
         */
        private static Map<String, ?> buildGraalVMMetadataRecord(String className) {
            return [
                name: className,
                allDeclaredConstructors: true,
                allDeclaredFields: true,
                allDeclaredMethods: true
            ]
        }

    }

}
