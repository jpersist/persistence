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
import java.io.OutputStream;
import java.io.Writer;

/**
 * Contract for generating and exporting offline DDL scripts from a JPA entity metamodel
 * at build time without requiring a live database connection.
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
     */
    void export(Action action, File file);

    /**
     * Generates DDL statements for the specified {@link Action} and writes them to the provided output stream.
     *
     * @param action the DDL generation mode ({@link Action#CREATE}, {@link Action#DROP}, or {@link Action#BOTH})
     * @param out    the destination {@link OutputStream} to receive the generated SQL script
     */
    void export(Action action, OutputStream out);

    /**
     * Generates DDL statements for the specified {@link Action} and writes them to the provided character writer.
     *
     * @param action the DDL generation mode ({@link Action#CREATE}, {@link Action#DROP}, or {@link Action#BOTH})
     * @param writer the destination {@link Writer} to receive the generated SQL script
     */
    void export(Action action, Writer writer);

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
