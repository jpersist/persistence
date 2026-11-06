package persist.hibernate.enhance.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import persist.test.mock.MockJakartaHibernateEnhancerProvider;
import persist.test.mock.MockJavaxHibernateEnhancerProvider;
import persist.test.mock.TestModuleLayers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HibernateEnhancerProviderTest {

    public static class CustomEnhancerProvider implements HibernateEnhancerProvider {
        @Override
        public HibernateEnhancer getEnhancer(File classesDir, Set<File> classpathFiles, Map<HibernateEnhancer.Option, Boolean> options) {
            return null;
        }

        @Override
        public String getSupportedNamespace() {
            return "jakarta";
        }
    }

    static class InMemoryClassLoader extends ClassLoader {
        private final Class<?> providerClass;

        InMemoryClassLoader(ClassLoader parent, Class<?> providerClass) {
            super(parent);
            this.providerClass = providerClass;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (("META-INF/services/" + HibernateEnhancerProvider.class.getName()).equals(name)) {
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
    void getHibernateEnhancerProvider_withUnsupportedVersion_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider("1.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void getHibernateEnhancerProvider_withNullVersion_throwsException() {
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(null))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0", "0.0", "99", "abc", ""})
    void getHibernateEnhancerProvider_withVariousInvalidVersions_throwsIllegalArgumentException(String version) {
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(version))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void getHibernateEnhancerProvider_withLegacyVersions_resolvesJavaxNamespace(String version) {
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void getHibernateEnhancerProvider_withJakartaVersions_resolvesJakartaNamespace(String version) {
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void getHibernateEnhancerProvider_withLegacyVersions_includesProviderClassName(String version) {
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("HibernateEnhancerProvider");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void getHibernateEnhancerProvider_withJakartaVersions_includesProviderClassName(String version) {
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("HibernateEnhancerProvider");
    }

    @Test
    void getHibernateEnhancerProvider_withClassLoader_discoversCustomProvider() {
        ClassLoader classLoader = new InMemoryClassLoader(getClass().getClassLoader(), CustomEnhancerProvider.class);
        HibernateEnhancerProvider provider = HibernateEnhancerProvider.getHibernateEnhancerProvider("3.1", classLoader);
        assertThat(provider).isNotNull().isInstanceOf(CustomEnhancerProvider.class);
        assertThat(provider.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void getHibernateEnhancerProvider_withClassLoader_withUnsupportedVersion_throwsIllegalArgumentException() {
        ClassLoader classLoader = getClass().getClassLoader();
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider("1.0", classLoader))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void getHibernateEnhancerProvider_withClassLoader_whenNoProviderFound_throwsIllegalStateException() {
        ClassLoader classLoader = ClassLoader.getPlatformClassLoader();
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider("3.1", classLoader))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("HibernateEnhancerProvider");
    }

    @Test
    void getHibernateEnhancerProvider_withModuleLayer_discoversCustomProvider() {
        ModuleLayer layer = TestModuleLayers.createModularLayer(
            getClass().getClassLoader(),
            HibernateEnhancerProvider.class,
            MockJakartaHibernateEnhancerProvider.class
        );

        Object provider = TestModuleLayers.invokeStatic(
            layer,
            HibernateEnhancerProvider.class,
            "getHibernateEnhancerProvider",
            new Class<?>[]{String.class, ModuleLayer.class},
            "3.1",
            layer
        );

        assertThat(provider).isNotNull();
        assertThat(provider.getClass().getName()).isEqualTo(MockJakartaHibernateEnhancerProvider.class.getName());
    }

    @Test
    void getHibernateEnhancerProvider_withModuleLayer_discoversCustomLegacyProvider() {
        ModuleLayer layer = TestModuleLayers.createModularLayer(
            getClass().getClassLoader(),
            HibernateEnhancerProvider.class,
            MockJavaxHibernateEnhancerProvider.class
        );

        Object provider = TestModuleLayers.invokeStatic(
            layer,
            HibernateEnhancerProvider.class,
            "getHibernateEnhancerProvider",
            new Class<?>[]{String.class, ModuleLayer.class},
            "2.2",
            layer
        );

        assertThat(provider).isNotNull();
        assertThat(provider.getClass().getName()).isEqualTo(MockJavaxHibernateEnhancerProvider.class.getName());
    }

    @Test
    void getHibernateEnhancerProvider_withModuleLayer_withNullVersion_throwsNullPointerException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(null, bootLayer))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0", "0.0", "99", "abc", ""})
    void getHibernateEnhancerProvider_withModuleLayer_withVariousInvalidVersions_throwsIllegalArgumentException(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(version, bootLayer))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void getHibernateEnhancerProvider_withModuleLayer_withLegacyVersions_resolvesJavaxNamespace(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(version, bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void getHibernateEnhancerProvider_withModuleLayer_withJakartaVersions_resolvesJakartaNamespace(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider(version, bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

    @Test
    void getHibernateEnhancerProvider_withModuleLayer_withUnsupportedVersion_throwsIllegalArgumentException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider("1.0", bootLayer))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void getHibernateEnhancerProvider_withModuleLayer_whenNoProviderFound_throwsIllegalStateException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider("3.1", bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("HibernateEnhancerProvider");
    }

    @Test
    void getHibernateEnhancerProvider_withEmptyModuleLayer_whenNoProviderFound_throwsIllegalStateException() {
        ModuleLayer emptyLayer = ModuleLayer.empty();
        assertThatThrownBy(() -> HibernateEnhancerProvider.getHibernateEnhancerProvider("3.1", emptyLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("HibernateEnhancerProvider");
    }

}
