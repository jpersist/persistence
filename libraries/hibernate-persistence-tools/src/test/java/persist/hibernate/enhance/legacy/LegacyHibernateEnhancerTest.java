package persist.hibernate.enhance.legacy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import persist.hibernate.enhance.spi.HibernateEnhancer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class LegacyHibernateEnhancerTest {

    private static final String PRODUCT_CLASS_NAME = "persist.hibernate.enhance.legacy.model.Product";

    private static File classesDir;

    private static byte[] loadClassBytes(String className) throws IOException {
        String resourcePath = className.replace('.', '/') + ".class";
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(is).as("Class resource for %s", className).isNotNull();
            return is.readAllBytes();
        }
    }

    private static File loadClassesDir() throws URISyntaxException {
        return new File(LegacyHibernateEnhancerTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    @BeforeAll
    static void setup() throws URISyntaxException {
        classesDir = loadClassesDir();
    }

    @Test
    void from_withClassesDir_returnsNonNullEnhancer() {
        var enhancer = new LegacyHibernateEnhancerProvider().getEnhancer(
                classesDir, Collections.emptySet(), Collections.emptyMap());

        assertThat(enhancer).isNotNull();
    }

    @Test
    void from_withClassesDir_returnsHibernateEnhancer() {
        var enhancer = new LegacyHibernateEnhancerProvider().getEnhancer(
                classesDir, Collections.emptySet(), Collections.emptyMap());

        assertThat(enhancer).isInstanceOf(HibernateEnhancer.class);
    }

    @Test
    void from_withClassLoader_returnsNonNullEnhancer() {
        var enhancer = LegacyHibernateEnhancer.from(
                Thread.currentThread().getContextClassLoader(), Collections.emptyMap());

        assertThat(enhancer).isNotNull();
    }

    @Test
    void from_withClassLoader_returnsHibernateEnhancer() {
        var enhancer = LegacyHibernateEnhancer.from(
                Thread.currentThread().getContextClassLoader(), Collections.emptyMap());

        assertThat(enhancer).isInstanceOf(HibernateEnhancer.class);
    }

    @Test
    void enhance_withEntityClass_returnsEnhancedBytes() throws IOException {
        var enhancer = LegacyHibernateEnhancer.from(
                Thread.currentThread().getContextClassLoader(), Collections.emptyMap());
        byte[] originalBytes = loadClassBytes(PRODUCT_CLASS_NAME);

        byte[] enhancedBytes = enhancer.enhance(PRODUCT_CLASS_NAME, originalBytes);

        assertThat(enhancedBytes).isNotNull();
    }

    @Test
    void enhance_withEntityClass_returnsModifiedBytes() throws IOException {
        var enhancer = LegacyHibernateEnhancer.from(
                Thread.currentThread().getContextClassLoader(), Collections.emptyMap());
        byte[] originalBytes = loadClassBytes(PRODUCT_CLASS_NAME);

        byte[] enhancedBytes = enhancer.enhance(PRODUCT_CLASS_NAME, originalBytes);

        assertThat(enhancedBytes).isNotEqualTo(originalBytes);
    }

    @Test
    void enhance_withAllOptionsEnabled_returnsEnhancedBytes() throws IOException {
        Map<HibernateEnhancer.Option, Boolean> options = Map.of(
                HibernateEnhancer.Option.LAZY_INITIALIZATION, true,
                HibernateEnhancer.Option.DIRTY_TRACKING, true,
                HibernateEnhancer.Option.ASSOCIATION_MANAGEMENT, true,
                HibernateEnhancer.Option.EXTENDED_ENHANCEMENT, true
        );
        var enhancer = LegacyHibernateEnhancer.from(
                Thread.currentThread().getContextClassLoader(), options);
        byte[] originalBytes = loadClassBytes(PRODUCT_CLASS_NAME);

        byte[] enhancedBytes = enhancer.enhance(PRODUCT_CLASS_NAME, originalBytes);

        assertThat(enhancedBytes).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(HibernateEnhancer.Option.class)
    void enhance_withSingleOptionEnabled_doesNotThrowException(HibernateEnhancer.Option option) throws IOException {
        Map<HibernateEnhancer.Option, Boolean> options = Map.of(option, true);
        var enhancer = LegacyHibernateEnhancer.from(
                Thread.currentThread().getContextClassLoader(), options);
        byte[] originalBytes = loadClassBytes(PRODUCT_CLASS_NAME);

        assertThatCode(() -> enhancer.enhance(PRODUCT_CLASS_NAME, originalBytes))
                .doesNotThrowAnyException();
    }

    @Test
    void enhance_withAllOptionsDisabled_returnsEnhancedBytes() throws IOException {
        Map<HibernateEnhancer.Option, Boolean> options = Map.of(
                HibernateEnhancer.Option.LAZY_INITIALIZATION, false,
                HibernateEnhancer.Option.DIRTY_TRACKING, false,
                HibernateEnhancer.Option.ASSOCIATION_MANAGEMENT, false,
                HibernateEnhancer.Option.EXTENDED_ENHANCEMENT, false
        );
        var enhancer = LegacyHibernateEnhancer.from(
                Thread.currentThread().getContextClassLoader(), options);
        byte[] originalBytes = loadClassBytes(PRODUCT_CLASS_NAME);

        byte[] enhancedBytes = enhancer.enhance(PRODUCT_CLASS_NAME, originalBytes);

        assertThat(enhancedBytes).isNotNull();
    }

}
