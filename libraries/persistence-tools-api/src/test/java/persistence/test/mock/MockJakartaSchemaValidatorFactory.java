package persistence.test.mock;

import persistence.tools.SchemaValidator;
import persistence.tools.spi.SchemaValidatorFactory;

import java.util.List;

public class MockJakartaSchemaValidatorFactory extends MockJakartaPersistenceServiceProvider implements SchemaValidatorFactory {

    public MockJakartaSchemaValidatorFactory() {
    }

    @Override
    public SchemaValidator newSchemaValidator(List<String> discoveredClassNames) {
        return connectionProperties -> {};
    }
}
