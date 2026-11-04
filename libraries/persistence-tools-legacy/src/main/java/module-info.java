module io.github.jpersist.tools.legacy {
    // 1. Depend on the core JPersist validation and enhancer API
    requires io.github.jpersist.tools.api;

    // 2. Require JDK modules utilized by Hibernate internals (JNDI/Naming)
    requires java.naming;

    // 3. Require Hibernate 5 core and the legacy javax.persistence specifications
    // Note: 'org.hibernate.orm.core' is the module name assigned to Hibernate 5.x core JARs
    requires org.hibernate.orm.core;
    requires java.persistence;

    // 4. Register legacy linter and validator factories into the native ServiceLoader
    provides persist.lint.api.spi.NamedObjectParserFactory
        with persist.lint.legacy.LegacyNamedObjectParserFactory;

    provides persist.lint.api.spi.SchemaValidatorFactory
        with persist.lint.legacy.LegacySchemaValidatorFactory;

    // 5. Register the legacy bytecode enhancement provider into the native ServiceLoader
    provides persist.hibernate.enhance.spi.HibernateEnhancerProvider
        with persist.hibernate.enhance.legacy.LegacyHibernateEnhancerProvider;

    // 6. Maintain strict implementation encapsulation
    // No packages are exported since interaction happens entirely behind the API module SPIs.
}
