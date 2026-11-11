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

import hibernate.javax.persistence.tools.provider.HibernateSchemaValidatorFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import persistence.tools.SchemaValidator;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * {@link SchemaValidator} implementation for the legacy
 * {@code javax.persistence} namespace ({@code "javax"}).
 *
 * <p>Uses Hibernate&nbsp;5 to build the entity metamodel from the
 * discovered annotated classes and validates it against the live database
 * schema reachable through the supplied JDBC connection properties.</p>
 *
 * @since 1.0.0
 * @see HibernateSchemaValidatorFactory
 * @see SchemaValidator
 */
public class HibernateSchemaValidator implements SchemaValidator {

    /** Fully qualified class names of the JPA entities to validate. */
    private final List<String> discoveredClasses;

    private HibernateSchemaValidator(List<String> discoveredClasses) {
        this.discoveredClasses = discoveredClasses;
    }

    /**
     * Creates a new {@code HibernateSchemaValidator} for the given entity
     * class names.
     *
     * @param discoveredClasses fully qualified class names of the JPA
     *                          entities to register in the metamodel
     * @return a new schema validator instance
     */
    public static HibernateSchemaValidator of(List<String> discoveredClasses) {
        return new HibernateSchemaValidator(discoveredClasses);
    }

    /** {@inheritDoc} */
    @Override
    public void validateSchema(Map<String, String> connectionProperties) {
        try (BootstrapServiceRegistry bsr = buildBootstrapServiceRegistry();
             ServiceRegistry ssr = buildServiceRegistry(bsr, connectionProperties)) {
            var metadataSources = new MetadataSources(ssr);
            discoveredClasses.forEach(metadataSources::addAnnotatedClassName);

            var metadata = metadataSources.buildMetadata();

            // 1. Retrieve the SchemaManagementTool from the Service Registry
            var tool = ssr.getService(SchemaManagementTool.class);

            // 2. Fetch the SchemaValidator engine
            var validator = tool.getSchemaValidator(Collections.emptyMap());

            // 3. Build execution settings (Halts execution and throws an exception on failure)
            var options = SchemaManagementToolCoordinator.buildExecutionOptions(
                Collections.emptyMap(),
                ExceptionHandlerHaltImpl.INSTANCE
            );

            // 4. Perform the validation (Note: No ContributableMatcher parameter in v5)
            validator.doValidation(metadata, options);
        }
    }

    private static BootstrapServiceRegistry buildBootstrapServiceRegistry() {
        return new BootstrapServiceRegistryBuilder().build();
    }

    private static ServiceRegistry buildServiceRegistry(BootstrapServiceRegistry bsr, Map<String, String> connectionProperties) {
        return new StandardServiceRegistryBuilder(bsr)
            .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
            .applySetting("hibernate.connection.driver_class", connectionProperties.get("driver"))
            .applySetting("hibernate.connection.url", connectionProperties.get("url"))
            .applySetting("hibernate.connection.username", connectionProperties.get("username"))
            .applySetting("hibernate.connection.password", connectionProperties.get("password"))
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", "true")
            .build();
    }

}
