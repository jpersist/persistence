package persist.lint.query.legacy;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import persist.lint.query.spi.QueryParser;

import java.util.Collections;
import java.util.List;

/**
 * {@link QueryParser} implementation for the legacy {@code javax.persistence}
 * namespace.
 *
 * <p>This parser uses Hibernate&nbsp;5 to compile JPQL query strings through
 * the AST-based {@link QueryTranslatorFactory}. The compilation verifies HQL
 * grammar, entity mappings, and property paths entirely offline — no live
 * database connection is required.</p>
 *
 * <p>Instances are created through the {@link #from(List)} factory method.</p>
 *
 * @see LegacyQueryParserFactory
 */
public class LegacyQueryParser implements QueryParser {

    private final Metadata metadata;

    private LegacyQueryParser(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Creates a new {@code LegacyQueryParser} configured with the supplied
     * entity class names.
     *
     * @param discoveredClassNames fully qualified names of JPA entity classes
     * @return a parser ready to validate JPQL queries
     */
    public static LegacyQueryParser from(List<String> discoveredClassNames) {
        return new LegacyQueryParser(buildMetadata(discoveredClassNames));
    }

    /** {@inheritDoc} */
    @Override
    public void parseQuery(String queryName, String queryString) {
        try (SessionFactoryImplementor sessionFactory = from(metadata)) {
            // Initialize the standard AST-based translator factory
            QueryTranslatorFactory translatorFactory = new ASTQueryTranslatorFactory();

            parseQuery(queryName, queryString, translatorFactory, sessionFactory);
        }
    }

    /**
     * Builds a Hibernate {@link Metadata} instance from the given entity
     * class names using an offline H2 dialect configuration.
     *
     * @param discoveredClasses fully qualified names of JPA entity classes
     * @return the compiled metadata
     */
    public static Metadata buildMetadata(List<String> discoveredClasses) {
        // Pure, strongly-typed legacy Hibernate 5 bootstrapping

        // 1. Define settings required for a completely offline build-time validation
        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
            // Provide a dialect so the HQL compiler understands function mappings
            .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
            .applySetting("hibernate.connection.url", "jdbc:h2:mem:lintdb;DB_CLOSE_DELAY=-1")
            // Explicitly disable automatic database schema actions during linting
            .applySetting("hibernate.hbm2ddl.auto", "none")
            // Optional: Enforce strict compliance with JPA 2.2 spec rules
            .applySetting("hibernate.jpa.compliance.query", "true")
            .build();

        var metadataSources = new MetadataSources(serviceRegistry);

        discoveredClasses.forEach(metadataSources::addAnnotatedClassName);

        return metadataSources.getMetadataBuilder().build();
    }

    private static void parseQuery(String queryName, String queryString,
                                   QueryTranslatorFactory translatorFactory,
                                   SessionFactoryImplementor sessionFactory) {
        // Create a translator instance for this specific query string
        // The queryIdentifier can match your named query string key
        QueryTranslator translator = translatorFactory.createQueryTranslator(
            queryName,
            queryString,
            Collections.emptyMap(),
            sessionFactory,
            null
        );

        // Force compilation. This processes the HQL into the ANTLR AST tree
        // and translates it to physical SQL.
        // If the query violates syntax or property paths, it fails here.
        translator.compile(Collections.emptyMap(), false);
    }

    private static SessionFactoryImplementor from(Metadata metadata) {
        return (SessionFactoryImplementor) metadata.buildSessionFactory();
    }

}
