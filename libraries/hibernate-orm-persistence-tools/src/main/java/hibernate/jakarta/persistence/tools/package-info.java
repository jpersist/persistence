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
 * Jakarta Persistence tool implementations backed by Hibernate ORM&nbsp;6.
 *
 * <p>This package provides bytecode enhancement, semantic query model (SQM) query parsing,
 * and database schema validation components targeting the {@code jakarta.persistence}
 * namespace (JPA&nbsp;3.x):</p>
 *
 * <ul>
 *   <li>{@link hibernate.jakarta.persistence.tools.HibernateEnhancer} &ndash; transforms
 *       compiled entity class bytes using Hibernate ORM&nbsp;6 bytecode enhancement.</li>
 *   <li>{@link hibernate.jakarta.persistence.tools.HibernateNamedObjectParser} &ndash; compiles
 *       and validates named JPQL queries into SQM trees offline.</li>
 *   <li>{@link hibernate.jakarta.persistence.tools.HibernateSchemaValidator} &ndash; validates
 *       the JPA entity metamodel against a live database schema.</li>
 * </ul>
 *
 * @since 2.0.0
 * @see persistence.tools
 * @see hibernate.jakarta.persistence.tools.provider.HibernatePersistenceServiceProvider
 */
package hibernate.jakarta.persistence.tools;
