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
import persistence.test.mock.MockJakartaEntityModelLinterFactory;
import persistence.test.mock.MockJavaxEntityModelLinterFactory;
import persistence.test.mock.TestModuleLayers;
import persistence.tools.EntityModelLinter;

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
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityModelLinterFactoryTest {

    public static class CustomJakartaEntityModelLinterFactory implements EntityModelLinterFactory {
        @Override
        public EntityModelLinter newEntityModelLinter(List<String> entityClassNames, Map<String, String> settings) {
            return Collections::emptyList;
        }

        @Override
        public String getSupportedNamespace() {
            return "jakarta";
        }
    }

    public static class CustomJavaxEntityModelLinterFactory implements EntityModelLinterFactory {
        @Override
        public EntityModelLinter newEntityModelLinter(List<String> entityClassNames, Map<String, String> settings) {
            return Collections::emptyList;
        }

        @Override
        public String getSupportedNamespace() {
            return "javax";
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
            if (("META-INF/services/" + EntityModelLinterFactory.class.getName()).equals(name)) {
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
    void newEntityModelLinter_withDefaultSettings_delegatesToOverloadedMethod() {
        AtomicReference<Map<String, String>> capturedSettings = new AtomicReference<>();
        AtomicReference<List<String>> capturedClasses = new AtomicReference<>();

        EntityModelLinterFactory factory = new EntityModelLinterFactory() {
            @Override
            public EntityModelLinter newEntityModelLinter(List<String> entityClassNames, Map<String, String> settings) {
                capturedClasses.set(entityClassNames);
                capturedSettings.set(settings);
                return Collections::emptyList;
            }

            @Override
            public String getSupportedNamespace() {
                return "jakarta";
            }
        };

        List<String> classNames = List.of("com.example.User", "com.example.Order");
        EntityModelLinter linter = factory.newEntityModelLinter(classNames);

        assertThat(linter).isNotNull();
        assertThat(capturedClasses.get()).isEqualTo(classNames);
        assertThat(capturedSettings.get()).isNotNull().isEmpty();
    }

    @Test
    void newEntityModelLinter_withCustomSettings_passesSettingsCorrectly() {
        AtomicReference<Map<String, String>> capturedSettings = new AtomicReference<>();
        AtomicReference<List<String>> capturedClasses = new AtomicReference<>();

        EntityModelLinterFactory factory = new EntityModelLinterFactory() {
            @Override
            public EntityModelLinter newEntityModelLinter(List<String> entityClassNames, Map<String, String> settings) {
                capturedClasses.set(entityClassNames);
                capturedSettings.set(settings);
                return Collections::emptyList;
            }

            @Override
            public String getSupportedNamespace() {
                return "jakarta";
            }
        };

        List<String> classNames = List.of("com.example.User");
        Map<String, String> settings = Map.of("strictMode", "true");
        EntityModelLinter linter = factory.newEntityModelLinter(classNames, settings);

        assertThat(linter).isNotNull();
        assertThat(capturedClasses.get()).isEqualTo(classNames);
        assertThat(capturedSettings.get()).isEqualTo(settings);
    }

    @Test
    void newEntityModelLinterFactory_withUnsupportedVersion_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> EntityModelLinterFactory.newEntityModelLinterFactory("1.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void newEntityModelLinterFactory_withNullVersion_throwsException() {
        assertThatThrownBy(() -> EntityModelLinterFactory.newEntityModelLinterFactory(null))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0", "0.0", "99", "abc", ""})
    void newEntityModelLinterFactory_withVariousInvalidVersions_throwsIllegalArgumentException(String version) {
        assertThatThrownBy(() -> EntityModelLinterFactory.newEntityModelLinterFactory(version))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newEntityModelLinterFactory_withLegacyVersions_resolvesJavaxNamespace(String version) {
        assertThatThrownBy(() -> EntityModelLinterFactory.newEntityModelLinterFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax")
            .hasMessageContaining("EntityModelLinterFactory");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newEntityModelLinterFactory_withJakartaVersions_resolvesJakartaNamespace(String version) {
        assertThatThrownBy(() -> EntityModelLinterFactory.newEntityModelLinterFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta")
            .hasMessageContaining("EntityModelLinterFactory");
    }

    @Test
    void newEntityModelLinterFactory_withClassLoader_discoversCustomFactory() {
        ClassLoader classLoader = new InMemoryClassLoader(getClass().getClassLoader(), CustomJakartaEntityModelLinterFactory.class);
        EntityModelLinterFactory factory = EntityModelLinterFactory.newEntityModelLinterFactory("3.1", classLoader);
        assertThat(factory).isNotNull().isInstanceOf(CustomJakartaEntityModelLinterFactory.class);
        assertThat(factory.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void newEntityModelLinterFactory_withClassLoader_discoversCustomLegacyFactory() {
        ClassLoader classLoader = new InMemoryClassLoader(getClass().getClassLoader(), CustomJavaxEntityModelLinterFactory.class);
        EntityModelLinterFactory factory = EntityModelLinterFactory.newEntityModelLinterFactory("2.2", classLoader);
        assertThat(factory).isNotNull().isInstanceOf(CustomJavaxEntityModelLinterFactory.class);
        assertThat(factory.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void newEntityModelLinterFactory_withClassLoader_withUnsupportedVersion_throwsIllegalArgumentException() {
        ClassLoader classLoader = getClass().getClassLoader();
        assertThatThrownBy(() -> EntityModelLinterFactory.newEntityModelLinterFactory("1.0", classLoader))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void newEntityModelLinterFactory_withClassLoader_whenNoProviderFound_throwsIllegalStateException() {
        ClassLoader classLoader = ClassLoader.getPlatformClassLoader();
        assertThatThrownBy(() -> EntityModelLinterFactory.newEntityModelLinterFactory("3.1", classLoader))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("EntityModelLinterFactory");
    }

    @Test
    void newEntityModelLinterFactory_withModuleLayer_discoversCustomFactory() {
        ModuleLayer layer = TestModuleLayers.createModularLayer(
            getClass().getClassLoader(),
            EntityModelLinterFactory.class,
            MockJakartaEntityModelLinterFactory.class
        );

        Object factory = TestModuleLayers.invokeStatic(
            layer,
            EntityModelLinterFactory.class,
            "newEntityModelLinterFactory",
            new Class<?>[]{String.class, ModuleLayer.class},
            "3.1",
            layer
        );

        assertThat(factory).isNotNull();
        assertThat(factory.getClass().getName()).isEqualTo(MockJakartaEntityModelLinterFactory.class.getName());
    }

    @Test
    void newEntityModelLinterFactory_withModuleLayer_discoversCustomLegacyFactory() {
        ModuleLayer layer = TestModuleLayers.createModularLayer(
            getClass().getClassLoader(),
            EntityModelLinterFactory.class,
            MockJavaxEntityModelLinterFactory.class
        );

        Object factory = TestModuleLayers.invokeStatic(
            layer,
            EntityModelLinterFactory.class,
            "newEntityModelLinterFactory",
            new Class<?>[]{String.class, ModuleLayer.class},
            "2.2",
            layer
        );

        assertThat(factory).isNotNull();
        assertThat(factory.getClass().getName()).isEqualTo(MockJavaxEntityModelLinterFactory.class.getName());
    }

    @Test
    void newEntityModelLinterFactory_withModuleLayer_withNullVersion_throwsNullPointerException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> EntityModelLinterFactory.newEntityModelLinterFactory(null, bootLayer))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0", "0.0", "99", "abc", ""})
    void newEntityModelLinterFactory_withModuleLayer_withVariousInvalidVersions_throwsIllegalArgumentException(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> EntityModelLinterFactory.newEntityModelLinterFactory(version, bootLayer))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newEntityModelLinterFactory_withModuleLayer_withLegacyVersions_resolvesJavaxNamespace(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> EntityModelLinterFactory.newEntityModelLinterFactory(version, bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newEntityModelLinterFactory_withModuleLayer_withJakartaVersions_resolvesJakartaNamespace(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> EntityModelLinterFactory.newEntityModelLinterFactory(version, bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

    @Test
    void newEntityModelLinterFactory_withModuleLayer_withUnsupportedVersion_throwsIllegalArgumentException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> EntityModelLinterFactory.newEntityModelLinterFactory("1.0", bootLayer))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void newEntityModelLinterFactory_withModuleLayer_whenNoProviderFound_throwsIllegalStateException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> EntityModelLinterFactory.newEntityModelLinterFactory("3.1", bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("EntityModelLinterFactory");
    }

    @Test
    void newEntityModelLinterFactory_withEmptyModuleLayer_whenNoProviderFound_throwsIllegalStateException() {
        ModuleLayer emptyLayer = ModuleLayer.empty();
        assertThatThrownBy(() -> EntityModelLinterFactory.newEntityModelLinterFactory("3.1", emptyLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("EntityModelLinterFactory");
    }

}
