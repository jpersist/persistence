package persist.lint.legacy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import persist.lint.api.spi.NamedObjectParserFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LegacyNamedObjectParserFactoryTest {

    @Test
    void getSupportedNamespace_returnsJavax() {
        var factory = new LegacyNamedObjectParserFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void newNamedObjectParser_returnsNonNullInstance() {
        var factory = new LegacyNamedObjectParserFactory();

        var parser = factory.newNamedObjectParser(List.of(TestEntity.class.getName()));

        assertThat(parser).isNotNull().isInstanceOf(LegacyNamedObjectParser.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newNamedObjectParserFactory_withLegacyVersions_returnsNonNullInstance(String version) {
        var factory = NamedObjectParserFactory.newNamedObjectParserFactory(version);

        assertThat(factory).isInstanceOf(LegacyNamedObjectParserFactory.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newNamedObjectParserFactory_withJakartaVersions_throwsIllegalStateException(String version) {
        assertThatThrownBy(() -> NamedObjectParserFactory.newNamedObjectParserFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

}
