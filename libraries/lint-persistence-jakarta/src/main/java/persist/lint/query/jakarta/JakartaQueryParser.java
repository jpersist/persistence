package persist.lint.query.jakarta;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.spi.QueryEngine;
import persist.lint.query.spi.QueryParser;

import java.util.List;

/**
 * {@link QueryParser} implementation for the Jakarta Persistence namespace.
 *
 * <p>This parser uses Hibernate ORM&nbsp;6 to compile JPQL query strings
 * into a Semantic Query Model (SQM) tree. The compilation verifies grammar,
 * entity mappings, property paths, and function references entirely offline
 * — no live database connection is required.</p>
 *
 * <p>Instances are created through the {@link #from(List)} factory method.</p>
 *
 * @see JakartaQueryParserFactory
 */
public class JakartaQueryParser implements QueryParser {

    private final Metadata metadata;

    private JakartaQueryParser(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Creates a new {@code JakartaQueryParser} configured with the supplied
     * entity class names.
     *
     * @param discoveredClassNames fully qualified names of JPA entity classes
     * @return a parser ready to validate JPQL queries
     */
    public static JakartaQueryParser from(List<String> discoveredClassNames) {
        return new JakartaQueryParser(buildMetadata(discoveredClassNames));
    }

    /** {@inheritDoc} */
    @Override
    public void parseQuery(String queryName, String queryString) {
        try (SessionFactoryImplementor sessionFactory = from(metadata)) {
            parseQuery(queryString, sessionFactory);
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
        // Pure, strongly-typed Hibernate 6 bootstrapping
        var ssr = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
            .applySetting("hibernate.connection.url", "jdbc:h2:mem:lintdb;DB_CLOSE_DELAY=-1")
            // Explicitly disable background JDBC metadata connections query passes!
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
            // Disable DDL validation loops during compilation
            .applySetting("hibernate.hbm2ddl.auto", "none")
            .build();

        var metadataSources = new MetadataSources(ssr);

        // Strongly register every single ASM-discovered entity class natively!
        discoveredClasses.forEach(metadataSources::addAnnotatedClassName);

        return metadataSources.getMetadataBuilder().build();
    }

    private static void parseQuery(String queryString, SessionFactoryImplementor sessionFactory) {
        // 1. In Hibernate 6, QueryPlanCache & repositories are managed by the centralized QueryEngine
        QueryEngine queryEngine = sessionFactory.getQueryEngine();
        HqlTranslator hqlTranslator = queryEngine.getHqlTranslator();

        // 2. Use the HqlTranslator to compile the string into a Semantic Query Model (SQM) tree
        // This verifies grammar, entity maps, fields, and functions completely offline
        parseQuery(queryString, hqlTranslator);
    }

    private static void parseQuery(String queryString, HqlTranslator hqlTranslator) {
        hqlTranslator.translate(queryString, Object.class);
    }

    private static SessionFactoryImplementor from(Metadata metadata) {
        return (SessionFactoryImplementor) metadata.buildSessionFactory();
    }

}
