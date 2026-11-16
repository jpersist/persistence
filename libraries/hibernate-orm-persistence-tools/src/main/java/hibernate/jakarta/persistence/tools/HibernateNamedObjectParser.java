/*
 * Copyright 2024-2026 JPersist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hibernate.jakarta.persistence.tools;

import hibernate.base.persistence.tools.HibernateUtil;
import hibernate.jakarta.persistence.tools.internal.NoOpConnectionProvider;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.service.ServiceRegistry;
import hibernate.jakarta.persistence.tools.provider.HibernateNamedObjectParserFactory;
import persistence.tools.NamedObjectDescriptor;
import persistence.tools.NamedObjectParser;

import java.util.*;

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
 * <p>Instances are created through the {@link #from(List)} or
 * {@link #from(List, Map)} factory methods. This class implements
 * {@link AutoCloseable}; closing an instance releases the underlying
 * Hibernate {@code SessionFactory} and {@code ServiceRegistry}. Always use
 * try-with-resources.</p>
 *
 * @since 1.0.0
 * @see HibernateNamedObjectParserFactory
 */
public class HibernateNamedObjectParser implements NamedObjectParser {

    private static final Map<String, Object> DEFAULT_SETTINGS = Map.of(
        // Provide a baseline dialect so the SQM compiler understands standard function mappings
        "hibernate.dialect", "org.hibernate.dialect.H2Dialect",
        // Explicitly disable background JDBC metadata connection passes
        "hibernate.boot.allow_jdbc_metadata_access", "false",
        // Prevent Hibernate 6 from falling back to minimum H2 dialect capabilities when offline
        "jakarta.persistence.database-product-version", "2.2.224",
        // Suppress HHH000181 connection provider info logs during offline bootstrap
        "hibernate.connection.provider_class", NoOpConnectionProvider.class.getName(),
        // Enforce strict compliance with Jakarta Persistence query rules by default
        "hibernate.jpa.compliance.query", "true",
        // Bypass runtime bytecode enhancement routines during offline linting
        "hibernate.bytecode.provider", "none"
    );

    private static final Map<String, Object> ENFORCED_SETTINGS = Map.of(
        // Guarantee automatic database schema actions remain disabled during query linting
        "hibernate.hbm2ddl.auto", "none"
    );

    private final SessionFactoryImplementor sessionFactory;

    private final ServiceRegistry serviceRegistry;

    private final HqlTranslator translator;

    private HibernateNamedObjectParser(ServiceRegistry serviceRegistry, SessionFactoryImplementor sessionFactory) {
        this.serviceRegistry = serviceRegistry;
        this.sessionFactory = sessionFactory;
        this.translator = sessionFactory.getQueryEngine().getHqlTranslator();
    }

    /**
     * Creates a new {@code HibernateNamedObjectParser} configured with the
     * supplied entity class names and default offline settings.
     *
     * @param discoveredClassNames fully qualified names of JPA entity classes
     * @return a parser ready to validate named queries and stored procedures;
     *         the caller is responsible for closing it when finished
     */
    public static HibernateNamedObjectParser from(List<String> discoveredClassNames) {
        return from(discoveredClassNames, Collections.emptyMap());
    }

    /**
     * Creates a new {@code HibernateNamedObjectParser} configured with the
     * supplied entity class names and custom settings merged on top of
     * predefined offline defaults.
     *
     * @param discoveredClassNames fully qualified names of JPA entity classes
     * @param settings             custom configuration overrides (e.g.&nbsp;target SQL dialect
     *                             or naming strategies); may be {@code null} or empty
     * @return a parser ready to validate named queries and stored procedures;
     *         the caller is responsible for closing it when finished
     * @since 2.1.0
     */
    public static HibernateNamedObjectParser from(List<String> discoveredClassNames,
                                                  Map<String, Object> settings) {
        Map<String, Object> mergedSettings = HibernateUtil.mergeSettings(
            DEFAULT_SETTINGS,
            settings,
            ENFORCED_SETTINGS
        );

        var ssr = new StandardServiceRegistryBuilder()
            .applySettings(mergedSettings)
            .build();

        try {
            var metadataSources = new MetadataSources(ssr);
            discoveredClassNames.forEach(metadataSources::addAnnotatedClassName);

            var metadata = metadataSources.getMetadataBuilder().build();
            SessionFactoryImplementor sf = (SessionFactoryImplementor) metadata.buildSessionFactory();

            return new HibernateNamedObjectParser(ssr, sf);
        } catch (Exception e) {
            StandardServiceRegistryBuilder.destroy(ssr);
            throw e;
        }
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
