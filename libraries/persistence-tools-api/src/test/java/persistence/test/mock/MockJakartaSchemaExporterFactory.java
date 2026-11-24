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

package persistence.test.mock;

import persistence.tools.SchemaExporter;
import persistence.tools.spi.SchemaExporterFactory;

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class MockJakartaSchemaExporterFactory extends MockJakartaPersistenceServiceProvider implements SchemaExporterFactory {

    public MockJakartaSchemaExporterFactory() {
    }

    @Override
    public SchemaExporter newSchemaExporter(List<String> entityClassNames, Map<String, Object> settings) {
        return new SchemaExporter() {
            @Override
            public void export(Action action, File file) {
            }

            @Override
            public void export(Action action, OutputStream stream) {
            }

            @Override
            public void export(Action action, Writer writer) {
            }
        };
    }
}
