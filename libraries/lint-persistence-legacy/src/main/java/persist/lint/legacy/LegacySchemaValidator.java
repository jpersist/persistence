package persist.lint.legacy;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import persist.lint.api.spi.SchemaValidator;

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
 * @since 2.1.0
 * @see LegacySchemaValidatorFactory
 * @see SchemaValidator
 */
public class LegacySchemaValidator implements SchemaValidator {

    /** Fully qualified class names of the JPA entities to validate. */
    private final List<String> discoveredClasses;

    private LegacySchemaValidator(List<String> discoveredClasses) {
        this.discoveredClasses = discoveredClasses;
    }

    /**
     * Creates a new {@code LegacySchemaValidator} for the given entity
     * class names.
     *
     * @param discoveredClasses fully qualified class names of the JPA
     *                          entities to register in the metamodel
     * @return a new schema validator instance
     */
    public static LegacySchemaValidator of(List<String> discoveredClasses) {
        return new LegacySchemaValidator(discoveredClasses);
    }

    /** {@inheritDoc} */
    @Override
    public void validateSchema(Map<String, String> connectionProperties) {
        var bsr = new BootstrapServiceRegistryBuilder().build();

        var ssr = new StandardServiceRegistryBuilder(bsr)
                .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                .applySetting("hibernate.connection.driver_class", connectionProperties.get("driver"))
                .applySetting("hibernate.connection.url", connectionProperties.get("url"))
                .applySetting("hibernate.connection.username", connectionProperties.get("username"))
                .applySetting("hibernate.connection.password", connectionProperties.get("password"))
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "true")
                .build();

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
