package persist.test.mock;

import persist.lint.api.NamedObjectDescriptor;
import persist.lint.api.spi.NamedObjectParser;
import persist.lint.api.spi.NamedObjectParserFactory;

import java.util.List;
import java.util.Set;

public class MockJavaxNamedObjectParserFactory extends MockJavaxPersistenceServiceProvider implements NamedObjectParserFactory {

    public MockJavaxNamedObjectParserFactory() {
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
