package persist.jakarta.gradle.extension

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

/**
 * Named DSL extension for managing the static compile-time query verification linter.
 *
 * @since 1.6.0
 */
interface LinterExtension {

    /**
     * Toggles whether the static @NamedQuery analysis task should execute.
     * <p>Defaults to {@code true}.</p>
     *
     * @return The lazy property tracking the linter activation state.
     */
    @Input
    Property<Boolean> getEnabled()

    /**
     * Toggles whether discovering a query syntax error or metamodel mismatch
     * should immediately fail the Gradle build execution pass.
     * <p>Defaults to {@code true}. When set to {@code false}, errors are logged as warnings.</p>
     *
     * @return The lazy property tracking the build failure strategy on errors.
     */
    @Input
    Property<Boolean> getFailOnError()

    /**
     * Toggles whether sub-critical semantic validation warnings should be muted.
     * <p>Defaults to {@code false}.</p>
     *
     * @return The lazy property tracking the warning suppression strategy.
     */
    @Input
    Property<Boolean> getIgnoreWarnings()

}
