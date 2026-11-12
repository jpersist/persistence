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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class HibernateUtilTest {

    @TempDir
    Path tempDir;

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
}
