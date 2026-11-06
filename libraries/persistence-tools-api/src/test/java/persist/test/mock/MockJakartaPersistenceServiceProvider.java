package persist.test.mock;

import persist.api.spi.PersistenceServiceProvider;

public class MockJakartaPersistenceServiceProvider implements PersistenceServiceProvider {

    public MockJakartaPersistenceServiceProvider() {
    }

    @Override
    public String getSupportedNamespace() {
        return "jakarta";
    }
}
