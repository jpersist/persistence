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

import hibernate.jakarta.persistence.tools.provider.HibernateSchemaValidatorFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import persistence.tools.SchemaValidator;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static hibernate.base.persistence.tools.HibernateUtil.*;

/**
 * {@link SchemaValidator} implementation for the Jakarta Persistence
 * namespace ({@code "jakarta"}).
 *
 * <p>Uses Hibernate ORM&nbsp;6 to build the entity metamodel from the
 * discovered annotated classes and validates it against the live database
 * schema reachable through the supplied JDBC connection properties.</p>
 *
 * @since 1.0.0
 * @see HibernateSchemaValidatorFactory
 * @see SchemaValidator
 */
public class HibernateSchemaValidator implements SchemaValidator {

    private static final Map<String, Object> DEFAULT_SETTINGS = Map.of(
        "hibernate.dialect", "org.hibernate.dialect.H2Dialect",
        "hibernate.boot.allow_jdbc_metadata_access", "true"
    );

    /** Fully qualified class names of the JPA entities to validate. */
    private final List<String> discoveredClasses;

    /** Configuration settings supplied at validator creation time. */
    private final Map<String, Object> factorySettings;

    private HibernateSchemaValidator(List<String> discoveredClasses, Map<String, Object> factorySettings) {
        this.discoveredClasses = discoveredClasses;
        this.factorySettings = factorySettings;
    }

    /**
     * Creates a new {@code HibernateSchemaValidator} for the given entity
     * class names and default validator settings.
     *
     * @param discoveredClasses fully qualified class names of the JPA
     *                          entities to register in the metamodel
     * @return a new schema validator instance
     */
    public static HibernateSchemaValidator of(List<String> discoveredClasses) {
        return of(discoveredClasses, Collections.emptyMap());
    }

    /**
     * Creates a new {@code HibernateSchemaValidator} for the given entity
     * class names and custom configuration settings.
     *
     * @param discoveredClasses fully qualified class names of the JPA
     *                          entities to register in the metamodel
     * @param settings          custom configuration overrides (e.g.&nbsp;target SQL dialect,
     *                          naming strategies, or default schema); may be {@code null} or empty
     * @return a new schema validator instance
     * @since 2.1.0
     */
    public static HibernateSchemaValidator of(List<String> discoveredClasses, Map<String, Object> settings) {
        return new HibernateSchemaValidator(discoveredClasses, settings);
    }

    /** {@inheritDoc} */
    @Override
    public void validateSchema(Map<String, String> connectionProperties) {
        Map<String, Object> mergedSettings = mergeSettings(
            DEFAULT_SETTINGS,
            factorySettings,
            toConnectionSettings(connectionProperties)
        );

        try (BootstrapServiceRegistry bsr = buildBootstrapServiceRegistry();
             ServiceRegistry ssr = buildServiceRegistry(bsr, mergedSettings)) {
            // 1. Extract the active metadata model mapped to the unit container
            var metadataSources = new MetadataSources(ssr);

            discoveredClasses.forEach(metadataSources::addAnnotatedClassName);

            var metadata = metadataSources.getMetadataBuilder().build();

            // 2. Resolve the modern SchemaManagementTool from the ServiceRegistry
            var tool = ssr.requireService(SchemaManagementTool.class);
            var validator = tool.getSchemaValidator(mergedSettings);

            // 3. Build execution settings (such as deciding whether to halt on errors)
            var options = SchemaManagementToolCoordinator.buildExecutionOptions(
                mergedSettings,
                ExceptionHandlerHaltImpl.INSTANCE
            );

            // 4. Perform the validation against the live DB schema
            validator.doValidation(metadata, options, ContributableMatcher.ALL);
        }
    }

    // Initialize a database-free Bootstrap Service Registry
    private static BootstrapServiceRegistry buildBootstrapServiceRegistry() {
        return new BootstrapServiceRegistryBuilder().build();
    }

    // Build the standard service registry with tenant connection parameters
    private static ServiceRegistry buildServiceRegistry(BootstrapServiceRegistry bsr, Map<String, Object> mergedSettings) {
        return new StandardServiceRegistryBuilder(bsr)
            .applySettings(mergedSettings)
            .build();
    }

}
