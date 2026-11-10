package hibernate.javax.persistence.tools.provider;

import org.junit.jupiter.api.Test;
import persistence.tools.spi.PersistenceServiceProvider;

import static org.assertj.core.api.Assertions.assertThat;

class HibernatePersistenceServiceProviderTest {

    @Test
    void getSupportedNamespace_returnsJavax() {
        var provider = new HibernateNamedObjectParserFactory();

        assertThat(provider.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void legacyNamedObjectParserFactory_isPersistenceServiceProvider() {
        var provider = new HibernateNamedObjectParserFactory();

        assertThat(provider).isInstanceOf(PersistenceServiceProvider.class);
    }

    @Test
    void legacySchemaValidatorFactory_isPersistenceServiceProvider() {
        var provider = new HibernateSchemaValidatorFactory();

        assertThat(provider).isInstanceOf(PersistenceServiceProvider.class);
    }

    @Test
    void legacySchemaValidatorFactory_getSupportedNamespace_returnsJavax() {
        var provider = new HibernateSchemaValidatorFactory();

        assertThat(provider.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void legacyPersistenceLinterFactory_implementsPersistenceServiceProvider() {
        assertThat(PersistenceServiceProvider.class).isAssignableFrom(HibernatePersistenceLinterFactory.class);
    }

}
