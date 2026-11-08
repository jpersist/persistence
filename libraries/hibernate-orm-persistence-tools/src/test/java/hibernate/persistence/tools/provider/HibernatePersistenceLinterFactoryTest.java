package hibernate.persistence.tools.provider;

import org.junit.jupiter.api.Test;
import persistence.tools.spi.PersistenceLinterFactory;

import static org.assertj.core.api.Assertions.assertThat;

class HibernatePersistenceLinterFactoryTest {

    @Test
    void getSupportedNamespace_returnsJakarta() {
        var factory = new HibernateNamedObjectParserFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void jakartaNamedObjectParserFactory_isPersistenceLinterFactory() {
        var factory = new HibernateNamedObjectParserFactory();

        assertThat(factory).isInstanceOf(PersistenceLinterFactory.class);
    }

    @Test
    void jakartaNamedObjectParserFactory_isJakartaPersistenceLinterFactory() {
        var factory = new HibernateNamedObjectParserFactory();

        assertThat(factory).isInstanceOf(HibernatePersistenceLinterFactory.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_isPersistenceLinterFactory() {
        var factory = new HibernateSchemaValidatorFactory();

        assertThat(factory).isInstanceOf(PersistenceLinterFactory.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_isJakartaPersistenceLinterFactory() {
        var factory = new HibernateSchemaValidatorFactory();

        assertThat(factory).isInstanceOf(HibernatePersistenceLinterFactory.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_getSupportedNamespace_returnsJakarta() {
        var factory = new HibernateSchemaValidatorFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("jakarta");
    }

}
