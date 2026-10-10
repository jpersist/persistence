package persist.lint.query.jakarta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import persist.lint.query.spi.QueryParserFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JakartaQueryParserFactoryTest {

    @Test
    void getSupportedNamespace_returnsJakarta() {
        var factory = new JakartaQueryParserFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void newQueryParser_returnsNonNullInstance() {
        var factory = new JakartaQueryParserFactory();

        var parser = factory.newQueryParser(List.of(TestEntity.class.getName()));

        assertThat(parser).isNotNull().isInstanceOf(JakartaQueryParser.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newQueryParserFactory_withLegacyVersions_throwsIllegalStateException(String version) {
        assertThatThrownBy(() -> QueryParserFactory.newQueryParserFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newQueryParserFactory_withJakartaVersions_returnsNonNullInstance(String version) {
        var factory = QueryParserFactory.newQueryParserFactory(version);

        assertThat(factory).isInstanceOf(JakartaQueryParserFactory.class);
    }

}
