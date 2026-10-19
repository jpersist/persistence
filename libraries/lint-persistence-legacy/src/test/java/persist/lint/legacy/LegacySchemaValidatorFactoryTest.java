package persist.lint.legacy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import persist.lint.api.spi.SchemaValidatorFactory;
import persist.lint.query.legacy.TestEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LegacySchemaValidatorFactoryTest {

    @Test
    void getSupportedNamespace_returnsJavax() {
        var factory = new LegacySchemaValidatorFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void newSchemaValidator_returnsNonNullInstance() {
        var factory = new LegacySchemaValidatorFactory();

        var validator = factory.newSchemaValidator(List.of(TestEntity.class.getName()));

        assertThat(validator).isNotNull().isInstanceOf(LegacySchemaValidator.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newSchemaValidatorFactory_withLegacyVersions_returnsNonNullInstance(String version) {
        var factory = SchemaValidatorFactory.newSchemaValidatorFactory(version);

        assertThat(factory).isInstanceOf(LegacySchemaValidatorFactory.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newSchemaValidatorFactory_withJakartaVersions_throwsIllegalStateException(String version) {
        assertThatThrownBy(() -> SchemaValidatorFactory.newSchemaValidatorFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

}
