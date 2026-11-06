/**
 * Core API module for the Persistence Tools Libraries.
 *
 * <p>Defines the Service Provider Interfaces (SPIs) for build-time
 * validation and bytecode enhancement of JPA persistence artifacts.
 * This module contains no concrete implementations; provider modules
 * such as {@code io.github.jpersist.tools.jakarta} and
 * {@code io.github.jpersist.tools.legacy} are discovered at runtime
 * through the {@link java.util.ServiceLoader} mechanism.</p>
 *
 * <h2>Exported packages</h2>
 * <ul>
 *   <li>{@code persist.api.spi} &mdash; root service-provider interface
 *       ({@link persist.api.spi.PersistenceServiceProvider})</li>
 *   <li>{@code persist.lint.api} &mdash; shared model types
 *       ({@link persist.lint.api.NamedObjectDescriptor},
 *       {@link persist.lint.api.NamedObjectType})</li>
 *   <li>{@code persist.lint.api.spi} &mdash; linting SPIs
 *       ({@link persist.lint.api.spi.NamedObjectParser},
 *       {@link persist.lint.api.spi.SchemaValidator}, and their factories)</li>
 *   <li>{@code persist.hibernate.enhance.spi} &mdash; Hibernate bytecode
 *       enhancement SPI ({@link persist.hibernate.enhance.spi.HibernateEnhancer},
 *       {@link persist.hibernate.enhance.spi.HibernateEnhancerProvider})</li>
 * </ul>
 *
 * <h2>Service declarations</h2>
 * <p>This module declares {@code uses} directives for all provider
 * interfaces so that {@link java.util.ServiceLoader} can locate
 * implementations at runtime.</p>
 *
 * @since 3.1.0
 */
module io.github.jpersist.tools.api {

    exports persist.api.spi;
    exports persist.lint.api;
    exports persist.lint.api.spi;
    exports persist.hibernate.enhance.spi;

    uses persist.api.spi.PersistenceServiceProvider;
    uses persist.lint.api.spi.PersistenceLinterFactory;
    uses persist.lint.api.spi.NamedObjectParserFactory;
    uses persist.lint.api.spi.SchemaValidatorFactory;
    uses persist.hibernate.enhance.spi.HibernateEnhancerProvider;

}
