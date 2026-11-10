package persistence.tools.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import persistence.test.mock.MockJakartaPersistenceServiceProvider;
import persistence.test.mock.MockJavaxPersistenceServiceProvider;
import persistence.test.mock.TestModuleLayers;

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

class PersistenceServiceProviderTest {

    public static class CustomJakartaProvider implements PersistenceServiceProvider {
        @Override
        public String getSupportedNamespace() {
            return "jakarta";
        }
    }

    public static class CustomJavaxProvider implements PersistenceServiceProvider {
        @Override
        public String getSupportedNamespace() {
            return "javax";
        }
    }

    static class InMemoryServiceProviderClassLoader extends ClassLoader {
        private final Class<? extends PersistenceServiceProvider> providerClass;

        InMemoryServiceProviderClassLoader(ClassLoader parent, Class<? extends PersistenceServiceProvider> providerClass) {
            super(parent);
            this.providerClass = providerClass;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (("META-INF/services/" + PersistenceServiceProvider.class.getName()).equals(name)) {
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
    void getServiceProvider_withUnsupportedVersion_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider("1.0", PersistenceServiceProvider.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void getServiceProvider_withNullVersion_throwsException() {
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider(null, PersistenceServiceProvider.class))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0", "0.0", "99", "abc", ""})
    void getServiceProvider_withVariousInvalidVersions_throwsIllegalArgumentException(String version) {
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider(version, PersistenceServiceProvider.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void getServiceProvider_withLegacyVersions_resolvesJavaxNamespace(String version) {
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider(version, PersistenceServiceProvider.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void getServiceProvider_withJakartaVersions_resolvesJakartaNamespace(String version) {
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider(version, PersistenceServiceProvider.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void getServiceProvider_withLegacyVersions_includesProviderClassName(String version) {
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider(version, PersistenceServiceProvider.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PersistenceServiceProvider");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void getServiceProvider_withJakartaVersions_includesProviderClassName(String version) {
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider(version, PersistenceServiceProvider.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PersistenceServiceProvider");
    }

    @Test
    void getServiceProvider_withClassLoader_discoversCustomProvider() {
        ClassLoader classLoader = new InMemoryServiceProviderClassLoader(
            getClass().getClassLoader(),
            CustomJakartaProvider.class
        );

        PersistenceServiceProvider provider = PersistenceServiceProvider.getServiceProvider(
            "3.1",
            PersistenceServiceProvider.class,
            classLoader
        );

        assertThat(provider).isNotNull();
        assertThat(provider).isInstanceOf(CustomJakartaProvider.class);
        assertThat(provider.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void getServiceProvider_withClassLoader_discoversCustomLegacyProvider() {
        ClassLoader classLoader = new InMemoryServiceProviderClassLoader(
            getClass().getClassLoader(),
            CustomJavaxProvider.class
        );

        PersistenceServiceProvider provider = PersistenceServiceProvider.getServiceProvider(
            "2.2",
            PersistenceServiceProvider.class,
            classLoader
        );

        assertThat(provider).isNotNull();
        assertThat(provider).isInstanceOf(CustomJavaxProvider.class);
        assertThat(provider.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void getServiceProvider_withClassLoader_withUnsupportedVersion_throwsIllegalArgumentException() {
        ClassLoader classLoader = getClass().getClassLoader();
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider("1.0", PersistenceServiceProvider.class, classLoader))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void getServiceProvider_withClassLoader_whenNoProviderFound_throwsIllegalStateException() {
        ClassLoader classLoader = ClassLoader.getPlatformClassLoader();
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider("3.1", PersistenceServiceProvider.class, classLoader))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to discover a registered PersistenceServiceProvider provider for namespace: jakarta");
    }

    @Test
    void getServiceProvider_withModuleLayer_discoversCustomProvider() {
        ModuleLayer layer = TestModuleLayers.createModularLayer(
            getClass().getClassLoader(),
            PersistenceServiceProvider.class,
            MockJakartaPersistenceServiceProvider.class
        );

        Object provider = TestModuleLayers.invokeStatic(
            layer,
            PersistenceServiceProvider.class,
            "getServiceProvider",
            new Class<?>[]{String.class, Class.class, ModuleLayer.class},
            "3.1",
            PersistenceServiceProvider.class,
            layer
        );

        assertThat(provider).isNotNull();
        assertThat(provider.getClass().getName()).isEqualTo(MockJakartaPersistenceServiceProvider.class.getName());
    }

    @Test
    void getServiceProvider_withModuleLayer_discoversCustomLegacyProvider() {
        ModuleLayer layer = TestModuleLayers.createModularLayer(
            getClass().getClassLoader(),
            PersistenceServiceProvider.class,
            MockJavaxPersistenceServiceProvider.class
        );

        Object provider = TestModuleLayers.invokeStatic(
            layer,
            PersistenceServiceProvider.class,
            "getServiceProvider",
            new Class<?>[]{String.class, Class.class, ModuleLayer.class},
            "2.2",
            PersistenceServiceProvider.class,
            layer
        );

        assertThat(provider).isNotNull();
        assertThat(provider.getClass().getName()).isEqualTo(MockJavaxPersistenceServiceProvider.class.getName());
    }

    @Test
    void getServiceProvider_withModuleLayer_withNullVersion_throwsNullPointerException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider(null, PersistenceServiceProvider.class, bootLayer))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0", "0.0", "99", "abc", ""})
    void getServiceProvider_withModuleLayer_withVariousInvalidVersions_throwsIllegalArgumentException(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider(version, PersistenceServiceProvider.class, bootLayer))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void getServiceProvider_withModuleLayer_withLegacyVersions_resolvesJavaxNamespace(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider(version, PersistenceServiceProvider.class, bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void getServiceProvider_withModuleLayer_withJakartaVersions_resolvesJakartaNamespace(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider(version, PersistenceServiceProvider.class, bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

    @Test
    void getServiceProvider_withModuleLayer_withUnsupportedVersion_throwsIllegalArgumentException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider("1.0", PersistenceServiceProvider.class, bootLayer))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void getServiceProvider_withModuleLayer_whenNoProviderFound_throwsIllegalStateException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider("3.1", PersistenceServiceProvider.class, bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to discover a registered PersistenceServiceProvider provider for namespace: jakarta");
    }

    @Test
    void getServiceProvider_withEmptyModuleLayer_whenNoProviderFound_throwsIllegalStateException() {
        ModuleLayer emptyLayer = ModuleLayer.empty();
        assertThatThrownBy(() -> PersistenceServiceProvider.getServiceProvider("3.1", PersistenceServiceProvider.class, emptyLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to discover a registered PersistenceServiceProvider provider for namespace: jakarta");
    }

}
