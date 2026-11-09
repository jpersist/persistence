package persistence.test.mock;

import persistence.tools.Enhancer;
import persistence.tools.spi.EnhancerProvider;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MockJavaxEnhancerProvider extends MockJavaxPersistenceServiceProvider implements EnhancerProvider {

    public MockJavaxEnhancerProvider() {
    }

    @Override
    public Enhancer getEnhancer(Collection<Set<File>> classpathFilesCollection, Map<String, Object> settings) {
        return (className, classBytes) -> classBytes;
    }
}
