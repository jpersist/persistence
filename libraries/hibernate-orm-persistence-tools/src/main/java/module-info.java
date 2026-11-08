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
        with hibernate.persistence.tools.provider.HibernateNamedObjectParserFactory;

    provides persistence.tools.spi.SchemaValidatorFactory
        with hibernate.persistence.tools.provider.HibernateSchemaValidatorFactory;

    // 5. Register the bytecode enhancement provider into the native ServiceLoader
    provides persistence.tools.spi.EnhancerProvider
        with hibernate.persistence.tools.provider.HibernateEnhancerProvider;

    // 6. Enforce strong encapsulation by hiding all implementation packages.
    // No 'exports' directive is specified here since the project interacts
    // with this provider strictly decoupled behind the 'persistence-tools-api' SPIs.
}
