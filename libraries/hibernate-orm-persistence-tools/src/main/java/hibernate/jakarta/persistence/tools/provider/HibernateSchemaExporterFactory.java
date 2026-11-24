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

package hibernate.jakarta.persistence.tools.provider;

import hibernate.jakarta.persistence.tools.HibernateSchemaExporter;
import persistence.tools.SchemaExporter;
import persistence.tools.spi.SchemaExporterFactory;

import java.util.List;
import java.util.Map;

/**
 * Hibernate ORM&nbsp;6 provider factory for {@link HibernateSchemaExporter}.
 *
 * <p>This factory is registered as a {@link java.util.ServiceLoader} provider and creates
 * {@link HibernateSchemaExporter} instances targeting the {@code "jakarta"} persistence namespace.</p>
 *
 * @since 2.2.0
 * @see HibernateSchemaExporter
 * @see SchemaExporterFactory
 * @see HibernatePersistenceServiceProvider
 */
public class HibernateSchemaExporterFactory extends HibernatePersistenceServiceProvider implements SchemaExporterFactory {

    /**
     * Creates a new {@code HibernateSchemaExporterFactory}.
     */
    public HibernateSchemaExporterFactory() {
        // Default no args constructor
    }

    /** {@inheritDoc} */
    @Override
    public SchemaExporter newSchemaExporter(List<String> entityClassNames, Map<String, Object> settings) {
        return HibernateSchemaExporter.of(entityClassNames, settings, Thread.currentThread().getContextClassLoader());
    }

}
