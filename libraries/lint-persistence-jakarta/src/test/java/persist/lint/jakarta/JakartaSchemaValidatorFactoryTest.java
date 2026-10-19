package persist.lint.jakarta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import persist.lint.api.spi.SchemaValidatorFactory;
import persist.lint.query.jakarta.TestEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JakartaSchemaValidatorFactoryTest {

    @Test
    void getSupportedNamespace_returnsJakarta() {
        var factory = new JakartaSchemaValidatorFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void newSchemaValidator_returnsNonNullInstance() {
        var factory = new JakartaSchemaValidatorFactory();

        var validator = factory.newSchemaValidator(List.of(TestEntity.class.getName()));

        assertThat(validator).isNotNull().isInstanceOf(JakartaSchemaValidator.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newSchemaValidatorFactory_withLegacyVersions_throwsIllegalStateException(String version) {
        assertThatThrownBy(() -> SchemaValidatorFactory.newSchemaValidatorFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newSchemaValidatorFactory_withJakartaVersions_returnsNonNullInstance(String version) {
        var factory = SchemaValidatorFactory.newSchemaValidatorFactory(version);

        assertThat(factory).isInstanceOf(JakartaSchemaValidatorFactory.class);
    }

}
