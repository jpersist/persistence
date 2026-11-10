package hibernate.javax.persistence.tools.provider;

import org.junit.jupiter.api.Test;
import persistence.tools.spi.PersistenceLinterFactory;

import static org.assertj.core.api.Assertions.assertThat;

class HibernatePersistenceLinterFactoryTest {

    @Test
    void getSupportedNamespace_returnsJavax() {
        var factory = new HibernateNamedObjectParserFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void legacyNamedObjectParserFactory_isPersistenceLinterFactory() {
        var factory = new HibernateNamedObjectParserFactory();

        assertThat(factory).isInstanceOf(PersistenceLinterFactory.class);
    }

    @Test
    void legacyNamedObjectParserFactory_isLegacyPersistenceLinterFactory() {
        var factory = new HibernateNamedObjectParserFactory();

        assertThat(factory).isInstanceOf(HibernatePersistenceLinterFactory.class);
    }

    @Test
    void legacySchemaValidatorFactory_isPersistenceLinterFactory() {
        var factory = new HibernateSchemaValidatorFactory();

        assertThat(factory).isInstanceOf(PersistenceLinterFactory.class);
    }

    @Test
    void legacySchemaValidatorFactory_isLegacyPersistenceLinterFactory() {
        var factory = new HibernateSchemaValidatorFactory();

        assertThat(factory).isInstanceOf(HibernatePersistenceLinterFactory.class);
    }

    @Test
    void legacySchemaValidatorFactory_getSupportedNamespace_returnsJavax() {
        var factory = new HibernateSchemaValidatorFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("javax");
    }

}
