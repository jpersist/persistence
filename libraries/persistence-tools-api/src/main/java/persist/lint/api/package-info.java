/**
 * Core data types for the Lint Persistence build-time validation API.
 *
 * <p>This package contains the value objects shared across the SPI and
 * its provider implementations:</p>
 *
 * <ul>
 *   <li>{@link persist.lint.api.NamedObjectDescriptor} — immutable
 *       descriptor returned after a named query or stored procedure has
 *       been successfully parsed.</li>
 *   <li>{@link persist.lint.api.NamedObjectType} — enumerates the kinds
 *       of named objects (query, stored procedure).</li>
 * </ul>
 *
 * @since 2.0.0
 * @see persist.lint.api.spi
 */
package persist.lint.api;
