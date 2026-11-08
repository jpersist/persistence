package persist.hibernate.enhance.jakarta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import persist.api.spi.PersistenceServiceProvider;
import persist.hibernate.enhance.spi.HibernateEnhancerProvider;
import persist.jakarta.spi.JakartaPersistenceServiceProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void getHibernateEnhancerProvider_withLegacyVersions_throwsIllegalStateException(String version) {
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void getHibernateEnhancerProvider_withJakartaVersions_returnsNonNullInstance(String version) {
        var provider = HibernateEnhancerProvider.getHibernateEnhancerProvider(version);

        assertThat(provider).isInstanceOf(JakartaHibernateEnhancerProvider.class);
    }

}
