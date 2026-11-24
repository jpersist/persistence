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
import persistence.test.mock.MockJakartaSchemaExporterFactory;
import persistence.test.mock.MockJavaxSchemaExporterFactory;
import persistence.test.mock.TestModuleLayers;
import persistence.tools.SchemaExporter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
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

class SchemaExporterFactoryTest {

    private static final SchemaExporter NOOP_EXPORTER = new SchemaExporter() {
        @Override
        public void export(Action action, File file) {}

        @Override
        public void export(Action action, OutputStream stream) {}

        @Override
        public void export(Action action, Writer writer) {}
    };

    public static class CustomJakartaSchemaExporterFactory implements SchemaExporterFactory {
        @Override
        public SchemaExporter newSchemaExporter(List<String> entityClassNames, Map<String, Object> settings) {
            return NOOP_EXPORTER;
        }

        @Override
        public String getSupportedNamespace() {
            return "jakarta";
        }
    }

    public static class CustomJavaxSchemaExporterFactory implements SchemaExporterFactory {
        @Override
        public SchemaExporter newSchemaExporter(List<String> entityClassNames, Map<String, Object> settings) {
            return NOOP_EXPORTER;
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
            if (("META-INF/services/" + SchemaExporterFactory.class.getName()).equals(name)) {
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
    void newSchemaExporter_withDefaultSettings_delegatesToOverloadedMethod() {
        AtomicReference<Map<String, Object>> capturedSettings = new AtomicReference<>();
        AtomicReference<List<String>> capturedClasses = new AtomicReference<>();

        SchemaExporterFactory factory = new SchemaExporterFactory() {
            @Override
            public SchemaExporter newSchemaExporter(List<String> entityClassNames, Map<String, Object> settings) {
                capturedClasses.set(entityClassNames);
                capturedSettings.set(settings);
                return NOOP_EXPORTER;
            }

            @Override
            public String getSupportedNamespace() {
                return "jakarta";
            }
        };

        List<String> classNames = List.of("com.example.User", "com.example.Order");
        SchemaExporter exporter = factory.newSchemaExporter(classNames);

        assertThat(exporter).isNotNull();
        assertThat(capturedClasses.get()).isEqualTo(classNames);
        assertThat(capturedSettings.get()).isNotNull().isEmpty();
    }

    @Test
    void newSchemaExporter_withCustomSettings_passesSettingsCorrectly() {
        AtomicReference<Map<String, Object>> capturedSettings = new AtomicReference<>();
        AtomicReference<List<String>> capturedClasses = new AtomicReference<>();

        SchemaExporterFactory factory = new SchemaExporterFactory() {
            @Override
            public SchemaExporter newSchemaExporter(List<String> entityClassNames, Map<String, Object> settings) {
                capturedClasses.set(entityClassNames);
                capturedSettings.set(settings);
                return NOOP_EXPORTER;
            }

            @Override
            public String getSupportedNamespace() {
                return "jakarta";
            }
        };

        List<String> classNames = List.of("com.example.User");
        Map<String, Object> settings = Map.of("format", true);
        SchemaExporter exporter = factory.newSchemaExporter(classNames, settings);

        assertThat(exporter).isNotNull();
        assertThat(capturedClasses.get()).isEqualTo(classNames);
        assertThat(capturedSettings.get()).isEqualTo(settings);
    }

    @Test
    void newSchemaExporterFactory_withUnsupportedVersion_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> SchemaExporterFactory.newSchemaExporterFactory("1.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void newSchemaExporterFactory_withNullVersion_throwsException() {
        assertThatThrownBy(() -> SchemaExporterFactory.newSchemaExporterFactory(null))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0", "0.0", "99", "abc", ""})
    void newSchemaExporterFactory_withVariousInvalidVersions_throwsIllegalArgumentException(String version) {
        assertThatThrownBy(() -> SchemaExporterFactory.newSchemaExporterFactory(version))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newSchemaExporterFactory_withLegacyVersions_resolvesJavaxNamespace(String version) {
        assertThatThrownBy(() -> SchemaExporterFactory.newSchemaExporterFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax")
            .hasMessageContaining("SchemaExporterFactory");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newSchemaExporterFactory_withJakartaVersions_resolvesJakartaNamespace(String version) {
        assertThatThrownBy(() -> SchemaExporterFactory.newSchemaExporterFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta")
            .hasMessageContaining("SchemaExporterFactory");
    }

    @Test
    void newSchemaExporterFactory_withClassLoader_discoversCustomFactory() {
        ClassLoader classLoader = new InMemoryClassLoader(getClass().getClassLoader(), CustomJakartaSchemaExporterFactory.class);
        SchemaExporterFactory factory = SchemaExporterFactory.newSchemaExporterFactory("3.1", classLoader);
        assertThat(factory).isNotNull().isInstanceOf(CustomJakartaSchemaExporterFactory.class);
        assertThat(factory.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void newSchemaExporterFactory_withClassLoader_discoversCustomLegacyFactory() {
        ClassLoader classLoader = new InMemoryClassLoader(getClass().getClassLoader(), CustomJavaxSchemaExporterFactory.class);
        SchemaExporterFactory factory = SchemaExporterFactory.newSchemaExporterFactory("2.2", classLoader);
        assertThat(factory).isNotNull().isInstanceOf(CustomJavaxSchemaExporterFactory.class);
        assertThat(factory.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void newSchemaExporterFactory_withClassLoader_withUnsupportedVersion_throwsIllegalArgumentException() {
        ClassLoader classLoader = getClass().getClassLoader();
        assertThatThrownBy(() -> SchemaExporterFactory.newSchemaExporterFactory("1.0", classLoader))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void newSchemaExporterFactory_withClassLoader_whenNoProviderFound_throwsIllegalStateException() {
        ClassLoader classLoader = ClassLoader.getPlatformClassLoader();
        assertThatThrownBy(() -> SchemaExporterFactory.newSchemaExporterFactory("3.1", classLoader))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SchemaExporterFactory");
    }

    @Test
    void newSchemaExporterFactory_withModuleLayer_discoversCustomFactory() {
        ModuleLayer layer = TestModuleLayers.createModularLayer(
            getClass().getClassLoader(),
            SchemaExporterFactory.class,
            MockJakartaSchemaExporterFactory.class
        );

        Object factory = TestModuleLayers.invokeStatic(
            layer,
            SchemaExporterFactory.class,
            "newSchemaExporterFactory",
            new Class<?>[]{String.class, ModuleLayer.class},
            "3.1",
            layer
        );

        assertThat(factory).isNotNull();
        assertThat(factory.getClass().getName()).isEqualTo(MockJakartaSchemaExporterFactory.class.getName());
    }

    @Test
    void newSchemaExporterFactory_withModuleLayer_discoversCustomLegacyFactory() {
        ModuleLayer layer = TestModuleLayers.createModularLayer(
            getClass().getClassLoader(),
            SchemaExporterFactory.class,
            MockJavaxSchemaExporterFactory.class
        );

        Object factory = TestModuleLayers.invokeStatic(
            layer,
            SchemaExporterFactory.class,
            "newSchemaExporterFactory",
            new Class<?>[]{String.class, ModuleLayer.class},
            "2.2",
            layer
        );

        assertThat(factory).isNotNull();
        assertThat(factory.getClass().getName()).isEqualTo(MockJavaxSchemaExporterFactory.class.getName());
    }

    @Test
    void newSchemaExporterFactory_withModuleLayer_withNullVersion_throwsNullPointerException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> SchemaExporterFactory.newSchemaExporterFactory(null, bootLayer))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0", "0.0", "99", "abc", ""})
    void newSchemaExporterFactory_withModuleLayer_withVariousInvalidVersions_throwsIllegalArgumentException(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> SchemaExporterFactory.newSchemaExporterFactory(version, bootLayer))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newSchemaExporterFactory_withModuleLayer_withLegacyVersions_resolvesJavaxNamespace(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> SchemaExporterFactory.newSchemaExporterFactory(version, bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newSchemaExporterFactory_withModuleLayer_withJakartaVersions_resolvesJakartaNamespace(String version) {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> SchemaExporterFactory.newSchemaExporterFactory(version, bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

    @Test
    void newSchemaExporterFactory_withModuleLayer_withUnsupportedVersion_throwsIllegalArgumentException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> SchemaExporterFactory.newSchemaExporterFactory("1.0", bootLayer))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void newSchemaExporterFactory_withModuleLayer_whenNoProviderFound_throwsIllegalStateException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> SchemaExporterFactory.newSchemaExporterFactory("3.1", bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SchemaExporterFactory");
    }

    @Test
    void newSchemaExporterFactory_withEmptyModuleLayer_whenNoProviderFound_throwsIllegalStateException() {
        ModuleLayer emptyLayer = ModuleLayer.empty();
        assertThatThrownBy(() -> SchemaExporterFactory.newSchemaExporterFactory("3.1", emptyLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SchemaExporterFactory");
    }

}
