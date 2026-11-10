package hibernate.jakarta.persistence.tools.provider;

import org.junit.jupiter.api.Test;
import persistence.tools.spi.PersistenceServiceProvider;

import static org.assertj.core.api.Assertions.assertThat;

class HibernatePersistenceServiceProviderTest {

    @Test
    void getSupportedNamespace_returnsJakarta() {
        var provider = new HibernateNamedObjectParserFactory();

        assertThat(provider.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void jakartaNamedObjectParserFactory_isPersistenceServiceProvider() {
        var provider = new HibernateNamedObjectParserFactory();

        assertThat(provider).isInstanceOf(PersistenceServiceProvider.class);
    }

    @Test
    void jakartaNamedObjectParserFactory_isJakartaPersistenceServiceProvider() {
        var provider = new HibernateNamedObjectParserFactory();

        assertThat(provider).isInstanceOf(HibernatePersistenceServiceProvider.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_isPersistenceServiceProvider() {
        var provider = new HibernateSchemaValidatorFactory();

        assertThat(provider).isInstanceOf(PersistenceServiceProvider.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_isJakartaPersistenceServiceProvider() {
        var provider = new HibernateSchemaValidatorFactory();

        assertThat(provider).isInstanceOf(HibernatePersistenceServiceProvider.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_getSupportedNamespace_returnsJakarta() {
        var provider = new HibernateSchemaValidatorFactory();

        assertThat(provider.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void jakartaPersistenceLinterFactory_extendsJakartaPersistenceServiceProvider() {
        var factory = new HibernateNamedObjectParserFactory();

        assertThat(factory).isInstanceOf(HibernatePersistenceLinterFactory.class);
        assertThat(HibernatePersistenceLinterFactory.class.getSuperclass()).isEqualTo(HibernatePersistenceServiceProvider.class);
    }

}
