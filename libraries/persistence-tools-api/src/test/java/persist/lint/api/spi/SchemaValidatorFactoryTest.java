package persist.lint.api.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

class SchemaValidatorFactoryTest {

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
        private final Class<?> providerClass;

        InMemoryClassLoader(ClassLoader parent, Class<?> providerClass) {
            super(parent);
            this.providerClass = providerClass;
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (("META-INF/services/" + SchemaValidatorFactory.class.getName()).equals(name)) {
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
    void newSchemaValidatorFactory_withUnsupportedVersion_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> SchemaValidatorFactory.newSchemaValidatorFactory("1.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void newSchemaValidatorFactory_withNullVersion_throwsException() {
        assertThatThrownBy(() -> SchemaValidatorFactory.newSchemaValidatorFactory(null))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0", "0.0", "99", "abc", ""})
    void newSchemaValidatorFactory_withVariousInvalidVersions_throwsIllegalArgumentException(String version) {
        assertThatThrownBy(() -> SchemaValidatorFactory.newSchemaValidatorFactory(version))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newSchemaValidatorFactory_withLegacyVersions_resolvesJavaxNamespace(String version) {
        assertThatThrownBy(() -> SchemaValidatorFactory.newSchemaValidatorFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newSchemaValidatorFactory_withJakartaVersions_resolvesJakartaNamespace(String version) {
        assertThatThrownBy(() -> SchemaValidatorFactory.newSchemaValidatorFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

    @Test
    void newSchemaValidatorFactory_withClassLoader_discoversCustomFactory() {
        ClassLoader classLoader = new InMemoryClassLoader(getClass().getClassLoader(), CustomSchemaValidatorFactory.class);
        SchemaValidatorFactory factory = SchemaValidatorFactory.newSchemaValidatorFactory("3.1", classLoader);
        assertThat(factory).isNotNull().isInstanceOf(CustomSchemaValidatorFactory.class);
    }

    @Test
    void newSchemaValidatorFactory_withClassLoader_withUnsupportedVersion_throwsIllegalArgumentException() {
        ClassLoader classLoader = getClass().getClassLoader();
        assertThatThrownBy(() -> SchemaValidatorFactory.newSchemaValidatorFactory("1.0", classLoader))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void newSchemaValidatorFactory_withClassLoader_whenNoProviderFound_throwsIllegalStateException() {
        ClassLoader classLoader = ClassLoader.getPlatformClassLoader();
        assertThatThrownBy(() -> SchemaValidatorFactory.newSchemaValidatorFactory("3.1", classLoader))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SchemaValidatorFactory");
    }

    @Test
    void newSchemaValidatorFactory_withModuleLayer_withUnsupportedVersion_throwsIllegalArgumentException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> SchemaValidatorFactory.newSchemaValidatorFactory("1.0", bootLayer))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported JPA version: 1.0");
    }

    @Test
    void newSchemaValidatorFactory_withModuleLayer_whenNoProviderFound_throwsIllegalStateException() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        assertThatThrownBy(() -> SchemaValidatorFactory.newSchemaValidatorFactory("3.1", bootLayer))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SchemaValidatorFactory");
    }

}
