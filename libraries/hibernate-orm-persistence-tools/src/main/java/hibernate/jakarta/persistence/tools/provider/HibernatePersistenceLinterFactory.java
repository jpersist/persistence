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

import persistence.tools.spi.PersistenceLinterFactory;

/**
 * Abstract base class for Jakarta Persistence linter factories.
 *
 * <p>Provides the common {@link #getSupportedNamespace()} implementation
 * that returns {@code "jakarta"}, shared by all Jakarta-specific factory
 * types such as {@link HibernateNamedObjectParserFactory} and
 * {@link HibernateSchemaValidatorFactory}.</p>
 *
 * @since 1.0.0
 * @see PersistenceLinterFactory
 * @see HibernateNamedObjectParserFactory
 * @see HibernateSchemaValidatorFactory
 */
public abstract class HibernatePersistenceLinterFactory extends HibernatePersistenceServiceProvider implements PersistenceLinterFactory {

    /** Creates a new {@code HibernatePersistenceLinterFactory}. */
    protected HibernatePersistenceLinterFactory() {}

}
