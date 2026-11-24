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

package hibernate.base.persistence.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class HibernateUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void mergeSettings_twoArgs_overridesDefaultsWithConsumerSettings() {
        Map<String, Object> defaults = Map.of(
                "hibernate.show_sql", "false",
                "hibernate.format_sql", "false",
                "hibernate.hbm2ddl.auto", "none"
        );
        Map<String, Object> consumer = Map.of(
                "hibernate.show_sql", "true",
                "hibernate.dialect", "org.hibernate.dialect.H2Dialect"
        );

        Map<String, Object> result = HibernateUtil.mergeSettings(defaults, consumer);

        assertThat(result)
                .isNotNull()
                .hasSize(4)
                .containsEntry("hibernate.show_sql", "true")
                .containsEntry("hibernate.format_sql", "false")
                .containsEntry("hibernate.hbm2ddl.auto", "none")
                .containsEntry("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
    }

    @Test
    void mergeSettings_threeArgs_appliesStrictThreeTierPrecedence() {
        Map<String, Object> defaults = Map.of(
                "key1", "default1",
                "key2", "default2",
                "key3", "default3",
                "key4", "default4"
        );
        Map<String, Object> consumer = Map.of(
                "key2", "consumer2",
                "key3", "consumer3",
                "key5", "consumer5"
        );
        Map<String, Object> enforced = Map.of(
                "key3", "enforced3",
                "key6", "enforced6"
        );

        Map<String, Object> result = HibernateUtil.mergeSettings(defaults, consumer, enforced);

        assertThat(result)
                .isNotNull()
                .hasSize(6)
                .containsEntry("key1", "default1")
                .containsEntry("key2", "consumer2")
                .containsEntry("key3", "enforced3")
                .containsEntry("key4", "default4")
                .containsEntry("key5", "consumer5")
                .containsEntry("key6", "enforced6");
    }

    @Test
    void mergeSettings_withNullAndEmptyMaps_handlesSafely() {
        assertThat(HibernateUtil.mergeSettings(null, null))
                .isNotNull()
                .isEmpty();

        assertThat(HibernateUtil.mergeSettings(null, null, null))
                .isNotNull()
                .isEmpty();

        Map<String, Object> sample = Map.of("key", "value");

        assertThat(HibernateUtil.mergeSettings(sample, null))
                .containsExactly(entry("key", "value"));

        assertThat(HibernateUtil.mergeSettings(null, sample))
                .containsExactly(entry("key", "value"));

        assertThat(HibernateUtil.mergeSettings(null, null, sample))
                .containsExactly(entry("key", "value"));

        assertThat(HibernateUtil.mergeSettings(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()))
                .isEmpty();
    }

    @Test
    void mergeSettings_withNullKeysOrValues_safelyIgnoresNullEntries() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("validDefault", "value1");
        defaults.put(null, "nullKeyDefault");
        defaults.put("nullValueDefault", null);

        Map<String, Object> consumer = new HashMap<>();
        consumer.put("validConsumer", "value2");
        consumer.put(null, "nullKeyConsumer");
        consumer.put("nullValueConsumer", null);

        Map<String, Object> enforced = new HashMap<>();
        enforced.put("validEnforced", "value3");
        enforced.put(null, "nullKeyEnforced");
        enforced.put("nullValueEnforced", null);

        Map<String, Object> result = HibernateUtil.mergeSettings(defaults, consumer, enforced);

        assertThat(result)
                .isNotNull()
                .hasSize(3)
                .containsEntry("validDefault", "value1")
                .containsEntry("validConsumer", "value2")
                .containsEntry("validEnforced", "value3")
                .doesNotContainKey(null)
                .doesNotContainValue(null);
    }

    @Test
    void mergeSettings_returnsUnmodifiableMap() {
        Map<String, Object> result = HibernateUtil.mergeSettings(
                Map.of("key", "value"),
                Map.of()
        );

        assertThatThrownBy(() -> result.put("newKey", "newValue"))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> result.remove("key"))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(result::clear)
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mergeSettings_preservesKeyOrder() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("d1", 1);
        defaults.put("d2", 2);

        Map<String, Object> consumer = new LinkedHashMap<>();
        consumer.put("c1", 3);
        consumer.put("d1", 10);

        Map<String, Object> enforced = new LinkedHashMap<>();
        enforced.put("e1", 4);
        enforced.put("c1", 30);

        Map<String, Object> result = HibernateUtil.mergeSettings(defaults, consumer, enforced);

        assertThat(result.keySet()).containsExactly("d1", "d2", "c1", "e1");
        assertThat(result.values()).containsExactly(10, 2, 30, 4);
    }

    @Test
    void toConnectionSettings_withAllProperties_mapsToHibernateKeys() {
        Map<String, String> connectionProps = Map.of(
            "driver", "org.h2.Driver",
            "url", "jdbc:h2:mem:testdb",
            "username", "sa",
            "password", "secret"
        );

        Map<String, Object> result = HibernateUtil.toConnectionSettings(connectionProps);

        assertThat(result)
            .isNotNull()
            .hasSize(4)
            .containsEntry("hibernate.connection.driver_class", "org.h2.Driver")
            .containsEntry("hibernate.connection.url", "jdbc:h2:mem:testdb")
            .containsEntry("hibernate.connection.username", "sa")
            .containsEntry("hibernate.connection.password", "secret");
    }

    @Test
    void toConnectionSettings_withMissingOrNullValues_ignoresNullEntries() {
        Map<String, String> connectionProps = new HashMap<>();
        connectionProps.put("driver", "org.h2.Driver");
        connectionProps.put("url", "jdbc:h2:mem:testdb");
        connectionProps.put("username", "sa");
        connectionProps.put("password", null);

        Map<String, Object> result = HibernateUtil.toConnectionSettings(connectionProps);

        assertThat(result)
            .isNotNull()
            .hasSize(3)
            .containsEntry("hibernate.connection.driver_class", "org.h2.Driver")
            .containsEntry("hibernate.connection.url", "jdbc:h2:mem:testdb")
            .containsEntry("hibernate.connection.username", "sa")
            .doesNotContainKey("hibernate.connection.password");
    }

    @Test
    void toConnectionSettings_withNullOrEmptyMap_returnsEmptyMap() {
        assertThat(HibernateUtil.toConnectionSettings(null))
            .isNotNull()
            .isEmpty();

        assertThat(HibernateUtil.toConnectionSettings(Collections.emptyMap()))
            .isNotNull()
            .isEmpty();
    }

    @Test
    void toConnectionSettings_ignoresUnknownKeys() {
        Map<String, String> connectionProps = Map.of(
            "url", "jdbc:h2:mem:testdb",
            "unknown.key", "someValue"
        );

        Map<String, Object> result = HibernateUtil.toConnectionSettings(connectionProps);

        assertThat(result)
            .isNotNull()
            .hasSize(1)
            .containsEntry("hibernate.connection.url", "jdbc:h2:mem:testdb")
            .doesNotContainKey("unknown.key");
    }

    @Test
    void convertSettings_withValidOptions_returnsConvertedMap() {
        Map<String, Object> settings = Map.of(
                HibernateEnhancerOption.LAZY_INITIALIZATION.value(), Boolean.TRUE,
                HibernateEnhancerOption.DIRTY_TRACKING.value(), Boolean.FALSE,
                HibernateEnhancerOption.ASSOCIATION_MANAGEMENT.value(), Boolean.TRUE,
                HibernateEnhancerOption.EXTENDED_ENHANCEMENT.value(), Boolean.FALSE
        );

        Map<HibernateEnhancerOption, Boolean> result = HibernateUtil.convertSettings(settings);

        assertThat(result)
                .isNotNull()
                .hasSize(4)
                .containsEntry(HibernateEnhancerOption.LAZY_INITIALIZATION, true)
                .containsEntry(HibernateEnhancerOption.DIRTY_TRACKING, false)
                .containsEntry(HibernateEnhancerOption.ASSOCIATION_MANAGEMENT, true)
                .containsEntry(HibernateEnhancerOption.EXTENDED_ENHANCEMENT, false);
    }

    @Test
    void convertSettings_withSubsetOfOptions_returnsConvertedMap() {
        Map<String, Object> settings = Map.of(
                HibernateEnhancerOption.LAZY_INITIALIZATION.value(), Boolean.TRUE
        );

        Map<HibernateEnhancerOption, Boolean> result = HibernateUtil.convertSettings(settings);

        assertThat(result)
                .isNotNull()
                .hasSize(1)
                .containsEntry(HibernateEnhancerOption.LAZY_INITIALIZATION, true);
    }

    @Test
    void convertSettings_withEmptyMap_returnsEmptyMap() {
        Map<HibernateEnhancerOption, Boolean> result = HibernateUtil.convertSettings(Collections.emptyMap());

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void convertSettings_withInvalidOptionKey_throwsIllegalArgumentException() {
        Map<String, Object> settings = Map.of(
                "invalid.enhancer.option", Boolean.TRUE
        );

        assertThatIllegalArgumentException()
                .isThrownBy(() -> HibernateUtil.convertSettings(settings))
                .withMessage("Unknown option: invalid.enhancer.option");
    }

    @Test
    void convertSettings_withNonBooleanValue_throwsClassCastException() {
        Map<String, Object> settings = Map.of(
                HibernateEnhancerOption.LAZY_INITIALIZATION.value(), "true"
        );

        assertThatThrownBy(() -> HibernateUtil.convertSettings(settings))
                .isInstanceOf(ClassCastException.class);
    }

    @Test
    void toURL_withStandardFile_returnsCorrectURL() throws Exception {
        File file = tempDir.resolve("sample.txt").toFile();
        boolean created = file.createNewFile();
        assertThat(created).isTrue();

        URL url = HibernateUtil.toURL(file);

        assertThat(url).isNotNull().isEqualTo(file.toURI().toURL());
    }

    @Test
    void toURL_withDirectory_returnsDirectoryURL() throws Exception {
        File directory = tempDir.resolve("sub-dir").toFile();
        boolean created = directory.mkdir();
        assertThat(created).isTrue();

        URL url = HibernateUtil.toURL(directory);

        assertThat(url).isNotNull().isEqualTo(directory.toURI().toURL());
    }

    @Test
    void toURL_withFileWithSpacesAndSpecialChars_returnsCorrectURL() throws Exception {
        File file = tempDir.resolve("test file with spaces & symbols.jar").toFile();
        boolean created = file.createNewFile();
        assertThat(created).isTrue();

        URL url = HibernateUtil.toURL(file);

        assertThat(url).isNotNull().isEqualTo(file.toURI().toURL());
    }

    @Test
    void toURL_whenMalformedURLExceptionOccurs_throwsRuntimeException() {
        File malformedFile = new File("invalid-file") {
            @Override
            public URI toURI() {
                return URI.create("invalid-protocol://dummy/path");
            }
        };

        assertThatRuntimeException()
                .isThrownBy(() -> HibernateUtil.toURL(malformedFile))
                .withCauseInstanceOf(MalformedURLException.class);
    }

    @Test
    void createURLClassLoader_withValidClasspathCollections_createsClassLoaderWithAllUrls() throws IOException {
        File dir1 = tempDir.resolve("classes1").toFile();
        File dir2 = tempDir.resolve("classes2").toFile();
        File jar1 = tempDir.resolve("lib1.jar").toFile();
        assertThat(dir1.mkdir()).isTrue();
        assertThat(dir2.mkdir()).isTrue();
        assertThat(jar1.createNewFile()).isTrue();

        Set<File> set1 = new LinkedHashSet<>(List.of(dir1, jar1));
        Set<File> set2 = new LinkedHashSet<>(List.of(dir2));
        List<Set<File>> classpathFiles = List.of(set1, set2);

        ClassLoader parent = ClassLoader.getPlatformClassLoader();

        try (URLClassLoader classLoader = HibernateUtil.createURLClassLoader(parent, classpathFiles)) {
            assertThat(classLoader).isNotNull();
            assertThat(classLoader.getParent()).isSameAs(parent);
            assertThat(classLoader.getURLs()).containsExactly(
                    dir1.toURI().toURL(),
                    jar1.toURI().toURL(),
                    dir2.toURI().toURL()
            );
        }
    }

    @Test
    void createURLClassLoader_withEmptyClasspath_createsEmptyClassLoader() throws IOException {
        ClassLoader parent = getClass().getClassLoader();

        try (URLClassLoader classLoader = HibernateUtil.createURLClassLoader(parent, Collections.emptyList())) {
            assertThat(classLoader).isNotNull();
            assertThat(classLoader.getParent()).isSameAs(parent);
            assertThat(classLoader.getURLs()).isEmpty();
        }
    }

    @Test
    void createURLClassLoader_withEmptyInnerSets_ignoresEmptySets() throws IOException {
        File jar = tempDir.resolve("lib.jar").toFile();
        assertThat(jar.createNewFile()).isTrue();

        List<Set<File>> classpathFiles = List.of(
                Collections.emptySet(),
                Set.of(jar),
                Collections.emptySet()
        );

        ClassLoader parent = ClassLoader.getSystemClassLoader();

        try (URLClassLoader classLoader = HibernateUtil.createURLClassLoader(parent, classpathFiles)) {
            assertThat(classLoader).isNotNull();
            assertThat(classLoader.getURLs()).containsExactly(jar.toURI().toURL());
        }
    }

    @Test
    void shouldReturnDefaultOfflineDDLSettings() {
        Map<String, Object> defaults = HibernateUtil.defaultOfflineDDLSettings();

        assertThat(defaults)
            .containsEntry("hibernate.hbm2ddl.delimiter", ";")
            .containsEntry("hibernate.format_sql", "true");
    }

    @Test
    void shouldBridgeOutputStreamToUTF8WriterWithoutClosingStream() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer writer = HibernateUtil.toWriter(out);

        writer.write("CREATE TABLE orders (id BIGINT PRIMARY KEY);");
        writer.flush();

        assertThat(out.toString(StandardCharsets.UTF_8))
            .isEqualTo("CREATE TABLE orders (id BIGINT PRIMARY KEY);");
    }

    @Test
    void shouldRejectNullOutputStreamInToUTF8Writer() {
        assertThatNullPointerException()
            .isThrownBy(() -> HibernateUtil.toWriter((OutputStream) null))
            .withMessage("OutputStream must not be null");
    }
}
