package persist.test.mock;

import persist.api.spi.PersistenceServiceProvider;

public class MockJavaxPersistenceServiceProvider implements PersistenceServiceProvider {

    public MockJavaxPersistenceServiceProvider() {
    }

    @Override
    public String getSupportedNamespace() {
        return "javax";
    }
}
