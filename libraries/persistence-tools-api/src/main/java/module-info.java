/**
 * Defines the public API and Service Provider Interfaces (SPI) for JPersist persistence tools.
 *
 * <p>This module provides vendor-agnostic abstractions and contracts for build-time persistence
 * validation, bytecode enhancement, schema verification, and named query parsing across JPA
 * specification versions.</p>
 *
 * <h2>Exported Packages</h2>
 * <ul>
 *   <li>{@link persistence.tools} &ndash; User-facing validation, query parsing, schema verification,
 *       and bytecode enhancement contracts.</li>
 *   <li>{@link persistence.tools.spi} &ndash; Service Provider Interfaces (SPI) and factory discovery
 *       mechanisms for namespace-specific implementations.</li>
 * </ul>
 *
 * <h2>Service Loader Discoveries</h2>
 * <p>This module declares {@code uses} directives for the following service provider interfaces:</p>
 * <ul>
 *   <li>{@link persistence.tools.spi.PersistenceServiceProvider}</li>
 *   <li>{@link persistence.tools.spi.PersistenceLinterFactory}</li>
 *   <li>{@link persistence.tools.spi.NamedObjectParserFactory}</li>
 *   <li>{@link persistence.tools.spi.SchemaValidatorFactory}</li>
 *   <li>{@link persistence.tools.spi.EnhancerProvider}</li>
 * </ul>
 *
 * @since 4.0.0
 */
module io.github.jpersist.tools.api {

    // 1. Export clean, vendor-agnostic user-facing API contracts
    exports persistence.tools;

    // 2. Export service provider interfaces for reference implementations
    exports persistence.tools.spi;

    // 3. Declare modular ServiceLoader discovery targets across the new SPI layout
    uses persistence.tools.spi.PersistenceServiceProvider;
    uses persistence.tools.spi.PersistenceLinterFactory;
    uses persistence.tools.spi.NamedObjectParserFactory;
    uses persistence.tools.spi.SchemaValidatorFactory;
    uses persistence.tools.spi.EnhancerProvider;

}
