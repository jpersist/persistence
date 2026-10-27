package persist.hibernate.enhance.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HibernateEnhancerProviderTest {

    @Test
    void getHibernateEnhancerProvider_withUnsupportedVersion_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider("1.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void getHibernateEnhancerProvider_withNullVersion_throwsException() {
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(null))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0", "0.0", "99", "abc", ""})
    void getHibernateEnhancerProvider_withVariousInvalidVersions_throwsIllegalArgumentException(String version) {
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(version))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void getHibernateEnhancerProvider_withLegacyVersions_resolvesJavaxNamespace(String version) {
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void getHibernateEnhancerProvider_withJakartaVersions_resolvesJakartaNamespace(String version) {
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void getHibernateEnhancerProvider_withLegacyVersions_includesProviderClassName(String version) {
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("HibernateEnhancerProvider");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void getHibernateEnhancerProvider_withJakartaVersions_includesProviderClassName(String version) {
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("HibernateEnhancerProvider");
    }

}
