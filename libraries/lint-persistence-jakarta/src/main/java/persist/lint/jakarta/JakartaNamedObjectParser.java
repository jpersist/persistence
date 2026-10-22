package persist.lint.jakarta;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.service.ServiceRegistry;
import persist.lint.api.NamedObjectDescriptor;
import persist.lint.api.spi.NamedObjectParser;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * {@link NamedObjectParser} implementation for the Jakarta Persistence namespace.
 *
 * <p>This parser uses Hibernate ORM&nbsp;6 to compile JPQL query strings
 * into a Semantic Query Model (SQM) tree. The compilation verifies grammar,
 * entity mappings, property paths, and function references entirely offline
 * — no live database connection is required.</p>
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
 * @see JakartaNamedObjectParserFactory
 */
public class JakartaNamedObjectParser implements NamedObjectParser {

    private final SessionFactoryImplementor sessionFactory;

    private final ServiceRegistry serviceRegistry;

    private final HqlTranslator translator;

    private JakartaNamedObjectParser(ServiceRegistry serviceRegistry, SessionFactoryImplementor sessionFactory) {
        this.serviceRegistry = serviceRegistry;
        this.sessionFactory = sessionFactory;
        this.translator = sessionFactory.getQueryEngine().getHqlTranslator();
    }

    /**
     * Creates a new {@code JakartaNamedObjectParser} configured with the
     * supplied entity class names.
     *
     * @param discoveredClassNames fully qualified names of JPA entity classes
     * @return a parser ready to validate named queries and stored procedures;
     *         the caller is responsible for closing it when finished
     */
    public static JakartaNamedObjectParser from(List<String> discoveredClassNames) {
        var ssr = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
            // Explicitly disable background JDBC metadata connections query passes!
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
            // Disable DDL validation loops during compilation
            .applySetting("hibernate.hbm2ddl.auto", "none")
            // Disable metadata builder cache processing to speed up bootstrap
            .applySetting("hibernate.bytecode.provider", "none")
            .build();

        var metadataSources = new MetadataSources(ssr);

        // Strongly register every single ASM-discovered entity class natively!
        discoveredClassNames.forEach(metadataSources::addAnnotatedClassName);

        var metadata = metadataSources.getMetadataBuilder().build();

        // Build the concrete SessionFactory mapping. Because DDL validation and JDBC lookups
        // are disabled above, this builds almost instantly using purely reflection mappings.
        SessionFactoryImplementor sf = (SessionFactoryImplementor) metadata.buildSessionFactory();

        return new JakartaNamedObjectParser(ssr, sf);
    }

    /** {@inheritDoc} */
    @Override
    public NamedObjectDescriptor parseQuery(String name, String queryString) {
        // Use the HqlTranslator to compile the string into a Semantic Query Model (SQM) tree
        // This verifies grammar, entity maps, fields, and functions completely offline
        SqmStatement<?> sqmStatement = translator.translate(queryString, Object.class);

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
