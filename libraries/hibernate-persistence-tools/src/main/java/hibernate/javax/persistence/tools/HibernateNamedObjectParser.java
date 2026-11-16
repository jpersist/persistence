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

package hibernate.javax.persistence.tools;

import hibernate.base.persistence.tools.HibernateUtil;
import hibernate.javax.persistence.tools.internal.NoOpConnectionProvider;
import hibernate.javax.persistence.tools.provider.HibernateNamedObjectParserFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.service.ServiceRegistry;
import persistence.tools.NamedObjectDescriptor;
import persistence.tools.NamedObjectParser;

import java.util.*;

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
        // Provide a baseline dialect so the HQL compiler understands standard function mappings
        "hibernate.dialect", "org.hibernate.dialect.H2Dialect",
        // Hibernate 5 flag to block background JDBC metadata connection lookups
        "hibernate.temp.use_jdbc_metadata_defaults", "false",
        // Suppress HHH000181 connection provider info logs during offline bootstrap
        "hibernate.connection.provider_class", NoOpConnectionProvider.class.getName(),
        // Enforce strict compliance with JPA 2.2 spec rules by default
        "hibernate.jpa.compliance.query", "true",
        // Bypass runtime bytecode enhancement routines during offline linting
        "hibernate.bytecode.provider", "none"
    );

    private static final Map<String, Object> ENFORCED_SETTINGS = Map.of(
        // Guarantee automatic database schema actions remain disabled during query linting
        "hibernate.hbm2ddl.auto", "none"
    );

    private final ServiceRegistry serviceRegistry;
    private final SessionFactoryImplementor sessionFactory;
    private final QueryTranslatorFactory translatorFactory;

    private HibernateNamedObjectParser(ServiceRegistry serviceRegistry, SessionFactoryImplementor sessionFactory) {
        this.serviceRegistry = serviceRegistry;
        this.sessionFactory = sessionFactory;
        this.translatorFactory = new ASTQueryTranslatorFactory();
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
     * @since 1.1.0
     */
    public static HibernateNamedObjectParser from(List<String> discoveredClassNames,
                                                  Map<String, Object> settings) {
        Map<String, Object> mergedSettings = HibernateUtil.mergeSettings(
            DEFAULT_SETTINGS,
            settings,
            ENFORCED_SETTINGS
        );

        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
            .applySettings(mergedSettings)
            .build();

        try {
            var metadataSources = new MetadataSources(serviceRegistry);
            discoveredClassNames.forEach(metadataSources::addAnnotatedClassName);

            var metadata = metadataSources.getMetadataBuilder().build();
            SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();
            return new HibernateNamedObjectParser(serviceRegistry, sessionFactory);
        } catch (Exception e) {
            StandardServiceRegistryBuilder.destroy(serviceRegistry);
            throw e;
        }
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
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        sessionFactory.close();
        StandardServiceRegistryBuilder.destroy(serviceRegistry);
    }

}
