package persist.lint.query.legacy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import persist.lint.query.spi.QueryParserFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LegacyQueryParserFactoryTest {

    @Test
    void getSupportedNamespace_returnsJavax() {
        var factory = new LegacyQueryParserFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void newQueryParser_returnsNonNullInstance() {
        var factory = new LegacyQueryParserFactory();

        var parser = factory.newQueryParser(List.of(TestEntity.class.getName()));

        assertThat(parser).isNotNull().isInstanceOf(LegacyQueryParser.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newQueryParserFactory_withLegacyVersions_returnsNonNullInstance(String version) {
        var factory = QueryParserFactory.newQueryParserFactory(version);

        assertThat(factory).isInstanceOf(LegacyQueryParserFactory.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newQueryParserFactory_withJakartaVersions_throwsIllegalStateException(String version) {
        assertThatThrownBy(() -> QueryParserFactory.newQueryParserFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

}
