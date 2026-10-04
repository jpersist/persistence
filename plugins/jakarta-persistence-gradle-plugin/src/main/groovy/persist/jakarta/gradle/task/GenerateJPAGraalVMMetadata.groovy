package persist.jakarta.gradle.task

import groovy.json.JsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Generates a GraalVM native image {@code reflect-config.json} file
 * containing all discovered JPA entity metadata classes to support AOT reflection.
 *
 * @since 1.5.0
 */
@DisableCachingByDefault(because = "Writes out structural JSON configuration file mapping directly")
abstract class GenerateJPAGraalVMMetadata extends DefaultTask {

    /**
     * The compiled, ASM-discovered fully qualified class names from the source set.
     */
    @Input
    abstract ListProperty<String> getIncludedClasses()

    /**
     * The location where the final reflect-config.json will be written.
     */
    @OutputFile
    abstract RegularFileProperty getOutputFile()

    @TaskAction
    void generate() {
        List<String> classes = includedClasses.getOrElse([])
        File targetFile = outputFile.get().asFile

        // Ensure parent directories exist safely
        targetFile.parentFile.mkdirs()

        // Build the GraalVM structural JSON data payload array
        List<Map<String, ?>> jsonPayload = classes.collect { className ->
            [
                "name"                   : className,
                "allDeclaredConstructors": true,
                "allDeclaredFields"      : true,
                "allDeclaredMethods"     : true
            ]
        }

        // Serialize the JSON elegantly with standard line-breaks formatting
        String jsonOutput = new JsonBuilder(jsonPayload).toPrettyString()

        targetFile.withWriter("UTF-8") { writer ->
            writer.write(jsonOutput)
        }
    }

}
