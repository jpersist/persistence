import persistence.tools.spi.EnhancerProvider;
import persistence.tools.spi.NamedObjectParserFactory;
import persistence.tools.spi.SchemaValidatorFactory;

/**
 * Legacy {@code javax.persistence} (JPA&nbsp;2.x) provider module for the
 * Persistence Tools Libraries.
 *
 * <p>This module implements the SPIs defined in
 * {@code io.github.jpersist.tools.api} using Hibernate&nbsp;5 and the
 * legacy {@code javax.persistence} API. It provides build-time JPQL query
 * validation via Hibernate's AST-based query translator, database schema
 * validation, and entity bytecode enhancement.</p>
 *
 * <h2>Provided services</h2>
 * <ul>
 *   <li>{@link NamedObjectParserFactory} &mdash;
 *       implemented by {@code persist.lint.legacy.LegacyNamedObjectParserFactory}</li>
 *   <li>{@link SchemaValidatorFactory} &mdash;
 *       implemented by {@code persist.lint.legacy.LegacySchemaValidatorFactory}</li>
 *   <li>{@link EnhancerProvider} &mdash;
 *       implemented by {@code persist.hibernate.enhance.legacy.LegacyHibernateEnhancerProvider}</li>
 * </ul>
 *
 * <h2>Encapsulation</h2>
 * <p>No packages are exported. All interaction with this module occurs
 * through the SPI interfaces declared in the API module and discovered
 * via the {@link java.util.ServiceLoader} mechanism.</p>
 *
 * @since 3.1.0
 */
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
    provides NamedObjectParserFactory
        with persist.lint.legacy.LegacyNamedObjectParserFactory;

    provides SchemaValidatorFactory
        with persist.lint.legacy.LegacySchemaValidatorFactory;

    // 5. Register the legacy bytecode enhancement provider into the native ServiceLoader
    provides EnhancerProvider
        with persist.hibernate.enhance.legacy.LegacyHibernateEnhancerProvider;

    // 6. Maintain strict implementation encapsulation
    // No packages are exported since interaction happens entirely behind the API module SPIs.
}
