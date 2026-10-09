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

public class LegacyQueryParser implements QueryParser {

    private final Metadata metadata;

    private LegacyQueryParser(Metadata metadata) {
        this.metadata = metadata;
    }

    public static LegacyQueryParser from(List<String> discoveredClassNames) {
        return new LegacyQueryParser(buildMetadata(discoveredClassNames));
    }

    @Override
    public void parseQuery(String queryName, String queryString) {
        try (SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory()) {
            // Initialize the standard AST-based translator factory
            QueryTranslatorFactory translatorFactory = new ASTQueryTranslatorFactory();

            // Create a translator instance for this specific query string
            // The queryIdentifier can match your named query string key
            QueryTranslator translator = translatorFactory.createQueryTranslator(
                queryName,
                queryString,
                Collections.emptyMap(),
                sessionFactory,
                null
            );

            // 4. Force compilation. This processes the HQL into the ANTLR AST tree
            // and translates it to physical SQL.
            // If the query violates syntax or property paths, it fails here.
            translator.compile(Collections.emptyMap(), false);
        }
    }

    public static Metadata buildMetadata(List<String> discoveredClasses) {
        // Pure, strongly-typed legacy Hibernate 5 bootstrapping

        // 1. Define settings required for a completely offline build-time validation
        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
            // Provide a dialect so the HQL compiler understands function mappings
            .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
            // Explicitly disable automatic database schema actions during linting
            .applySetting("hibernate.hbm2ddl.auto", "none")
            // Optional: Enforce strict compliance with JPA 2.2 spec rules
            .applySetting("hibernate.jpa.compliance.query", "true")
            .build();

        var metadataSources = new MetadataSources(serviceRegistry);

        discoveredClasses.forEach(metadataSources::addAnnotatedClassName);

        return metadataSources.getMetadataBuilder().build();
    }

}
