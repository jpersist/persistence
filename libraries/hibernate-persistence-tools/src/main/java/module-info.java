/**
 * Hibernate&nbsp;5 provider implementation for legacy {@code javax.persistence} ({@code "javax"} namespace).
 *
 * <p>This module provides runtime and build-time persistence tooling for applications targeting
 * legacy Java Persistence specifications (JPA&nbsp;2.0, 2.1, and 2.2). It registers factory implementations
 * for bytecode enhancement, AST query parsing, and database schema validation using Hibernate&nbsp;5.</p>
 *
 * <h2>Provided Services</h2>
 * <ul>
 *   <li>{@link persistence.tools.spi.NamedObjectParserFactory} &ndash; provided by
 *       {@link hibernate.javax.persistence.tools.provider.HibernateNamedObjectParserFactory}</li>
 *   <li>{@link persistence.tools.spi.SchemaValidatorFactory} &ndash; provided by
 *       {@link hibernate.javax.persistence.tools.provider.HibernateSchemaValidatorFactory}</li>
 *   <li>{@link persistence.tools.spi.EnhancerProvider} &ndash; provided by
 *       {@link hibernate.javax.persistence.tools.provider.HibernateEnhancerProvider}</li>
 * </ul>
 *
 * @since 1.0.0
 * @see io.github.jpersist.tools.api
 */
module io.github.jpersist.tools.hibernate {
    // 1. Depend on the core JPersist validation and enhancer API
    requires io.github.jpersist.tools.api;

    // 2. Require JDK modules utilized by Hibernate internals (JNDI/Naming)
    requires java.naming;

    // 3. Require Hibernate 5 core and the legacy javax.persistence specifications
    // Note: 'org.hibernate.orm.core' is the module name assigned to Hibernate 5.x core JARs
    requires org.hibernate.orm.core;
    requires java.persistence;

    // 4. Register legacy linter and validator factories into the native ServiceLoader
    provides persistence.tools.spi.NamedObjectParserFactory
        with hibernate.javax.persistence.tools.provider.HibernateNamedObjectParserFactory;

    provides persistence.tools.spi.SchemaValidatorFactory
        with hibernate.javax.persistence.tools.provider.HibernateSchemaValidatorFactory;

    // 5. Register the legacy bytecode enhancement provider into the native ServiceLoader
    provides persistence.tools.spi.EnhancerProvider
        with hibernate.javax.persistence.tools.provider.HibernateEnhancerProvider;

    // 6. Maintain strict implementation encapsulation
    // No packages are exported since interaction happens entirely behind the API module SPIs.
}
