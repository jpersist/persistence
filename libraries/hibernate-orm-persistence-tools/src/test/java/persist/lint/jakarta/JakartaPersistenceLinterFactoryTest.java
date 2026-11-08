package persist.lint.jakarta;

import org.junit.jupiter.api.Test;
import persist.lint.api.spi.PersistenceLinterFactory;

import static org.assertj.core.api.Assertions.assertThat;

class JakartaPersistenceLinterFactoryTest {

    @Test
    void getSupportedNamespace_returnsJakarta() {
        var factory = new JakartaNamedObjectParserFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void jakartaNamedObjectParserFactory_isPersistenceLinterFactory() {
        var factory = new JakartaNamedObjectParserFactory();

        assertThat(factory).isInstanceOf(PersistenceLinterFactory.class);
    }

    @Test
    void jakartaNamedObjectParserFactory_isJakartaPersistenceLinterFactory() {
        var factory = new JakartaNamedObjectParserFactory();

        assertThat(factory).isInstanceOf(JakartaPersistenceLinterFactory.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_isPersistenceLinterFactory() {
        var factory = new JakartaSchemaValidatorFactory();

        assertThat(factory).isInstanceOf(PersistenceLinterFactory.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_isJakartaPersistenceLinterFactory() {
        var factory = new JakartaSchemaValidatorFactory();

        assertThat(factory).isInstanceOf(JakartaPersistenceLinterFactory.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_getSupportedNamespace_returnsJakarta() {
        var factory = new JakartaSchemaValidatorFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("jakarta");
    }

}
