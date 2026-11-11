/**
 * Common persistence tooling infrastructure, configuration options, and utilities for Hibernate.
 *
 * <p>This module provides shared baseline contracts and utility methods used across version-specific
 * Hibernate persistence tooling providers (such as Hibernate&nbsp;5 and Hibernate ORM&nbsp;6).
 * It encapsulates shared enhancer option configuration and class loading helpers, while transitively
 * exposing the core persistence tools API.</p>
 *
 * <h2>Exported Packages</h2>
 * <ul>
 *   <li>{@link hibernate.base.persistence.tools} &ndash; Common bytecode enhancement options
 *       and utility classes for Hibernate tool implementations.</li>
 * </ul>
 *
 * @since 1.0.0
 * @see io.github.jpersist.tools.api
 * @see hibernate.base.persistence.tools
 */
module io.github.jpersist.tools.hibernate.base {
    // Expose the API transitively since shared base classes likely implement or extend core SPIs
    requires transitive io.github.jpersist.tools.api;

    // Export the base package to Hibernate provider modules
    exports hibernate.base.persistence.tools;
}
