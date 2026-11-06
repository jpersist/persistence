/**
 * Jakarta Persistence (JPA&nbsp;3.x) provider module for the Persistence Tools Libraries.
 *
 * <p>This module implements the SPIs defined in
 * {@code io.github.jpersist.tools.api} using Hibernate ORM&nbsp;6 and
 * Jakarta Persistence. It provides build-time JPQL query validation via
 * Hibernate's Semantic Query Model (SQM), database schema validation,
 * and entity bytecode enhancement.</p>
 *
 * <h2>Provided services</h2>
 * <ul>
 *   <li>{@link persist.lint.api.spi.NamedObjectParserFactory} &mdash;
 *       implemented by {@code persist.lint.jakarta.JakartaNamedObjectParserFactory}</li>
 *   <li>{@link persist.lint.api.spi.SchemaValidatorFactory} &mdash;
 *       implemented by {@code persist.lint.jakarta.JakartaSchemaValidatorFactory}</li>
 *   <li>{@link persist.hibernate.enhance.spi.HibernateEnhancerProvider} &mdash;
 *       implemented by {@code persist.hibernate.enhance.jakarta.JakartaHibernateEnhancerProvider}</li>
 * </ul>
 *
 * <h2>Encapsulation</h2>
 * <p>No packages are exported. All interaction with this module occurs
 * through the SPI interfaces declared in the API module and discovered
 * via the {@link java.util.ServiceLoader} mechanism.</p>
 *
 * @since 3.1.0
 */
module io.github.jpersist.tools.jakarta {
    // 1. Depend on the core JPersist validation and enhancer API
    requires io.github.jpersist.tools.api;

    // 2. Require JDK modules utilized by Hibernate internals (JNDI/Naming)
    requires java.naming;

    // 3. Require Hibernate ORM 6 core and Jakarta Persistence spec boundaries
    // Note: These names align with standard Hibernate 6 and Jakarta artifacts
    requires org.hibernate.orm.core;
    requires jakarta.persistence;

    // 4. Register linter and validator factories into the native ServiceLoader
    provides persist.lint.api.spi.NamedObjectParserFactory
        with persist.lint.jakarta.JakartaNamedObjectParserFactory;

    provides persist.lint.api.spi.SchemaValidatorFactory
        with persist.lint.jakarta.JakartaSchemaValidatorFactory;

    // 5. Register the bytecode enhancement provider into the native ServiceLoader
    provides persist.hibernate.enhance.spi.HibernateEnhancerProvider
        with persist.hibernate.enhance.jakarta.JakartaHibernateEnhancerProvider;

    // 6. Enforce strong encapsulation by hiding all implementation packages.
    // No 'exports' directive is specified here since the project interacts
    // with this provider strictly decoupled behind the 'persistence-tools-api' SPIs.
}
