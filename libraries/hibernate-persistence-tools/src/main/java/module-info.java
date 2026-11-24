/**
 * Hibernate&nbsp;5 provider implementation for legacy {@code javax.persistence} ({@code "javax"} namespace).
 *
 * <p>This module provides runtime and build-time persistence tooling for applications targeting
 * legacy Java Persistence specifications (JPA&nbsp;2.0, 2.1, and 2.2). It registers factory implementations
 * for entity model linting, offline DDL schema export, AST query parsing, database schema validation,
 * and bytecode enhancement using Hibernate&nbsp;5.</p>
 *
 * <h2>Provided Services</h2>
 * <ul>
 *   <li>{@link persistence.tools.spi.EntityModelLinterFactory} &ndash; provided by
 *       {@link hibernate.javax.persistence.tools.provider.HibernateEntityModelLinterFactory}</li>
 *   <li>{@link persistence.tools.spi.NamedObjectParserFactory} &ndash; provided by
 *       {@link hibernate.javax.persistence.tools.provider.HibernateNamedObjectParserFactory}</li>
 *   <li>{@link persistence.tools.spi.SchemaValidatorFactory} &ndash; provided by
 *       {@link hibernate.javax.persistence.tools.provider.HibernateSchemaValidatorFactory}</li>
 *   <li>{@link persistence.tools.spi.SchemaExporterFactory} &ndash; provided by
 *       {@link hibernate.javax.persistence.tools.provider.HibernateSchemaExporterFactory}</li>
 *   <li>{@link persistence.tools.spi.EnhancerProvider} &ndash; provided by
 *       {@link hibernate.javax.persistence.tools.provider.HibernateEnhancerProvider}</li>
 * </ul>
 *
 * @since 1.0.0
 * @see io.github.jpersist.tools.api
 * @see io.github.jpersist.tools.hibernate.base
 */
module io.github.jpersist.tools.hibernate {
    // 1. Depend on the core JPersist validation and enhancer API
    requires io.github.jpersist.tools.hibernate.base;

    // 2. Require JDK modules utilized by Hibernate internals (JNDI/Naming)
    requires java.naming;

    // 3. Require Hibernate 5 core and the legacy javax.persistence specifications
    // Note: 'org.hibernate.orm.core' is the module name assigned to Hibernate 5.x core JARs
    requires org.hibernate.orm.core;
    requires java.persistence;
    requires java.sql;

    // 4. Register legacy linter and validator factories into the native ServiceLoader
    provides persistence.tools.spi.EntityModelLinterFactory
        with hibernate.javax.persistence.tools.provider.HibernateEntityModelLinterFactory;

    provides persistence.tools.spi.NamedObjectParserFactory
        with hibernate.javax.persistence.tools.provider.HibernateNamedObjectParserFactory;

    provides persistence.tools.spi.SchemaValidatorFactory
        with hibernate.javax.persistence.tools.provider.HibernateSchemaValidatorFactory;

    // 5. Register the schema exporter factory into the native ServiceLoader
    provides persistence.tools.spi.SchemaExporterFactory
        with hibernate.javax.persistence.tools.provider.HibernateSchemaExporterFactory;

    // 6. Register the legacy bytecode enhancement provider into the native ServiceLoader
    provides persistence.tools.spi.EnhancerProvider
        with hibernate.javax.persistence.tools.provider.HibernateEnhancerProvider;

    // 7. Grant Hibernate 5 reflective access to instantiate internal SPI providers (e.g. NoOpConnectionProvider)
    // without exporting the package at compile time
    opens hibernate.javax.persistence.tools.internal to org.hibernate.orm.core;
}
