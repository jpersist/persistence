package hibernate.persistence.tools.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import persistence.tools.spi.EnhancerProvider;
import persistence.tools.spi.PersistenceServiceProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HibernateEnhancerProviderTest {

    @Test
    void getSupportedNamespace_returnsJavax() {
        var provider = new HibernateEnhancerProvider();

        assertThat(provider.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void legacyHibernateEnhancerProvider_isPersistenceServiceProvider() {
        var provider = new HibernateEnhancerProvider();

        assertThat(provider).isInstanceOf(PersistenceServiceProvider.class);
    }

    @Test
    void legacyHibernateEnhancerProvider_isHibernateEnhancerProvider() {
        var provider = new HibernateEnhancerProvider();

        assertThat(provider).isInstanceOf(EnhancerProvider.class);
    }

    @Test
    void legacyHibernateEnhancerProvider_isLegacyPersistenceServiceProvider() {
        var provider = new HibernateEnhancerProvider();

        assertThat(provider).isInstanceOf(HibernatePersistenceServiceProvider.class);
    }

    @Test
    void legacyHibernateEnhancerProvider_extendsLegacyPersistenceServiceProvider() {
        assertThat(HibernateEnhancerProvider.class.getSuperclass())
            .isEqualTo(HibernatePersistenceServiceProvider.class);
    }

    @Test
    void legacyHibernateEnhancerProvider_implementsHibernateEnhancerProvider() {
        assertThat(EnhancerProvider.class)
            .isAssignableFrom(HibernateEnhancerProvider.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void getHibernateEnhancerProvider_withLegacyVersions_returnsNonNullInstance(String version) {
        var provider = EnhancerProvider.getEnhancerProvider(version);

        assertThat(provider).isInstanceOf(HibernateEnhancerProvider.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void getHibernateEnhancerProvider_withJakartaVersions_throwsIllegalStateException(String version) {
        assertThatThrownBy(() -> EnhancerProvider.getEnhancerProvider(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

}
