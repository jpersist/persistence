package persist.lint.legacy;

import org.junit.jupiter.api.Test;
import persist.lint.api.spi.PersistenceLinterFactory;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyPersistenceLinterFactoryTest {

    @Test
    void getSupportedNamespace_returnsJavax() {
        var factory = new LegacyNamedObjectParserFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void legacyNamedObjectParserFactory_isPersistenceLinterFactory() {
        var factory = new LegacyNamedObjectParserFactory();

        assertThat(factory).isInstanceOf(PersistenceLinterFactory.class);
    }

    @Test
    void legacyNamedObjectParserFactory_isLegacyPersistenceLinterFactory() {
        var factory = new LegacyNamedObjectParserFactory();

        assertThat(factory).isInstanceOf(LegacyPersistenceLinterFactory.class);
    }

    @Test
    void legacySchemaValidatorFactory_isPersistenceLinterFactory() {
        var factory = new LegacySchemaValidatorFactory();

        assertThat(factory).isInstanceOf(PersistenceLinterFactory.class);
    }

    @Test
    void legacySchemaValidatorFactory_isLegacyPersistenceLinterFactory() {
        var factory = new LegacySchemaValidatorFactory();

        assertThat(factory).isInstanceOf(LegacyPersistenceLinterFactory.class);
    }

    @Test
    void legacySchemaValidatorFactory_getSupportedNamespace_returnsJavax() {
        var factory = new LegacySchemaValidatorFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("javax");
    }

}
