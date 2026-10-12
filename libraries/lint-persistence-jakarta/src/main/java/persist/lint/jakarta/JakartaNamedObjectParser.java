package persist.lint.jakarta;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.tree.SqmStatement;
import persist.lint.api.NamedObjectDescriptor;
import persist.lint.api.spi.NamedObjectParser;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class JakartaNamedObjectParser implements NamedObjectParser {

    private final Metadata metadata;

    private JakartaNamedObjectParser(Metadata metadata) {
        this.metadata = metadata;
    }

    public static JakartaNamedObjectParser from(List<String> discoveredClassNames) {
        return new JakartaNamedObjectParser(buildMetadata(discoveredClassNames));
    }

    @Override
    public NamedObjectDescriptor parseQuery(String name, String queryString) {
        try (SessionFactoryImplementor sessionFactory = from(metadata)) {
            return parseQuery(name, queryString, sessionFactory);
        }
    }

    @Override
    public NamedObjectDescriptor parseStoredProcedure(String name, String procedureName, Set<String> parameterNames) {
        return NamedObjectDescriptor.of(name, procedureName, parameterNames);
    }

    /**
     * Builds a Hibernate {@link Metadata} instance from the given entity
     * class names using an offline H2 dialect configuration.
     *
     * @param discoveredClasses fully qualified names of JPA entity classes
     * @return the compiled metadata
     */
    private static Metadata buildMetadata(List<String> discoveredClasses) {
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

    private static NamedObjectDescriptor parseQuery(String name, String queryString, SessionFactoryImplementor sessionFactory) {
        // 1. In Hibernate 6, QueryPlanCache & repositories are managed by the centralized QueryEngine
        QueryEngine queryEngine = sessionFactory.getQueryEngine();
        HqlTranslator hqlTranslator = queryEngine.getHqlTranslator();

        // 2. Use the HqlTranslator to compile the string into a Semantic Query Model (SQM) tree
        // This verifies grammar, entity maps, fields, and functions completely offline
        SqmStatement<?> sqmStatement = parseQuery(queryString, hqlTranslator);

        Set<String> namedParameters = new TreeSet<>();
        Set<Integer> positionalParameters = new TreeSet<>();

        // Extract parameter snapshots natively without reflection
        if (sqmStatement.getSqmParameters() != null) {
            sqmStatement.getSqmParameters().forEach(sqmParameter -> {
                if (sqmParameter.getName() != null) {
                    namedParameters.add(sqmParameter.getName());
                } else if (sqmParameter.getPosition() != null) {
                    positionalParameters.add(sqmParameter.getPosition());
                }
            });
        }

        return NamedObjectDescriptor.of(name, queryString, namedParameters, positionalParameters);
    }

    private static SqmStatement<?> parseQuery(String queryString, HqlTranslator hqlTranslator) {
        // Compile the HQL string into a Semantic Query Model (SQM) tree layout
        return hqlTranslator.translate(queryString, Object.class);
    }

    private static SessionFactoryImplementor from(Metadata metadata) {
        return (SessionFactoryImplementor) metadata.buildSessionFactory();
    }

}
