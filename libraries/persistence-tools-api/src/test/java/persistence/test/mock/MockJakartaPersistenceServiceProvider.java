package persistence.test.mock;

import persistence.tools.spi.PersistenceServiceProvider;

public class MockJakartaPersistenceServiceProvider implements PersistenceServiceProvider {

    public MockJakartaPersistenceServiceProvider() {
    }

    @Override
    public String getSupportedNamespace() {
        return "jakarta";
    }
}
