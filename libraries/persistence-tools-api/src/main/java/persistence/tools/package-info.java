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
 * Core public API contracts and data types for JPersist Persistence Tools.
 *
 * <p>This package defines vendor-agnostic interfaces and descriptors for build-time
 * persistence validation, bytecode enhancement, schema verification, and named query
 * parsing:</p>
 *
 * <ul>
 *   <li>{@link persistence.tools.Enhancer} &ndash; contract for performing build-time bytecode
 *       enhancement on compiled JPA entity classes.</li>
 *   <li>{@link persistence.tools.NamedObjectParser} &ndash; parses and validates named JPQL
 *       queries and stored procedure declarations against the entity metamodel.</li>
 *   <li>{@link persistence.tools.NamedObjectDescriptor} &ndash; immutable descriptor representing
 *       a parsed named query or stored procedure.</li>
 *   <li>{@link persistence.tools.NamedObjectType} &ndash; enumerates the types of parsed
 *       persistence objects.</li>
 *   <li>{@link persistence.tools.SchemaValidator} &ndash; validates the entity metamodel
 *       against a live database schema.</li>
 *   <li>{@link persistence.tools.PersistenceLinter} &ndash; marker interface for all
 *       persistence linting operations.</li>
 * </ul>
 *
 * @since 4.0.0
 * @see persistence.tools.spi
 */
package persistence.tools;
