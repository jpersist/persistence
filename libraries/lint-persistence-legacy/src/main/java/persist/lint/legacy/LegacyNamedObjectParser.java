package persist.lint.legacy;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import persist.lint.api.NamedObjectDescriptor;
import persist.lint.api.spi.NamedObjectParser;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class LegacyNamedObjectParser implements NamedObjectParser {

    private final Metadata metadata;

    private LegacyNamedObjectParser(Metadata metadata) {
        this.metadata = metadata;
    }

    public static LegacyNamedObjectParser from(List<String> discoveredClassNames) {
        return new LegacyNamedObjectParser(buildMetadata(discoveredClassNames));
    }

    @Override
    public NamedObjectDescriptor parseQuery(String name, String queryString) {
        try (SessionFactoryImplementor sessionFactory = from(metadata)) {
            // Initialize the standard AST-based translator factory
            QueryTranslatorFactory translatorFactory = new ASTQueryTranslatorFactory();

            return parseQuery(name, queryString, translatorFactory, sessionFactory);
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

    private static NamedObjectDescriptor parseQuery(String name, String queryString,
                                   QueryTranslatorFactory translatorFactory,
                                   SessionFactoryImplementor sessionFactory) {
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
            namedParameters = new TreeSet<>(parameterTranslations.getNamedParameterInformationMap().keySet());

            // Collect positional parameters (?1)
            positionalParameters = new TreeSet<>(parameterTranslations.getPositionalParameterInformationMap().keySet());
        }

        return NamedObjectDescriptor.of(name, queryString, namedParameters, positionalParameters);
    }

    private static SessionFactoryImplementor from(Metadata metadata) {
        return (SessionFactoryImplementor) metadata.buildSessionFactory();
    }

}
