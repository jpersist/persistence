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

package hibernate.jakarta.persistence.tools;

import hibernate.base.persistence.tools.HibernateEnhancerOption;
import hibernate.jakarta.persistence.tools.provider.HibernateEnhancerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import persistence.tools.Enhancer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class HibernateEnhancerTest {

    private static final String PRODUCT_CLASS_NAME = "hibernate.jakarta.persistence.tools.model.Product";

    private static File classesDir;

    private static byte[] loadClassBytes(String className) throws IOException {
        String resourcePath = className.replace('.', '/') + ".class";
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(is).as("Class resource for %s", className).isNotNull();
            return is.readAllBytes();
        }
    }

    private static File loadClassesDir() throws URISyntaxException {
        return new File(HibernateEnhancerTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    @BeforeAll
    static void setup() throws URISyntaxException {
        classesDir = loadClassesDir();
    }

    @Test
    void from_withClassesDir_returnsNonNullEnhancer() {
        var enhancer = new HibernateEnhancerProvider().getEnhancer(
                List.of(Collections.singleton(classesDir), Collections.emptySet()), Collections.emptyMap());

        assertThat(enhancer).isNotNull();
    }

    @Test
    void from_withClassesDir_returnsHibernateEnhancer() {
        var enhancer = new HibernateEnhancerProvider().getEnhancer(
                List.of(Collections.singleton(classesDir), Collections.emptySet()), Collections.emptyMap());

        assertThat(enhancer).isInstanceOf(Enhancer.class);
    }

    @Test
    void from_withClassLoader_returnsNonNullEnhancer() {
        var enhancer = HibernateEnhancer.from(
                Thread.currentThread().getContextClassLoader(), Collections.emptyMap());

        assertThat(enhancer).isNotNull();
    }

    @Test
    void from_withClassLoader_returnsHibernateEnhancer() {
        var enhancer = HibernateEnhancer.from(
                Thread.currentThread().getContextClassLoader(), Collections.emptyMap());

        assertThat(enhancer).isInstanceOf(Enhancer.class);
    }

    @Test
    void enhance_withEntityClass_returnsEnhancedBytes() throws IOException {
        var enhancer = HibernateEnhancer.from(
                Thread.currentThread().getContextClassLoader(), Collections.emptyMap());
        byte[] originalBytes = loadClassBytes(PRODUCT_CLASS_NAME);

        byte[] enhancedBytes = enhancer.enhance(PRODUCT_CLASS_NAME, originalBytes);

        assertThat(enhancedBytes).isNotNull();
    }

    @Test
    void enhance_withEntityClass_returnsModifiedBytes() throws IOException {
        var enhancer = HibernateEnhancer.from(
                Thread.currentThread().getContextClassLoader(), Collections.emptyMap());
        byte[] originalBytes = loadClassBytes(PRODUCT_CLASS_NAME);

        byte[] enhancedBytes = enhancer.enhance(PRODUCT_CLASS_NAME, originalBytes);

        assertThat(enhancedBytes).isNotEqualTo(originalBytes);
    }

    @Test
    void enhance_withAllOptionsEnabled_returnsEnhancedBytes() throws IOException {
        Map<String, Object> settings = Map.of(
                HibernateEnhancerOption.LAZY_INITIALIZATION.value(), Boolean.TRUE,
                HibernateEnhancerOption.DIRTY_TRACKING.value(), Boolean.TRUE,
                HibernateEnhancerOption.ASSOCIATION_MANAGEMENT.value(), Boolean.TRUE,
                HibernateEnhancerOption.EXTENDED_ENHANCEMENT.value(), Boolean.TRUE
        );
        var enhancer = HibernateEnhancer.from(
                Thread.currentThread().getContextClassLoader(), settings);
        byte[] originalBytes = loadClassBytes(PRODUCT_CLASS_NAME);

        byte[] enhancedBytes = enhancer.enhance(PRODUCT_CLASS_NAME, originalBytes);

        assertThat(enhancedBytes).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(HibernateEnhancerOption.class)
    void enhance_withSingleOptionEnabled_doesNotThrowException(HibernateEnhancerOption option) throws IOException {
        Map<String, Object> settings = Map.of(option.value(), Boolean.TRUE);
        var enhancer = HibernateEnhancer.from(
                Thread.currentThread().getContextClassLoader(), settings);
        byte[] originalBytes = loadClassBytes(PRODUCT_CLASS_NAME);

        assertThatCode(() -> enhancer.enhance(PRODUCT_CLASS_NAME, originalBytes))
                .doesNotThrowAnyException();
    }

    @Test
    void enhance_withAllOptionsDisabled_returnsEnhancedBytes() throws IOException {
        Map<String, Object> settings = Map.of(
                HibernateEnhancerOption.LAZY_INITIALIZATION.value(), Boolean.FALSE,
                HibernateEnhancerOption.DIRTY_TRACKING.value(), Boolean.FALSE,
                HibernateEnhancerOption.ASSOCIATION_MANAGEMENT.value(), Boolean.FALSE,
                HibernateEnhancerOption.EXTENDED_ENHANCEMENT.value(), Boolean.FALSE
        );
        var enhancer = HibernateEnhancer.from(
                Thread.currentThread().getContextClassLoader(), settings);
        byte[] originalBytes = loadClassBytes(PRODUCT_CLASS_NAME);

        byte[] enhancedBytes = enhancer.enhance(PRODUCT_CLASS_NAME, originalBytes);

        assertThat(enhancedBytes).isNotNull();
    }

}
