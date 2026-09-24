package persist.hibernate.gradle.extension

import org.gradle.api.provider.Property

/**
 * Configuration options for Hibernate's programmatic compile-time bytecode enhancement engine.
 * <p>
 * This interface exposes lazy {@link Property} flags that control how entity classes are modified
 * post-compilation. By manipulating bytecode at compile time, applications gain performance benefits
 * such as field-level lazy loading and direct interceptor tracking, completely bypassing the need
 * for dynamic proxies or lazy initialization exceptions during active runtime transactions.
 * </p>
 * <p>
 * Example configuration block inside a build script:
 * <pre>
 * hibernate {
 *     enhancement {
 *         enableLazyInitialization = true
 *         enableDirtyTracking = true
 *         enableAssociationManagement = true
 *         enableExtendedEnhancement = false
 *     }
 * }
 * </pre>
 * </p>
 *
 * @since 1.0.0
 */
interface EnhancementExtension {

    /**
     * Toggles whether to enhance entity bytecode to support field-level lazy initialization.
     * <p>
     * When set to {@code true}, specific fields or relationships annotated for lazy fetching can be loaded
     * individually on-demand when accessed via their getters, instead of initializing the entire containing entity.
     * Defaults to {@code true}.
     * </p>
     *
     * @return A lazy property controlling field-level lazy loading enhancement.
     */
    Property<Boolean> getEnableLazyInitialization()

    /**
     * Toggles whether to enhance entity bytecode to support self-contained, active inline dirty tracking.
     * <p>
     * When set to {@code true}, entities actively track their own internal state mutations. This allows Hibernate
     * to immediately discover which fields changed during active flushes, completely avoiding heavy reflection-based
     * state array comparisons against deep snapshot memory states.
     * Defaults to {@code true}.
     * </p>
     *
     * @return A lazy property controlling inline dirty checking enhancement.
     */
    Property<Boolean> getEnableDirtyTracking()

    /**
     * Toggles whether to enhance entity bytecode to automatically manage bidirectional associations.
     * <p>
     * When set to {@code true}, setting one side of a bidirectional relationship automatically updates the other side
     * (e.g., adding an item to a child collection automatically maintains the parent property reference in memory).
     * Defaults to {@code true}.
     * </p>
     *
     * @return A lazy property controlling bidirectional association management enhancement.
     */
    Property<Boolean> getEnableAssociationManagement()

    /**
     * Toggles whether to enable advanced extended bytecode enhancements.
     * <p>
     * When set to {@code true}, Hibernate expands its byte-level enhancement scope beyond simple entity definitions
     * to intercept non-proxy boundaries or customized orchestration scenarios.
     * Defaults to {@code false}.
     * </p>
     *
     * @return A lazy property controlling extended bytecode enhancement strategies.
     */
    Property<Boolean> getEnableExtendedEnhancement()
}
