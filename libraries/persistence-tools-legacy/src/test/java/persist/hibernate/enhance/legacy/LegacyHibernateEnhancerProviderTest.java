package persist.hibernate.enhance.legacy;

import org.junit.jupiter.api.Test;
import persist.api.spi.PersistenceServiceProvider;
import persist.hibernate.enhance.spi.HibernateEnhancerProvider;
import persist.legacy.spi.LegacyPersistenceServiceProvider;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyHibernateEnhancerProviderTest {

    @Test
    void getSupportedNamespace_returnsJavax() {
        var provider = new LegacyHibernateEnhancerProvider();

        assertThat(provider.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void legacyHibernateEnhancerProvider_isPersistenceServiceProvider() {
        var provider = new LegacyHibernateEnhancerProvider();

        assertThat(provider).isInstanceOf(PersistenceServiceProvider.class);
    }

    @Test
    void legacyHibernateEnhancerProvider_isHibernateEnhancerProvider() {
        var provider = new LegacyHibernateEnhancerProvider();

        assertThat(provider).isInstanceOf(HibernateEnhancerProvider.class);
    }

    @Test
    void legacyHibernateEnhancerProvider_isLegacyPersistenceServiceProvider() {
        var provider = new LegacyHibernateEnhancerProvider();

        assertThat(provider).isInstanceOf(LegacyPersistenceServiceProvider.class);
    }

    @Test
    void legacyHibernateEnhancerProvider_extendsLegacyPersistenceServiceProvider() {
        assertThat(LegacyHibernateEnhancerProvider.class.getSuperclass())
            .isEqualTo(LegacyPersistenceServiceProvider.class);
    }

    @Test
    void legacyHibernateEnhancerProvider_implementsHibernateEnhancerProvider() {
        assertThat(HibernateEnhancerProvider.class)
            .isAssignableFrom(LegacyHibernateEnhancerProvider.class);
    }

}
