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

import persistence.tools.SchemaExporter;

import java.util.List;
import java.util.Map;

/**
 * Service Provider Interface (SPI) factory for creating {@link SchemaExporter}
 * instances capable of generating offline DDL scripts at build time.
 *
 * <p>Implementations are discovered at runtime through the {@link java.util.ServiceLoader} mechanism.
 * Each implementation declares the persistence namespace it supports (e.g.&nbsp;{@code "javax"}
 * for JPA&nbsp;2.x or {@code "jakarta"} for JPA&nbsp;3.x) so that the correct schema export strategy can be
 * resolved for the target JPA specification version.</p>
 *
 * <p>In JPMS environments, provider modules register their implementation with
 * {@code provides persistence.tools.spi.SchemaExporterFactory} in {@code module-info.java}.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * SchemaExporterFactory factory = SchemaExporterFactory.newSchemaExporterFactory("3.1");
 * SchemaExporter exporter = factory.newSchemaExporter(
 *     List.of("com.example.model.Customer", "com.example.model.Order"),
 *     Map.of("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect",
 *            "hibernate.format_sql", "true")
 * );
 * exporter.export(SchemaExporter.Action.BOTH, new File("build/schema/ddl.sql"));
 * }</pre>
 *
 * @since 4.2.0
 * @see SchemaExporter
 * @see PersistenceServiceProvider
 */
public interface SchemaExporterFactory extends PersistenceServiceProvider {

    /**
     * Creates a new {@link SchemaExporter} configured with the provider's default offline settings.
     *
     * @param entityClassNames the fully qualified names of the compiled JPA entity classes to export
     * @return a configured {@link SchemaExporter} instance
     */
    default SchemaExporter newSchemaExporter(List<String> entityClassNames) {
        return newSchemaExporter(entityClassNames, Map.of());
    }

    /**
     * Creates a new {@link SchemaExporter} configured with custom provider settings.
     *
     * @param entityClassNames the fully qualified names of the compiled JPA entity classes to export
     * @param settings         provider-specific configuration properties (such as dialect or schema options)
     *                         merged over default offline settings
     * @return a configured {@link SchemaExporter} instance
     */
    SchemaExporter newSchemaExporter(List<String> entityClassNames, Map<String, Object> settings);

    /**
     * Discovers and returns a {@link SchemaExporterFactory} supporting the given JPA specification version
     * using the current thread's context class loader.
     *
     * @param jpaVersion the target JPA version string (e.g., {@code "2.2"}, {@code "3.1"})
     * @return the matching {@link SchemaExporterFactory} provider
     * @throws IllegalArgumentException if {@code jpaVersion} is not recognized
     * @throws IllegalStateException    if no provider is registered for the resolved namespace
     */
    static SchemaExporterFactory newSchemaExporterFactory(String jpaVersion) {
        return PersistenceServiceProvider.getServiceProvider(jpaVersion, SchemaExporterFactory.class);
    }

    /**
     * Discovers and returns a {@link SchemaExporterFactory} supporting the given JPA specification version
     * using the specified {@link ClassLoader}.
     *
     * @param jpaVersion  the target JPA version string (e.g., {@code "2.2"}, {@code "3.1"})
     * @param classLoader the class loader used to locate provider implementations
     * @return the matching {@link SchemaExporterFactory} provider
     * @throws IllegalArgumentException if {@code jpaVersion} is not recognized
     * @throws IllegalStateException    if no provider is registered for the resolved namespace
     */
    static SchemaExporterFactory newSchemaExporterFactory(String jpaVersion, ClassLoader classLoader) {
        return PersistenceServiceProvider.getServiceProvider(jpaVersion, SchemaExporterFactory.class, classLoader);
    }

    /**
     * Discovers and returns a {@link SchemaExporterFactory} supporting the given JPA specification version
     * within the specified JPMS {@link ModuleLayer}.
     *
     * @param jpaVersion  the target JPA version string (e.g., {@code "2.2"}, {@code "3.1"})
     * @param moduleLayer the module layer used to locate provider implementations
     * @return the matching {@link SchemaExporterFactory} provider
     * @throws IllegalArgumentException if {@code jpaVersion} is not recognized
     * @throws IllegalStateException    if no provider is registered for the resolved namespace
     */
    static SchemaExporterFactory newSchemaExporterFactory(String jpaVersion, ModuleLayer moduleLayer) {
        return PersistenceServiceProvider.getServiceProvider(jpaVersion, SchemaExporterFactory.class, moduleLayer);
    }

}
