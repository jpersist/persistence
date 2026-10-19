package persist.lint.jakarta;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import persist.lint.api.spi.SchemaValidator;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * {@link SchemaValidator} implementation for the Jakarta Persistence
 * namespace ({@code "jakarta"}).
 *
 * <p>Uses Hibernate ORM&nbsp;6 to build the entity metamodel from the
 * discovered annotated classes and validates it against the live database
 * schema reachable through the supplied JDBC connection properties.</p>
 *
 * @since 2.1.0
 * @see JakartaSchemaValidatorFactory
 * @see SchemaValidator
 */
public class JakartaSchemaValidator implements SchemaValidator {

    /** Fully qualified class names of the JPA entities to validate. */
    private final List<String> discoveredClasses;

    private JakartaSchemaValidator(List<String> discoveredClasses) {
        this.discoveredClasses = discoveredClasses;
    }

    /**
     * Creates a new {@code JakartaSchemaValidator} for the given entity
     * class names.
     *
     * @param discoveredClasses fully qualified class names of the JPA
     *                          entities to register in the metamodel
     * @return a new schema validator instance
     */
    public static JakartaSchemaValidator of(List<String> discoveredClasses) {
        return new JakartaSchemaValidator(discoveredClasses);
    }

    /** {@inheritDoc} */
    @Override
    public void validateSchema(Map<String, String> connectionProperties) {
        // 1. Initialize a database-free Bootstrap Service Registry
        var bsr = new BootstrapServiceRegistryBuilder().build();

        // 2. Build the standard service registry with tenant connection parameters
        var ssr = new StandardServiceRegistryBuilder(bsr)
                .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                .applySetting("hibernate.connection.driver_class", connectionProperties.get("driver"))
                .applySetting("hibernate.connection.url", connectionProperties.get("url"))
                .applySetting("hibernate.connection.username", connectionProperties.get("username"))
                .applySetting("hibernate.connection.password", connectionProperties.get("password"))
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "true")
                .build();

        // 3. Extract the active metadata model mapped to the unit container
        var metadataSources = new MetadataSources(ssr);

        discoveredClasses.forEach(metadataSources::addAnnotatedClassName);

        var metadata = metadataSources.getMetadataBuilder().build();

        // 4. Resolve the modern SchemaManagementTool from the ServiceRegistry
        var tool = ssr.requireService(SchemaManagementTool.class);
        var validator = tool.getSchemaValidator(Collections.emptyMap());

        // 5. Build execution settings (such as deciding whether to halt on errors)
        var options = SchemaManagementToolCoordinator.buildExecutionOptions(
                Collections.emptyMap(),
                ExceptionHandlerHaltImpl.INSTANCE
        );

        // 6. Perform the validation against the live DB schema
        validator.doValidation(metadata, options, ContributableMatcher.ALL);
    }

}
