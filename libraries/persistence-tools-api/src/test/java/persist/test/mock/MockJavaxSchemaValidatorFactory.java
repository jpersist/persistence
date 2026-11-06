package persist.test.mock;

import persist.lint.api.spi.SchemaValidator;
import persist.lint.api.spi.SchemaValidatorFactory;

import java.util.List;
import java.util.Map;

public class MockJavaxSchemaValidatorFactory extends MockJavaxPersistenceServiceProvider implements SchemaValidatorFactory {

    public MockJavaxSchemaValidatorFactory() {
    }

    @Override
    public SchemaValidator newSchemaValidator(List<String> discoveredClassNames) {
        return connectionProperties -> {};
    }
}
