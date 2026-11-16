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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for Hibernate-related operations.
 *
 * <p>
 * This class provides shared helper methods for working with Hibernate persistence tools,
 * including merging configuration settings maps across precedence tiers, converting configuration
 * settings maps to strongly-typed enhancer options, converting filesystem references to URLs,
 * and constructing isolated class loaders for inspection of compiled entity classes.
 * </p>
 *
 * @since 1.0.0
 * @see HibernateEnhancerOption
 */
public class HibernateUtil {

    /**
     * Private constructor to prevent instantiation.
     */
    private HibernateUtil() {
        // Do not create instances of this class
    }

    /**
     * Merges predefined default settings with consumer-supplied overrides.
     *
     * <p>Keys present in {@code consumerSettings} override matching keys in
     * {@code defaults}. Any {@code null} map argument or entry with a
     * {@code null} value is safely ignored.</p>
     *
     * @param defaults         the baseline settings defined by the persistence provider
     * @param consumerSettings custom settings supplied by the caller to override defaults
     * @return an unmodifiable {@link Map} containing the merged settings
     * @since 1.1.0
     */
    public static Map<String, Object> mergeSettings(Map<String, ?> defaults,
                                                    Map<String, ?> consumerSettings) {
        return mergeSettings(defaults, consumerSettings, Collections.emptyMap());
    }

    /**
     * Merges predefined default settings, consumer-supplied overrides, and
     * enforced safety invariants according to a strict three-tier precedence:
     *
     * <ol>
     *   <li>{@code defaults} — baseline settings defined by the provider.</li>
     *   <li>{@code consumerSettings} — caller overrides that replace matching defaults.</li>
     *   <li>{@code enforcedSettings} — protected invariants that take final precedence
     *       and cannot be overwritten by consumer input.</li>
     * </ol>
     *
     * <p>Any {@code null} map argument or entry with a {@code null} key or value
     * is safely ignored.</p>
     *
     * @param defaults         the baseline settings defined by the persistence provider
     * @param consumerSettings custom settings supplied by the caller
     * @param enforcedSettings protected settings applied last so they cannot be overridden
     * @return an unmodifiable {@link Map} containing the merged settings
     * @since 1.1.0
     */
    public static Map<String, Object> mergeSettings(Map<String, ?> defaults,
                                                    Map<String, ?> consumerSettings,
                                                    Map<String, ?> enforcedSettings) {
        Map<String, Object> merged = new LinkedHashMap<>();
        putNonNullEntries(merged, defaults);
        putNonNullEntries(merged, consumerSettings);
        putNonNullEntries(merged, enforcedSettings);
        return Collections.unmodifiableMap(merged);
    }

    /**
     * Translates simplified JDBC connection properties into standard Hibernate
     * {@code hibernate.connection.*} configuration settings.
     *
     * <p>The following shorthand keys are mapped when present with non-null values:</p>
     * <ul>
     *   <li>{@code "driver"} &rarr; {@code "hibernate.connection.driver_class"}</li>
     *   <li>{@code "url"} &rarr; {@code "hibernate.connection.url"}</li>
     *   <li>{@code "username"} &rarr; {@code "hibernate.connection.username"}</li>
     *   <li>{@code "password"} &rarr; {@code "hibernate.connection.password"}</li>
     * </ul>
     *
     * <p>If {@code connectionProperties} is {@code null} or empty, an empty map is returned.</p>
     *
     * @param connectionProperties the simplified JDBC connection properties map; may be {@code null} or empty
     * @return a {@link Map} containing the corresponding Hibernate connection settings
     * @since 1.1.0
     */
    public static Map<String, Object> toConnectionSettings(Map<String, String> connectionProperties) {
        if (connectionProperties == null || connectionProperties.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> mapped = new LinkedHashMap<>();
        putIfPresent(mapped, "hibernate.connection.driver_class", connectionProperties.get("driver"));
        putIfPresent(mapped, "hibernate.connection.url", connectionProperties.get("url"));
        putIfPresent(mapped, "hibernate.connection.username", connectionProperties.get("username"));
        putIfPresent(mapped, "hibernate.connection.password", connectionProperties.get("password"));

        return mapped;
    }

    private static void putNonNullEntries(Map<String, Object> target, Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        source.forEach((key, value) -> {
            if (Objects.nonNull(key) && Objects.nonNull(value)) {
                target.put(key, value);
            }
        });
    }

    private static void putIfPresent(Map<String, Object> target, String key, String value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    /**
     * Converts a {@link Map} of string-keyed settings to a {@link Map} of {@link HibernateEnhancerOption} to boolean values.
     *
     * <p>
     * The keys of the returned map are the {@link HibernateEnhancerOption} instances corresponding to the string keys of the
     * input map, resolved via {@link HibernateEnhancerOption#from(String)}. The values of the returned map are the boolean values
     * of the input map.
     * </p>
     *
     * @param settings the settings map to convert, where keys represent property key strings and values are booleans
     * @return the converted settings map mapping each {@link HibernateEnhancerOption} to its boolean state
     * @throws IllegalArgumentException if any key in {@code settings} does not match a known {@link HibernateEnhancerOption}
     * @throws ClassCastException if any value in {@code settings} cannot be cast to {@link Boolean}
     */
    public static Map<HibernateEnhancerOption, Boolean> convertSettings(Map<String, Object> settings) {
        return settings.entrySet().stream().collect(Collectors.toMap(
                entry -> HibernateEnhancerOption.from(entry.getKey()),
                entry -> (Boolean) entry.getValue()
        ));
    }

    /**
     * Converts a {@link File} to a {@link URL}.
     *
     * @param file the file to convert
     * @return the corresponding {@link URL}
     * @throws RuntimeException if the file URI cannot be converted to a URL
     */
    public static URL toURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a {@link URLClassLoader} whose classpath consists of the
     * compiled classes directory followed by the supplied classpath files.
     *
     * @param parent                    the parent class loader to delegate to
     * @param classpathFilesCollection  additional classpath entries (JARs or directories) grouped as collections of file sets
     * @return a new {@link URLClassLoader} covering the specified classpath
     */
    public static URLClassLoader createURLClassLoader(ClassLoader parent, Collection<Set<File>> classpathFilesCollection) {
        URL[] urls = classpathFilesCollection.stream()
                .flatMap(Set::stream)
                .map(HibernateUtil::toURL)
                .toArray(URL[]::new);
        return new URLClassLoader(urls, parent);
    }

}
