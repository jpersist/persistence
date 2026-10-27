package persist.lint.api.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersistenceLinterFactoryTest {

    @Test
    void newPersistenceLinterFactory_withUnsupportedVersion_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> PersistenceLinterFactory.newPersistenceLinterFactory("1.0", NamedObjectParserFactory.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void newPersistenceLinterFactory_withNullVersion_throwsException() {
        assertThatThrownBy(() -> PersistenceLinterFactory.newPersistenceLinterFactory(null, NamedObjectParserFactory.class))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0", "0.0", "99", "abc", ""})
    void newPersistenceLinterFactory_withVariousInvalidVersions_throwsIllegalArgumentException(String version) {
        assertThatThrownBy(() -> PersistenceLinterFactory.newPersistenceLinterFactory(version, NamedObjectParserFactory.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newPersistenceLinterFactory_withLegacyVersions_resolvesJavaxNamespace(String version) {
        assertThatThrownBy(() -> PersistenceLinterFactory.newPersistenceLinterFactory(version, NamedObjectParserFactory.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newPersistenceLinterFactory_withJakartaVersions_resolvesJakartaNamespace(String version) {
        assertThatThrownBy(() -> PersistenceLinterFactory.newPersistenceLinterFactory(version, NamedObjectParserFactory.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

    @Test
    void newNamedObjectParserFactory_delegatesToGenericMethod() {
        assertThatThrownBy(() -> PersistenceLinterFactory.newNamedObjectParserFactory("1.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void newSchemaValidatorFactory_delegatesToGenericMethod() {
        assertThatThrownBy(() -> PersistenceLinterFactory.newSchemaValidatorFactory("1.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newSchemaValidatorFactory_withLegacyVersions_resolvesJavaxNamespace(String version) {
        assertThatThrownBy(() -> PersistenceLinterFactory.newSchemaValidatorFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newSchemaValidatorFactory_withJakartaVersions_resolvesJakartaNamespace(String version) {
        assertThatThrownBy(() -> PersistenceLinterFactory.newSchemaValidatorFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

}
