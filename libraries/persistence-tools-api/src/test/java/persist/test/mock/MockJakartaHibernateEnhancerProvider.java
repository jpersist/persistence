package persist.test.mock;

import persist.hibernate.enhance.spi.HibernateEnhancer;
import persist.hibernate.enhance.spi.HibernateEnhancerProvider;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class MockJakartaHibernateEnhancerProvider extends MockJakartaPersistenceServiceProvider implements HibernateEnhancerProvider {

    public MockJakartaHibernateEnhancerProvider() {
    }

    @Override
    public HibernateEnhancer getEnhancer(File classesDir, Set<File> classpathFiles, Map<HibernateEnhancer.Option, Boolean> options) {
        return (className, classBytes) -> classBytes;
    }
}
