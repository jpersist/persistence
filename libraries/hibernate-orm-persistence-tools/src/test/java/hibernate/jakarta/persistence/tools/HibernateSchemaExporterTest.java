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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import persistence.tools.SchemaExporter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HibernateSchemaExporterTest {

    private static final List<String> MODEL_ENTITY_CLASS_NAMES = List.of(
        "hibernate.jakarta.persistence.tools.model.Product",
        "hibernate.jakarta.persistence.tools.model.Supplier",
        "hibernate.jakarta.persistence.tools.model.Warehouse",
        "hibernate.jakarta.persistence.tools.model.StockMovement"
    );

    @Test
    void of_withNullEntityClassNames_throwsNullPointerException() {
        assertThatThrownBy(() -> HibernateSchemaExporter.of(null, Collections.emptyMap(), getClass().getClassLoader()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void of_withNullClassLoader_throwsNullPointerException() {
        assertThatThrownBy(() -> HibernateSchemaExporter.of(List.of("SomeClass"), Collections.emptyMap(), null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void export_withNullAction_throwsNullPointerException(@TempDir Path tempDir) {
        HibernateSchemaExporter exporter = HibernateSchemaExporter.of(
            MODEL_ENTITY_CLASS_NAMES,
            Collections.emptyMap(),
            getClass().getClassLoader()
        );

        File file = tempDir.resolve("ddl.sql").toFile();
        assertThatThrownBy(() -> exporter.export(null, file))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> exporter.export(null, new ByteArrayOutputStream()))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> exporter.export(null, new StringWriter()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void export_withNullTargets_throwsNullPointerException() {
        HibernateSchemaExporter exporter = HibernateSchemaExporter.of(
            MODEL_ENTITY_CLASS_NAMES,
            Collections.emptyMap(),
            getClass().getClassLoader()
        );

        assertThatThrownBy(() -> exporter.export(SchemaExporter.Action.CREATE, (File) null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> exporter.export(SchemaExporter.Action.CREATE, (OutputStream) null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> exporter.export(SchemaExporter.Action.CREATE, (Writer) null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void export_createActionToWriter_generatesTableCreationStatements() throws IOException {
        HibernateSchemaExporter exporter = HibernateSchemaExporter.of(
            MODEL_ENTITY_CLASS_NAMES,
            Collections.emptyMap(),
            getClass().getClassLoader()
        );

        StringWriter writer = new StringWriter();
        exporter.export(SchemaExporter.Action.CREATE, writer);
        String ddl = writer.toString();

        assertThat(ddl)
            .containsIgnoringCase("create table Product")
            .containsIgnoringCase("create table Supplier")
            .containsIgnoringCase("create table Warehouse")
            .containsIgnoringCase("create table stock_movement");
    }

    @Test
    void export_dropActionToWriter_generatesTableDropStatements() throws IOException {
        HibernateSchemaExporter exporter = HibernateSchemaExporter.of(
            MODEL_ENTITY_CLASS_NAMES,
            Collections.emptyMap(),
            getClass().getClassLoader()
        );

        StringWriter writer = new StringWriter();
        exporter.export(SchemaExporter.Action.DROP, writer);
        String ddl = writer.toString();

        assertThat(ddl)
            .containsIgnoringCase("drop table")
            .containsIgnoringCase("Product")
            .containsIgnoringCase("Supplier")
            .containsIgnoringCase("Warehouse")
            .containsIgnoringCase("stock_movement");
    }

    @Test
    void export_bothActionToWriter_generatesDropAndCreateStatements() throws IOException {
        HibernateSchemaExporter exporter = HibernateSchemaExporter.of(
            MODEL_ENTITY_CLASS_NAMES,
            Collections.emptyMap(),
            getClass().getClassLoader()
        );

        StringWriter writer = new StringWriter();
        exporter.export(SchemaExporter.Action.BOTH, writer);
        String ddl = writer.toString();

        assertThat(ddl)
            .containsIgnoringCase("drop table")
            .containsIgnoringCase("create table Product")
            .containsIgnoringCase("create table Supplier")
            .containsIgnoringCase("create table Warehouse")
            .containsIgnoringCase("create table stock_movement");
    }

    @Test
    void export_createActionToFile_writesToFile(@TempDir Path tempDir) throws IOException {
        HibernateSchemaExporter exporter = HibernateSchemaExporter.of(
            MODEL_ENTITY_CLASS_NAMES,
            Collections.emptyMap(),
            getClass().getClassLoader()
        );

        File outputFile = tempDir.resolve("create-schema.sql").toFile();
        exporter.export(SchemaExporter.Action.CREATE, outputFile);

        assertThat(outputFile).exists().isNotEmpty();
        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        assertThat(content)
            .containsIgnoringCase("create table Product")
            .containsIgnoringCase("create table stock_movement");
    }

    @Test
    void export_createActionToOutputStream_writesToStream() throws IOException {
        HibernateSchemaExporter exporter = HibernateSchemaExporter.of(
            MODEL_ENTITY_CLASS_NAMES,
            Collections.emptyMap(),
            getClass().getClassLoader()
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        exporter.export(SchemaExporter.Action.CREATE, out);

        String content = out.toString(StandardCharsets.UTF_8);
        assertThat(content)
            .containsIgnoringCase("create table Product")
            .containsIgnoringCase("create table stock_movement");
    }

    @Test
    void export_withCustomSettings_appliesSettingsSuccessfully() throws IOException {
        Map<String, Object> customSettings = Map.of(
            "hibernate.format_sql", "true"
        );
        HibernateSchemaExporter exporter = HibernateSchemaExporter.of(
            MODEL_ENTITY_CLASS_NAMES,
            customSettings,
            getClass().getClassLoader()
        );

        StringWriter writer = new StringWriter();
        exporter.export(SchemaExporter.Action.CREATE, writer);
        String ddl = writer.toString();

        assertThat(ddl).isNotEmpty().containsIgnoringCase("create table Product");
    }

    @Test
    void export_withEmptyEntityList_generatesEmptyOrMinimalDdl() throws IOException {
        HibernateSchemaExporter exporter = HibernateSchemaExporter.of(
            List.of(),
            Collections.emptyMap(),
            getClass().getClassLoader()
        );

        StringWriter writer = new StringWriter();
        exporter.export(SchemaExporter.Action.CREATE, writer);
        String ddl = writer.toString();

        assertThat(ddl).doesNotContain("create table");
    }

}
