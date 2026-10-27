package persist.jakarta.spi;

import org.junit.jupiter.api.Test;
import persist.api.spi.PersistenceServiceProvider;
import persist.lint.jakarta.JakartaNamedObjectParserFactory;
import persist.lint.jakarta.JakartaPersistenceLinterFactory;
import persist.lint.jakarta.JakartaSchemaValidatorFactory;

import static org.assertj.core.api.Assertions.assertThat;

class JakartaPersistenceServiceProviderTest {

    @Test
    void getSupportedNamespace_returnsJakarta() {
        var provider = new JakartaNamedObjectParserFactory();

        assertThat(provider.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void jakartaNamedObjectParserFactory_isPersistenceServiceProvider() {
        var provider = new JakartaNamedObjectParserFactory();

        assertThat(provider).isInstanceOf(PersistenceServiceProvider.class);
    }

    @Test
    void jakartaNamedObjectParserFactory_isJakartaPersistenceServiceProvider() {
        var provider = new JakartaNamedObjectParserFactory();

        assertThat(provider).isInstanceOf(JakartaPersistenceServiceProvider.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_isPersistenceServiceProvider() {
        var provider = new JakartaSchemaValidatorFactory();

        assertThat(provider).isInstanceOf(PersistenceServiceProvider.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_isJakartaPersistenceServiceProvider() {
        var provider = new JakartaSchemaValidatorFactory();

        assertThat(provider).isInstanceOf(JakartaPersistenceServiceProvider.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_getSupportedNamespace_returnsJakarta() {
        var provider = new JakartaSchemaValidatorFactory();

        assertThat(provider.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void jakartaPersistenceLinterFactory_extendsJakartaPersistenceServiceProvider() {
        var factory = new JakartaNamedObjectParserFactory();

        assertThat(factory).isInstanceOf(JakartaPersistenceLinterFactory.class);
        assertThat(JakartaPersistenceLinterFactory.class.getSuperclass()).isEqualTo(JakartaPersistenceServiceProvider.class);
    }

}
