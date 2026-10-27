package persist.hibernate.enhance.jakarta;

import org.junit.jupiter.api.Test;
import persist.api.spi.PersistenceServiceProvider;
import persist.hibernate.enhance.spi.HibernateEnhancerProvider;
import persist.jakarta.spi.JakartaPersistenceServiceProvider;

import static org.assertj.core.api.Assertions.assertThat;

class JakartaHibernateEnhancerProviderTest {

    @Test
    void getSupportedNamespace_returnsJakarta() {
        var provider = new JakartaHibernateEnhancerProvider();

        assertThat(provider.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void jakartaHibernateEnhancerProvider_isPersistenceServiceProvider() {
        var provider = new JakartaHibernateEnhancerProvider();

        assertThat(provider).isInstanceOf(PersistenceServiceProvider.class);
    }

    @Test
    void jakartaHibernateEnhancerProvider_isHibernateEnhancerProvider() {
        var provider = new JakartaHibernateEnhancerProvider();

        assertThat(provider).isInstanceOf(HibernateEnhancerProvider.class);
    }

    @Test
    void jakartaHibernateEnhancerProvider_isJakartaPersistenceServiceProvider() {
        var provider = new JakartaHibernateEnhancerProvider();

        assertThat(provider).isInstanceOf(JakartaPersistenceServiceProvider.class);
    }

    @Test
    void jakartaHibernateEnhancerProvider_extendsJakartaPersistenceServiceProvider() {
        assertThat(JakartaHibernateEnhancerProvider.class.getSuperclass())
            .isEqualTo(JakartaPersistenceServiceProvider.class);
    }

    @Test
    void jakartaHibernateEnhancerProvider_implementsHibernateEnhancerProvider() {
        assertThat(HibernateEnhancerProvider.class)
            .isAssignableFrom(JakartaHibernateEnhancerProvider.class);
    }

}
