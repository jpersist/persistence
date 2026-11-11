/*
 * Copyright 2024-2026 JPersist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package persistence.tools.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import persistence.test.mock.MockJakartaNamedObjectParserFactory;
import persistence.test.mock.MockJavaxNamedObjectParserFactory;
import persistence.test.mock.TestModuleLayers;
import persistence.tools.NamedObjectParser;

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

class NamedObjectParserFactoryTest {

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

    static class InMemoryClassLoader extends ClassLoader {
        private final Class<?> providerClass;

        InMemoryClassLoader(ClassLoader parent, Class<?> providerClass) {
            super(parent);
            this.providerClass = providerClass;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (("META-INF/services/" + NamedObjectParserFactory.class.getName()).equals(name)) {
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
    void newNamedObjectParserFactory_withUnsupportedVersion_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> NamedObjectParserFactory.newNamedObjectParserFactory("1.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void newNamedObjectParserFactory_withNullVersion_throwsException() {
        assertThatThrownBy(() -> NamedObjectParserFactory.newNamedObjectParserFactory(null))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0", "0.0", "99", "abc", ""})
    void newNamedObjectParserFactory_withVariousInvalidVersions_throwsIllegalArgumentException(String version) {
        assertThatThrownBy(() -> NamedObjectParserFactory.newNamedObjectParserFactory(version))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newNamedObjectParserFactory_withLegacyVersions_resolvesJavaxNamespace(String version) {
        assertThatThrownBy(() -> NamedObjectParserFactory.newNamedObjectParserFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newNamedObjectParserFactory_withJakartaVersions_resolvesJakartaNamespace(String version) {
        assertThatThrownBy(() -> NamedObjectParserFactory.newNamedObjectParserFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

    @Test
    void newNamedObjectParserFactory_withClassLoader_discoversCustomFactory() {
        ClassLoader classLoader = new InMemoryClassLoader(getClass().getClassLoader(), CustomNamedObjectParserFactory.class);
        NamedObjectParserFactory factory = NamedObjectParserFactory.newNamedObjectParserFactory("3.1", classLoader);
        assertThat(factory).isNotNull().isInstanceOf(CustomNamedObjectParserFactory.class);
    }

    @Test
    void newNamedObjectParserFactory_withClassLoader_withUnsupportedVersion_throwsIllegalArgumentException() {
        ClassLoader classLoader = getClass().getClassLoader();
        assertThatThrownBy(() -> NamedObjectParserFactory.newNamedObjectParserFactory("1.0", classLoader))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void newNamedObjectParserFactory_withClassLoader_whenNoProviderFound_throwsIllegalStateException() {
        ClassLoader classLoader = ClassLoader.getPlatformClassLoader();
        assertThatThrownBy(() -> NamedObjectParserFactory.newNamedObjectParserFactory("3.1", classLoader))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NamedObjectParserFactory");
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
            NamedObjectParserFactory.class,
            "newNamedObjectParserFactory",
            new Class<?>[]{String.class, ModuleLayer.class},
            "3.1",
            layer
        );

        assertThat(factory).isNotNull();
        assertThat(factory.getClass().getName()).isEqualTo(MockJakartaNamedObjectParserFactory.class.getName());
    }

    @Test
    void newNamedObjectParserFactory_withModuleLayer_discoversCustomLegacyFactory() {
        ModuleLayer layer = TestModuleLayers.createModularLayer(
            getClass().getClassLoader(),
            NamedObjectParserFactory.class,
            MockJavaxNamedObjectParserFactory.class
        );

        Object factory = TestModuleLayers.invokeStatic(
            layer,
            NamedObjectParserFactory.class,
            "newNamedObjectParserFactory",
            new Class<?>[]{String.class, ModuleLayer.class},
            "2.2",
            layer
        );

        assertThat(factory).isNotNull();
        assertThat(factory.getClass().getName()).isEqualTo(MockJavaxNamedObjectParserFactory.class.getName());
    }

    @Test
    void newNamedObjectParserFactory_withModuleLayer_withNullVersion_throwsNullPointerException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> NamedObjectParserFactory.newNamedObjectParserFactory(null, bootLayer))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0", "0.0", "99", "abc", ""})
    void newNamedObjectParserFactory_withModuleLayer_withVariousInvalidVersions_throwsIllegalArgumentException(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> NamedObjectParserFactory.newNamedObjectParserFactory(version, bootLayer))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newNamedObjectParserFactory_withModuleLayer_withLegacyVersions_resolvesJavaxNamespace(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> NamedObjectParserFactory.newNamedObjectParserFactory(version, bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newNamedObjectParserFactory_withModuleLayer_withJakartaVersions_resolvesJakartaNamespace(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> NamedObjectParserFactory.newNamedObjectParserFactory(version, bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

    @Test
    void newNamedObjectParserFactory_withModuleLayer_withUnsupportedVersion_throwsIllegalArgumentException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> NamedObjectParserFactory.newNamedObjectParserFactory("1.0", bootLayer))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void newNamedObjectParserFactory_withModuleLayer_whenNoProviderFound_throwsIllegalStateException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> NamedObjectParserFactory.newNamedObjectParserFactory("3.1", bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NamedObjectParserFactory");
    }

    @Test
    void newNamedObjectParserFactory_withEmptyModuleLayer_whenNoProviderFound_throwsIllegalStateException() {
        ModuleLayer emptyLayer = ModuleLayer.empty();
        assertThatThrownBy(() -> NamedObjectParserFactory.newNamedObjectParserFactory("3.1", emptyLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NamedObjectParserFactory");
    }

}
