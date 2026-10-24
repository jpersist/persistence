package persist.openjpa.gradle.extension

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Configuration options for OpenJPA's compile-time bytecode enhancement engine.
 * <p>
 * This interface exposes lazy {@link Property} flags that control how entity classes are
 * modified post-compilation by the OpenJPA {@code PCEnhancer} tool. Bytecode enhancement
 * at compile time allows OpenJPA to inject persistence capability hooks directly into
 * entity binaries, enabling transparent state management without runtime proxies.
 * </p>
 * <p>
 * Example configuration block inside a build script:
 * <pre>
 * openjpa {
 *     main {
 *         enhancement {
 *             addDefaultConstructor = true
 *             enforcePropertyRestrictions = false
 *         }
 *     }
 * }
 * </pre>
 * </p>
 *
 * @since 1.8.0
 */
interface EnhancementExtension {

    /**
     * Toggles whether the OpenJPA enhancer should add a default no-argument constructor
     * to entity classes that lack one.
     * <p>
     * When set to {@code true}, the enhancer automatically inserts a protected no-arg
     * constructor into any entity class that does not already declare one, satisfying
     * the JPA specification requirement for entity instantiation.
     * Defaults to {@code true}.
     * </p>
     *
     * @return A lazy property controlling default constructor generation during enhancement.
     */
    @Optional
    @Input
    Property<Boolean> getAddDefaultConstructor()

    /**
     * Toggles whether the OpenJPA enhancer should enforce property access restrictions
     * on persistent entity fields.
     * <p>
     * When set to {@code true}, the enhancer validates that persistent properties conform
     * to the JavaBean access pattern conventions expected by the OpenJPA runtime engine.
     * Defaults to {@code false}.
     * </p>
     *
     * @return A lazy property controlling property restriction enforcement during enhancement.
     */
    @Optional
    @Input
    Property<Boolean> getEnforcePropertyRestrictions()

}
