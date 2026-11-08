package persist.legacy.spi;

import org.junit.jupiter.api.Test;
import persist.api.spi.PersistenceServiceProvider;
import persist.lint.legacy.LegacyNamedObjectParserFactory;
import persist.lint.legacy.LegacyPersistenceLinterFactory;
import persist.lint.legacy.LegacySchemaValidatorFactory;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyPersistenceServiceProviderTest {

    @Test
    void getSupportedNamespace_returnsJavax() {
        var provider = new LegacyNamedObjectParserFactory();

        assertThat(provider.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void legacyNamedObjectParserFactory_isPersistenceServiceProvider() {
        var provider = new LegacyNamedObjectParserFactory();

        assertThat(provider).isInstanceOf(PersistenceServiceProvider.class);
    }

    @Test
    void legacySchemaValidatorFactory_isPersistenceServiceProvider() {
        var provider = new LegacySchemaValidatorFactory();

        assertThat(provider).isInstanceOf(PersistenceServiceProvider.class);
    }

    @Test
    void legacySchemaValidatorFactory_getSupportedNamespace_returnsJavax() {
        var provider = new LegacySchemaValidatorFactory();

        assertThat(provider.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void legacyPersistenceLinterFactory_implementsPersistenceServiceProvider() {
        assertThat(PersistenceServiceProvider.class).isAssignableFrom(LegacyPersistenceLinterFactory.class);
    }

}
