package persistence.test.mock;

import persistence.tools.spi.PersistenceServiceProvider;

public class MockJavaxPersistenceServiceProvider implements PersistenceServiceProvider {

    public MockJavaxPersistenceServiceProvider() {
    }

    @Override
    public String getSupportedNamespace() {
        return "javax";
    }
}
