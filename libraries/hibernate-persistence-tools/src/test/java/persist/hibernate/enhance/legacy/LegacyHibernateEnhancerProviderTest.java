package persist.hibernate.enhance.legacy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import persist.api.spi.PersistenceServiceProvider;
import persist.hibernate.enhance.spi.HibernateEnhancerProvider;
import persist.legacy.spi.LegacyPersistenceServiceProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void getHibernateEnhancerProvider_withLegacyVersions_returnsNonNullInstance(String version) {
        var provider = HibernateEnhancerProvider.getHibernateEnhancerProvider(version);

        assertThat(provider).isInstanceOf(LegacyHibernateEnhancerProvider.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void getHibernateEnhancerProvider_withJakartaVersions_throwsIllegalStateException(String version) {
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

}
