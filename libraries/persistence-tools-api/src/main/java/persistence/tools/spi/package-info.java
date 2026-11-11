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

/**
 * Service Provider Interfaces (SPI) and discovery mechanisms for Persistence Tools.
 *
 * <p>This package defines the SPI abstractions implemented by persistence providers
 * (e.g.&nbsp;Hibernate ORM&nbsp;6 for Jakarta Persistence or Hibernate&nbsp;5 for legacy
 * {@code javax.persistence}). Implementations are discovered at runtime via the
 * {@link java.util.ServiceLoader} API based on the target JPA specification version.</p>
 *
 * <ul>
 *   <li>{@link persistence.tools.spi.PersistenceServiceProvider} &ndash; root interface for
 *       all persistence service providers, providing namespace resolution and version-based lookup.</li>
 *   <li>{@link persistence.tools.spi.PersistenceLinterFactory} &ndash; base factory contract for
 *       discovering persistence linter factories by JPA version.</li>
 *   <li>{@link persistence.tools.spi.NamedObjectParserFactory} &ndash; factory SPI for creating
 *       {@link persistence.tools.NamedObjectParser} instances.</li>
 *   <li>{@link persistence.tools.spi.SchemaValidatorFactory} &ndash; factory SPI for creating
 *       {@link persistence.tools.SchemaValidator} instances.</li>
 *   <li>{@link persistence.tools.spi.EnhancerProvider} &ndash; SPI for creating
 *       {@link persistence.tools.Enhancer} instances.</li>
 * </ul>
 *
 * @since 4.0.0
 * @see persistence.tools
 */
package persistence.tools.spi;
