package persist.openjpa.gradle.extension

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Configuration options for the OpenJPA static canonical metamodel generation processor.
 * <p>
 * This interface exposes lazy {@link Property} attributes that control how the OpenJPA
 * annotation processor generates JPA static metamodel classes (e.g., {@code Entity_})
 * from annotated entity sources during the compilation phase.
 * </p>
 * <p>
 * Example configuration block inside a build script:
 * <pre>
 * openjpa {
 *     main {
 *         metamodel {
 *             enabled = true
 *             logLevel = 'TRACE'
 *             source = 8
 *         }
 *     }
 * }
 * </pre>
 * </p>
 *
 * @since 1.8.0
 */
interface MetaModelExtension {

    /**
     * Toggles whether the OpenJPA metamodel annotation processor is active.
     * <p>
     * When set to {@code true}, the processor generates static canonical metamodel classes
     * for all discovered JPA entity types during compilation.
     * Defaults to {@code true}.
     * </p>
     *
     * @return A lazy property controlling metamodel generation activation.
     */
    @Optional
    @Input
    Property<Boolean> getEnabled()

    /**
     * Configures the logging verbosity level used by the OpenJPA annotation processor.
     * <p>
     * Accepts one of the {@link LogLevel} enumeration constants to control the amount
     * of diagnostic output emitted during metamodel generation.
     * </p>
     *
     * @return A lazy property controlling the annotation processor log level.
     */
    @Optional
    @Input
    Property<LogLevel> getLogLevel()

    /**
     * Configures the Java source compatibility version for the generated metamodel classes.
     * <p>
     * This value is forwarded as the {@code -Aopenjpa.source} compiler argument to ensure
     * the generated source files comply with the specified language level.
     * </p>
     *
     * @return A lazy property controlling the source compatibility version.
     */
    @Optional
    @Input
    Property<Integer> getSource()

    /**
     * Configures a custom naming policy for the generated metamodel class identifiers.
     * <p>
     * This value is forwarded as the {@code -Aopenjpa.naming} compiler argument to override
     * the default naming strategy used by the annotation processor.
     * </p>
     *
     * @return A lazy property controlling the metamodel naming convention.
     */
    @Optional
    @Input
    Property<String> getNaming()

    /**
     * Configures a custom header comment to prepend to the generated metamodel source files.
     * <p>
     * This value is forwarded as the {@code -Aopenjpa.header} compiler argument.
     * </p>
     *
     * @return A lazy property controlling the generated file header content.
     */
    @Optional
    @Input
    Property<String> getHeader()

    /**
     * Configures whether the {@code @Generated} annotation should be added to
     * the generated metamodel classes.
     * <p>
     * This value is forwarded as the {@code -Aopenjpa.addGeneratedAnnotation} compiler argument.
     * </p>
     *
     * @return A lazy property controlling the {@code @Generated} annotation inclusion.
     */
    @Optional
    @Input
    Property<String> getAddGeneratedAnnotation()

    /**
     * Enumeration of supported logging verbosity levels for the OpenJPA annotation processor.
     *
     * @since 1.8.0
     */
    enum LogLevel {

        /** Finest-grained diagnostic tracing output. */
        TRACE,

        /** Standard informational messages. */
        INFO,

        /** Warning messages indicating potential issues. */
        WARN,

        /** Error messages indicating processing failures. */
        ERROR

    }

}
