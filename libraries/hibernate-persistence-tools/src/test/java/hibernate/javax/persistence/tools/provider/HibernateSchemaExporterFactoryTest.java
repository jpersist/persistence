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

package hibernate.javax.persistence.tools.provider;

import hibernate.javax.persistence.tools.HibernateSchemaExporter;
import hibernate.javax.persistence.tools.TestEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import persistence.tools.spi.SchemaExporterFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HibernateSchemaExporterFactoryTest {

    @Test
    void getSupportedNamespace_returnsJavax() {
        var factory = new HibernateSchemaExporterFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void newSchemaExporter_returnsNonNullInstance() {
        var factory = new HibernateSchemaExporterFactory();

        var exporter = factory.newSchemaExporter(List.of(TestEntity.class.getName()));

        assertThat(exporter).isNotNull().isInstanceOf(HibernateSchemaExporter.class);
    }

    @Test
    void newSchemaExporter_withSettings_returnsNonNullInstance() {
        var factory = new HibernateSchemaExporterFactory();

        var exporter = factory.newSchemaExporter(List.of(TestEntity.class.getName()), Map.of("hibernate.format_sql", "true"));

        assertThat(exporter).isNotNull().isInstanceOf(HibernateSchemaExporter.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newSchemaExporterFactory_withLegacyVersions_returnsNonNullInstance(String version) {
        var factory = SchemaExporterFactory.newSchemaExporterFactory(version);

        assertThat(factory).isInstanceOf(HibernateSchemaExporterFactory.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newSchemaExporterFactory_withJakartaVersions_throwsIllegalStateException(String version) {
        assertThatThrownBy(() -> SchemaExporterFactory.newSchemaExporterFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jakarta");
    }

}
