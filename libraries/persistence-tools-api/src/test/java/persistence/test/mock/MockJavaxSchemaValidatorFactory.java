package persistence.test.mock;

import persistence.tools.SchemaValidator;
import persistence.tools.spi.SchemaValidatorFactory;

import java.util.List;

public class MockJavaxSchemaValidatorFactory extends MockJavaxPersistenceServiceProvider implements SchemaValidatorFactory {

    public MockJavaxSchemaValidatorFactory() {
    }

    @Override
    public SchemaValidator newSchemaValidator(List<String> discoveredClassNames) {
        return connectionProperties -> {};
    }
}
