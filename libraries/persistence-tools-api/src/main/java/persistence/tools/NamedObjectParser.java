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

import persistence.tools.spi.NamedObjectParserFactory;

import java.util.Set;

/**
 * Service provider interface for parsing and validating named JPA objects
 * (queries and stored procedures) at build time.
 *
 * <p>Implementations compile a JPQL query string or validate a stored
 * procedure declaration against the project's entity metamodel, verifying
 * syntax, entity references, and property paths without requiring a live
 * database connection.</p>
 *
 * <p>This interface extends {@link AutoCloseable}. Implementations typically
 * hold heavyweight resources such as a Hibernate {@code SessionFactory} and
 * {@code ServiceRegistry} that must be released after use. Callers should
 * always use try-with-resources to ensure proper cleanup:</p>
 *
 * <pre>{@code
 * try (NamedObjectParser parser = factory.newNamedObjectParser(entityClassNames)) {
 *     parser.parseQuery(name, jpql);
 * }
 * }</pre>
 *
 * @since 2.0.0
 * @see NamedObjectParserFactory
 * @see NamedObjectDescriptor
 */
public interface NamedObjectParser extends PersistenceLinter, AutoCloseable {

    /**
     * Parses and semantically validates the given JPQL query string.
     *
     * <p>If the query contains syntax errors or references unknown entities
     * or properties, the implementation should throw an appropriate runtime
     * exception.</p>
     *
     * @param name        the logical name of the query (e.g.,&nbsp;the value of
     *                    {@code @NamedQuery.name})
     * @param queryString the JPQL query string to validate
     * @return a descriptor containing the query metadata and discovered
     *         parameters
     * @throws RuntimeException if the query is syntactically or semantically
     *                          invalid
     */
    NamedObjectDescriptor parseQuery(String name, String queryString);

    /**
     * Validates a named stored procedure declaration.
     *
     * <p>The implementation records the procedure name and its parameter
     * names into a {@link NamedObjectDescriptor} for downstream
     * consumption.</p>
     *
     * @param name           the logical name of the stored procedure
     *                       (e.g.&nbsp;the value of
     *                       {@code @NamedStoredProcedureQuery.name})
     * @param procedureName  the database procedure name
     * @param parameterNames the set of parameter names declared on the
     *                       stored procedure
     * @return a descriptor containing the stored procedure metadata
     */
    NamedObjectDescriptor parseStoredProcedure(String name, String procedureName, Set<String> parameterNames);

}
