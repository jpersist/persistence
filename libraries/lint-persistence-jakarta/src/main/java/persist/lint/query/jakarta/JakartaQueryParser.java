package persist.lint.query.jakarta;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.spi.QueryEngine;
import persist.lint.query.spi.QueryParser;

import java.util.List;

public class JakartaQueryParser implements QueryParser {

    private final Metadata metadata;

    private JakartaQueryParser(Metadata metadata) {
        this.metadata = metadata;
    }

    public static JakartaQueryParser from(List<String> discoveredClassNames) {
        return new JakartaQueryParser(buildMetadata(discoveredClassNames));
    }

    @Override
    public void parseQuery(String queryName, String queryString) {
        try (SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory()) {
            // 1. In Hibernate 6, QueryPlanCache & repositories are managed by the centralized QueryEngine
            QueryEngine queryEngine = sessionFactory.getQueryEngine();
            HqlTranslator hqlTranslator = queryEngine.getHqlTranslator();

            // 2. Use the HqlTranslator to compile the string into a Semantic Query Model (SQM) tree
            // This verifies grammar, entity maps, fields, and functions completely offline
            hqlTranslator.translate(queryString, Object.class);
        }
    }

    public static Metadata buildMetadata(List<String> discoveredClasses) {
        // Pure, strongly-typed Hibernate 6 bootstrapping
        var ssr = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
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

}
