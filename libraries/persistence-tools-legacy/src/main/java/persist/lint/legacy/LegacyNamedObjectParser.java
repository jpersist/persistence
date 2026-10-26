package persist.lint.legacy;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.service.ServiceRegistry;
import persist.lint.api.NamedObjectDescriptor;
import persist.lint.api.spi.NamedObjectParser;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * {@link NamedObjectParser} implementation for the legacy
 * {@code javax.persistence} namespace.
 *
 * <p>This parser uses Hibernate&nbsp;5 to compile JPQL query strings through
 * the AST-based {@link org.hibernate.hql.spi.QueryTranslatorFactory}. The
 * compilation verifies HQL grammar, entity mappings, and property paths
 * entirely offline — no live database connection is required.</p>
 *
 * <p>Stored procedure declarations are validated by recording the procedure
 * name and its parameter names into a {@link NamedObjectDescriptor}.</p>
 *
 * <p>Instances are created through the {@link #from(List)} factory method.
 * This class implements {@link AutoCloseable}; closing an instance releases
 * the underlying Hibernate {@code SessionFactory} and
 * {@code ServiceRegistry}. Always use try-with-resources.</p>
 *
 * @since 2.0.0
 * @see LegacyNamedObjectParserFactory
 */
public class LegacyNamedObjectParser implements NamedObjectParser {

    private final ServiceRegistry serviceRegistry;
    private final SessionFactoryImplementor sessionFactory;
    private final QueryTranslatorFactory translatorFactory;

    private LegacyNamedObjectParser(ServiceRegistry serviceRegistry, SessionFactoryImplementor sessionFactory) {
        this.serviceRegistry = serviceRegistry;
        this.sessionFactory = sessionFactory;
        this.translatorFactory = new ASTQueryTranslatorFactory();
    }

    /**
     * Creates a new {@code LegacyNamedObjectParser} configured with the
     * supplied entity class names.
     *
     * @param discoveredClassNames fully qualified names of JPA entity classes
     * @return a parser ready to validate named queries and stored procedures;
     *         the caller is responsible for closing it when finished
     */
    public static LegacyNamedObjectParser from(List<String> discoveredClassNames) {
        // 1. Define settings required for a completely offline build-time validation
        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
            // Provide a dialect so the HQL compiler understands function mappings
            .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
            // Explicitly block background metadata connection sniffing loops
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
            // Explicitly disable automatic database schema actions during linting
            .applySetting("hibernate.hbm2ddl.auto", "none")
            // Optional: Enforce strict compliance with JPA 2.2 spec rules
            .applySetting("hibernate.jpa.compliance.query", "true")
            // Bypasses runtime enhancement routines during testing loops
            .applySetting("hibernate.bytecode.provider", "none")
            .build();

        var metadataSources = new MetadataSources(serviceRegistry);

        discoveredClassNames.forEach(metadataSources::addAnnotatedClassName);

        var metadata = metadataSources.getMetadataBuilder().build();

        SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();
        return new LegacyNamedObjectParser(serviceRegistry, sessionFactory);
    }

    /** {@inheritDoc} */
    @Override
    public NamedObjectDescriptor parseQuery(String name, String queryString) {
        // Create a translator instance for this specific query string
        // The queryIdentifier can match your named query string key
        QueryTranslator translator = translatorFactory.createQueryTranslator(
            name,
            queryString,
            Collections.emptyMap(),
            sessionFactory,
            null
        );

        // Force compilation. This processes the HQL into the ANTLR AST tree
        // and translates it to physical SQL.
        // If the query violates syntax or property paths, it fails here.
        translator.compile(Collections.emptyMap(), false);

        Set<String> namedParameters = new TreeSet<>();
        Set<Integer> positionalParameters = new TreeSet<>();

        var parameterTranslations = translator.getParameterTranslations();
        if (parameterTranslations != null) {
            // Collect named parameters (:skuCode)
            namedParameters.addAll(parameterTranslations.getNamedParameterInformationMap().keySet());

            // Collect positional parameters (?1)
            positionalParameters.addAll(parameterTranslations.getPositionalParameterInformationMap().keySet());
        }

        return NamedObjectDescriptor.of(name, queryString, namedParameters, positionalParameters);
    }

    /** {@inheritDoc} */
    @Override
    public NamedObjectDescriptor parseStoredProcedure(String name, String procedureName, Set<String> parameterNames) {
        return NamedObjectDescriptor.of(name, procedureName, parameterNames);
    }

    /**
     * Releases the Hibernate {@code SessionFactory} and
     * {@code ServiceRegistry} held by this parser.
     *
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        sessionFactory.close();
        StandardServiceRegistryBuilder.destroy(serviceRegistry);
    }

}
