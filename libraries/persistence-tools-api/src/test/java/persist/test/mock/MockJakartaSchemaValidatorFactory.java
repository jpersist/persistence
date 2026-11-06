package persist.test.mock;

import persist.lint.api.spi.SchemaValidator;
import persist.lint.api.spi.SchemaValidatorFactory;

import java.util.List;
import java.util.Map;

public class MockJakartaSchemaValidatorFactory extends MockJakartaPersistenceServiceProvider implements SchemaValidatorFactory {

    public MockJakartaSchemaValidatorFactory() {
    }

    @Override
    public SchemaValidator newSchemaValidator(List<String> discoveredClassNames) {
        return connectionProperties -> {};
    }
}
