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

package persistence.tools;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * Contract for generating and exporting offline DDL scripts from a JPA entity metamodel
 * at build time without requiring a live database connection or running database server.
 *
 * <p>Implementations inspect compiled JPA entity classes and generate schema definition statements
 * (such as table definitions, primary keys, foreign key constraints, and indexes) or schema teardown
 * statements tailored to a configured database dialect.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * SchemaExporterFactory factory = SchemaExporterFactory.newSchemaExporterFactory("3.1");
 * SchemaExporter exporter = factory.newSchemaExporter(
 *     List.of("com.example.model.Product", "com.example.model.Supplier"),
 *     Map.of("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
 * );
 *
 * // Export schema creation statements to a DDL file
 * exporter.export(SchemaExporter.Action.CREATE, new File("build/schema/create-schema.sql"));
 * }</pre>
 *
 * @since 4.2.0
 * @see Action
 * @see persistence.tools.spi.SchemaExporterFactory
 */
public interface SchemaExporter {

    /**
     * Generates DDL statements for the specified {@link Action} and writes them to the target file.
     *
     * @param action the DDL generation mode ({@link Action#CREATE}, {@link Action#DROP}, or {@link Action#BOTH})
     * @param file   the destination file where the generated SQL script will be written
     * @throws IOException          if an I/O error occurs while creating or writing to the file
     * @throws NullPointerException if {@code action} or {@code file} is {@code null}
     */
    void export(Action action, File file) throws IOException;

    /**
     * Generates DDL statements for the specified {@link Action} and writes them to the provided output stream.
     *
     * @param action the DDL generation mode ({@link Action#CREATE}, {@link Action#DROP}, or {@link Action#BOTH})
     * @param stream the destination {@link OutputStream} to receive the generated SQL script
     * @throws IOException          if an I/O error occurs while writing to the output stream
     * @throws NullPointerException if {@code action} or {@code stream} is {@code null}
     */
    void export(Action action, OutputStream stream) throws IOException;

    /**
     * Generates DDL statements for the specified {@link Action} and writes them to the provided character writer.
     *
     * @param action the DDL generation mode ({@link Action#CREATE}, {@link Action#DROP}, or {@link Action#BOTH})
     * @param writer the destination {@link Writer} to receive the generated SQL script
     * @throws IOException          if an I/O error occurs while writing to the writer
     * @throws NullPointerException if {@code action} or {@code writer} is {@code null}
     */
    void export(Action action, Writer writer) throws IOException;

    /**
     * Defines the scope of DDL statements to generate during schema export.
     *
     * @since 4.2.0
     */
    enum Action {

        /**
         * Generates only schema creation statements (e.g., {@code CREATE TABLE}, constraints, and indexes).
         */
        CREATE,

        /**
         * Generates only schema teardown statements (e.g., {@code DROP TABLE}).
         */
        DROP,

        /**
         * Generates schema drop statements followed by schema creation statements.
         */
        BOTH
    }

}
