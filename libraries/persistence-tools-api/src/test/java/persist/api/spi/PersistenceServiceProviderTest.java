package persist.api.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersistenceServiceProviderTest {

    @Test
    void getServiceProvider_withUnsupportedVersion_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider("1.0", PersistenceServiceProvider.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void getServiceProvider_withNullVersion_throwsException() {
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider(null, PersistenceServiceProvider.class))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0", "0.0", "99", "abc", ""})
    void getServiceProvider_withVariousInvalidVersions_throwsIllegalArgumentException(String version) {
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider(version, PersistenceServiceProvider.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void getServiceProvider_withLegacyVersions_resolvesJavaxNamespace(String version) {
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider(version, PersistenceServiceProvider.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void getServiceProvider_withJakartaVersions_resolvesJakartaNamespace(String version) {
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider(version, PersistenceServiceProvider.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void getServiceProvider_withLegacyVersions_includesProviderClassName(String version) {
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider(version, PersistenceServiceProvider.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PersistenceServiceProvider");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void getServiceProvider_withJakartaVersions_includesProviderClassName(String version) {
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider(version, PersistenceServiceProvider.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PersistenceServiceProvider");
    }

}
