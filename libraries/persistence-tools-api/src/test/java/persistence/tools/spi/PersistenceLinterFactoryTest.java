package persistence.tools.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import persistence.test.mock.MockJakartaNamedObjectParserFactory;
import persistence.test.mock.MockJakartaSchemaValidatorFactory;
import persistence.test.mock.TestModuleLayers;
import persistence.tools.NamedObjectParser;
import persistence.tools.SchemaValidator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersistenceLinterFactoryTest {

    public static class CustomNamedObjectParserFactory implements NamedObjectParserFactory {
        @Override
        public NamedObjectParser newNamedObjectParser(List<String> discoveredClassNames) {
            return null;
        }

        @Override
        public String getSupportedNamespace() {
            return "jakarta";
        }
    }

    public static class CustomSchemaValidatorFactory implements SchemaValidatorFactory {
        @Override
        public SchemaValidator newSchemaValidator(List<String> discoveredClassNames) {
            return null;
        }

        @Override
        public String getSupportedNamespace() {
            return "jakarta";
        }
    }

    static class InMemoryClassLoader extends ClassLoader {
        private final Class<?> serviceClass;
        private final Class<?> providerClass;

        InMemoryClassLoader(ClassLoader parent, Class<?> serviceClass, Class<?> providerClass) {
            super(parent);
            this.serviceClass = serviceClass;
            this.providerClass = providerClass;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (("META-INF/services/" + serviceClass.getName()).equals(name)) {
                URL url = new URL("test", null, -1, "/", new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL u) {
                        return new URLConnection(u) {
                            @Override
                            public void connect() {}

                            @Override
                            public InputStream getInputStream() {
                                return new ByteArrayInputStream((providerClass.getName() + "\n").getBytes(StandardCharsets.UTF_8));
                            }
                        };
                    }
                });
                return Collections.enumeration(List.of(url));
            }
            return super.getResources(name);
        }
    }

    @Test
    void newPersistenceLinterFactory_withUnsupportedVersion_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> PersistenceLinterFactory.newPersistenceLinterFactory("1.0", NamedObjectParserFactory.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void newPersistenceLinterFactory_withNullVersion_throwsException() {
        assertThatThrownBy(() -> PersistenceLinterFactory.newPersistenceLinterFactory(null, NamedObjectParserFactory.class))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0", "0.0", "99", "abc", ""})
    void newPersistenceLinterFactory_withVariousInvalidVersions_throwsIllegalArgumentException(String version) {
        assertThatThrownBy(() -> PersistenceLinterFactory.newPersistenceLinterFactory(version, NamedObjectParserFactory.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newPersistenceLinterFactory_withLegacyVersions_resolvesJavaxNamespace(String version) {
        assertThatThrownBy(() -> PersistenceLinterFactory.newPersistenceLinterFactory(version, NamedObjectParserFactory.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newPersistenceLinterFactory_withJakartaVersions_resolvesJakartaNamespace(String version) {
        assertThatThrownBy(() -> PersistenceLinterFactory.newPersistenceLinterFactory(version, NamedObjectParserFactory.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

    @Test
    void newNamedObjectParserFactory_delegatesToGenericMethod() {
        assertThatThrownBy(() -> PersistenceLinterFactory.newNamedObjectParserFactory("1.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void newSchemaValidatorFactory_delegatesToGenericMethod() {
        assertThatThrownBy(() -> PersistenceLinterFactory.newSchemaValidatorFactory("1.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newSchemaValidatorFactory_withLegacyVersions_resolvesJavaxNamespace(String version) {
        assertThatThrownBy(() -> PersistenceLinterFactory.newSchemaValidatorFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newSchemaValidatorFactory_withJakartaVersions_resolvesJakartaNamespace(String version) {
        assertThatThrownBy(() -> PersistenceLinterFactory.newSchemaValidatorFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

    @Test
    void newPersistenceLinterFactory_withClassLoader_discoversCustomFactory() {
        ClassLoader classLoader = new InMemoryClassLoader(
            getClass().getClassLoader(),
            NamedObjectParserFactory.class,
            CustomNamedObjectParserFactory.class
        );
        NamedObjectParserFactory factory = PersistenceLinterFactory.newPersistenceLinterFactory(
            "3.1",
            NamedObjectParserFactory.class,
            classLoader
        );
        assertThat(factory).isNotNull().isInstanceOf(CustomNamedObjectParserFactory.class);
    }

    @Test
    void newNamedObjectParserFactory_withClassLoader_discoversCustomFactory() {
        ClassLoader classLoader = new InMemoryClassLoader(
            getClass().getClassLoader(),
            NamedObjectParserFactory.class,
            CustomNamedObjectParserFactory.class
        );
        NamedObjectParserFactory factory = PersistenceLinterFactory.newNamedObjectParserFactory("3.1", classLoader);
        assertThat(factory).isNotNull().isInstanceOf(CustomNamedObjectParserFactory.class);
    }

    @Test
    void newSchemaValidatorFactory_withClassLoader_discoversCustomFactory() {
        ClassLoader classLoader = new InMemoryClassLoader(
            getClass().getClassLoader(),
            SchemaValidatorFactory.class,
            CustomSchemaValidatorFactory.class
        );
        SchemaValidatorFactory factory = PersistenceLinterFactory.newSchemaValidatorFactory("3.1", classLoader);
        assertThat(factory).isNotNull().isInstanceOf(CustomSchemaValidatorFactory.class);
    }

    @Test
    void newPersistenceLinterFactory_withModuleLayer_discoversCustomFactory() {
        ModuleLayer layer = TestModuleLayers.createModularLayer(
            getClass().getClassLoader(),
            NamedObjectParserFactory.class,
            MockJakartaNamedObjectParserFactory.class
        );

        Object factory = TestModuleLayers.invokeStatic(
            layer,
            PersistenceLinterFactory.class,
            "newPersistenceLinterFactory",
            new Class<?>[]{String.class, Class.class, ModuleLayer.class},
            "3.1",
            NamedObjectParserFactory.class,
            layer
        );

        assertThat(factory).isNotNull();
        assertThat(factory.getClass().getName()).isEqualTo(MockJakartaNamedObjectParserFactory.class.getName());
    }

    @Test
    void newNamedObjectParserFactory_withModuleLayer_discoversCustomFactory() {
        ModuleLayer layer = TestModuleLayers.createModularLayer(
            getClass().getClassLoader(),
            NamedObjectParserFactory.class,
            MockJakartaNamedObjectParserFactory.class
        );

        Object factory = TestModuleLayers.invokeStatic(
            layer,
            PersistenceLinterFactory.class,
            "newNamedObjectParserFactory",
            new Class<?>[]{String.class, ModuleLayer.class},
            "3.1",
            layer
        );

        assertThat(factory).isNotNull();
        assertThat(factory.getClass().getName()).isEqualTo(MockJakartaNamedObjectParserFactory.class.getName());
    }

    @Test
    void newSchemaValidatorFactory_withModuleLayer_discoversCustomFactory() {
        ModuleLayer layer = TestModuleLayers.createModularLayer(
            getClass().getClassLoader(),
            SchemaValidatorFactory.class,
            MockJakartaSchemaValidatorFactory.class
        );

        Object factory = TestModuleLayers.invokeStatic(
            layer,
            PersistenceLinterFactory.class,
            "newSchemaValidatorFactory",
            new Class<?>[]{String.class, ModuleLayer.class},
            "3.1",
            layer
        );

        assertThat(factory).isNotNull();
        assertThat(factory.getClass().getName()).isEqualTo(MockJakartaSchemaValidatorFactory.class.getName());
    }

    @Test
    void newPersistenceLinterFactory_withModuleLayer_withNullVersion_throwsNullPointerException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> PersistenceLinterFactory.newPersistenceLinterFactory(null, NamedObjectParserFactory.class, bootLayer))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0", "0.0", "99", "abc", ""})
    void newPersistenceLinterFactory_withModuleLayer_withVariousInvalidVersions_throwsIllegalArgumentException(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> PersistenceLinterFactory.newPersistenceLinterFactory(version, NamedObjectParserFactory.class, bootLayer))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newPersistenceLinterFactory_withModuleLayer_withLegacyVersions_resolvesJavaxNamespace(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> PersistenceLinterFactory.newPersistenceLinterFactory(version, NamedObjectParserFactory.class, bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newPersistenceLinterFactory_withModuleLayer_withJakartaVersions_resolvesJakartaNamespace(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> PersistenceLinterFactory.newPersistenceLinterFactory(version, NamedObjectParserFactory.class, bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

    @Test
    void newPersistenceLinterFactory_withModuleLayer_whenNoProviderFound_throwsIllegalStateException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> PersistenceLinterFactory.newPersistenceLinterFactory("3.1", NamedObjectParserFactory.class, bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NamedObjectParserFactory");
    }

    @Test
    void newNamedObjectParserFactory_withModuleLayer_whenNoProviderFound_throwsIllegalStateException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> PersistenceLinterFactory.newNamedObjectParserFactory("3.1", bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NamedObjectParserFactory");
    }

    @Test
    void newSchemaValidatorFactory_withModuleLayer_whenNoProviderFound_throwsIllegalStateException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> PersistenceLinterFactory.newSchemaValidatorFactory("3.1", bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SchemaValidatorFactory");
    }

    @Test
    void newPersistenceLinterFactory_withEmptyModuleLayer_whenNoProviderFound_throwsIllegalStateException() {
        ModuleLayer emptyLayer = ModuleLayer.empty();
        assertThatThrownBy(() -> PersistenceLinterFactory.newPersistenceLinterFactory("3.1", NamedObjectParserFactory.class, emptyLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NamedObjectParserFactory");
    }

}
