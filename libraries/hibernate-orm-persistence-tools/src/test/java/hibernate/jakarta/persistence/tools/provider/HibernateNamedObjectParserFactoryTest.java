package hibernate.jakarta.persistence.tools.provider;

import hibernate.jakarta.persistence.tools.HibernateNamedObjectParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import hibernate.jakarta.persistence.tools.TestEntity;
import persistence.tools.spi.NamedObjectParserFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HibernateNamedObjectParserFactoryTest {

    @Test
    void getSupportedNamespace_returnsJakarta() {
        var factory = new HibernateNamedObjectParserFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void newNamedObjectParser_returnsNonNullInstance() {
        var factory = new HibernateNamedObjectParserFactory();

        var parser = factory.newNamedObjectParser(List.of(TestEntity.class.getName()));

        assertThat(parser).isNotNull().isInstanceOf(HibernateNamedObjectParser.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newNamedObjectParserFactory_withLegacyVersions_throwsIllegalStateException(String version) {
        assertThatThrownBy(() -> NamedObjectParserFactory.newNamedObjectParserFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newNamedObjectParserFactory_withJakartaVersions_returnsNonNullInstance(String version) {
        var factory = NamedObjectParserFactory.newNamedObjectParserFactory(version);

        assertThat(factory).isInstanceOf(HibernateNamedObjectParserFactory.class);
    }

}
