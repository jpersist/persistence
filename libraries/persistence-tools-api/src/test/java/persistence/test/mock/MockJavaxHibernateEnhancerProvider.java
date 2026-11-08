package persistence.test.mock;

import persistence.tools.Enhancer;
import persistence.tools.spi.EnhancerProvider;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class MockJavaxHibernateEnhancerProvider extends MockJavaxPersistenceServiceProvider implements EnhancerProvider {

    public MockJavaxHibernateEnhancerProvider() {
    }

    @Override
    public Enhancer getEnhancer(File classesDir, Set<File> classpathFiles, Map<Enhancer.Option, Boolean> options) {
        return (className, classBytes) -> classBytes;
    }
}
