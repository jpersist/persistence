package persistence.test.mock;

import persistence.tools.NamedObjectDescriptor;
import persistence.tools.NamedObjectParser;
import persistence.tools.spi.NamedObjectParserFactory;

import java.util.List;
import java.util.Set;

public class MockJakartaNamedObjectParserFactory extends MockJakartaPersistenceServiceProvider implements NamedObjectParserFactory {

    public MockJakartaNamedObjectParserFactory() {
    }

    @Override
    public NamedObjectParser newNamedObjectParser(List<String> discoveredClassNames) {
        return new NamedObjectParser() {
            @Override
            public NamedObjectDescriptor parseQuery(String name, String queryString) {
                return null;
            }

            @Override
            public NamedObjectDescriptor parseStoredProcedure(String name, String procedureName, Set<String> parameterNames) {
                return null;
            }

            @Override
            public void close() {
            }
        };
    }
}
