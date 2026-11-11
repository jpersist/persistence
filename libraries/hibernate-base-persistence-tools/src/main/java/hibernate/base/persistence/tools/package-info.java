/**
 * Shared persistence tooling infrastructure, configuration options, and utilities for Hibernate.
 *
 * <p>This package provides common abstractions and utility components utilized across Hibernate
 * provider implementations (such as Hibernate&nbsp;5 and Hibernate ORM&nbsp;6):</p>
 *
 * <ul>
 *   <li>{@link hibernate.base.persistence.tools.HibernateEnhancerOption} &ndash; Enumeration of
 *       bytecode enhancement configuration options and their associated property keys.</li>
 *   <li>{@link hibernate.base.persistence.tools.HibernateUtil} &ndash; Shared utility methods for
 *       class loader creation, file-to-URL conversion, and enhancer setting mapping.</li>
 * </ul>
 *
 * @since 1.0.0
 * @see persistence.tools
 */
package hibernate.base.persistence.tools;
