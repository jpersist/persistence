/**
 * Hibernate ORM&nbsp;6 provider implementation for Jakarta Persistence ({@code "jakarta"} namespace).
 *
 * <p>This module provides runtime and build-time persistence tooling for applications targeting
 * Jakarta Persistence specifications (JPA&nbsp;3.0, 3.1, and 3.2). It registers factory implementations
 * for bytecode enhancement, semantic query model (SQM) query parsing, and database schema validation
 * using Hibernate ORM&nbsp;6.</p>
 *
 * <h2>Provided Services</h2>
 * <ul>
 *   <li>{@link persistence.tools.spi.NamedObjectParserFactory} &ndash; provided by
 *       {@link hibernate.jakarta.persistence.tools.provider.HibernateNamedObjectParserFactory}</li>
 *   <li>{@link persistence.tools.spi.SchemaValidatorFactory} &ndash; provided by
 *       {@link hibernate.jakarta.persistence.tools.provider.HibernateSchemaValidatorFactory}</li>
 *   <li>{@link persistence.tools.spi.EnhancerProvider} &ndash; provided by
 *       {@link hibernate.jakarta.persistence.tools.provider.HibernateEnhancerProvider}</li>
 * </ul>
 *
 * @since 2.0.0
 * @see io.github.jpersist.tools.api
 */
module io.github.jpersist.tools.hibernate.orm {
    // 1. Depend on the core JPersist validation and enhancer API
    requires io.github.jpersist.tools.api;

    // 2. Require JDK modules utilized by Hibernate internals (JNDI/Naming)
    requires java.naming;

    // 3. Require Hibernate ORM 6 core and Jakarta Persistence spec boundaries
    // Note: These names align with standard Hibernate 6 and Jakarta artifacts
    requires org.hibernate.orm.core;
    requires jakarta.persistence;

    // 4. Register linter and validator factories into the native ServiceLoader
    provides persistence.tools.spi.NamedObjectParserFactory
        with hibernate.jakarta.persistence.tools.provider.HibernateNamedObjectParserFactory;

    provides persistence.tools.spi.SchemaValidatorFactory
        with hibernate.jakarta.persistence.tools.provider.HibernateSchemaValidatorFactory;

    // 5. Register the bytecode enhancement provider into the native ServiceLoader
    provides persistence.tools.spi.EnhancerProvider
        with hibernate.jakarta.persistence.tools.provider.HibernateEnhancerProvider;

    // 6. Enforce strong encapsulation by hiding all implementation packages.
    // No 'exports' directive is specified here since the project interacts
    // with this provider strictly decoupled behind the 'persistence-tools-api' SPIs.
}
