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

import persistence.tools.spi.SchemaValidatorFactory;

import java.util.Map;

/**
 * Service provider interface for validating a JPA persistence schema
 * against a live database at build time.
 *
 * <p>Implementations use the entity metamodel built from the discovered
 * annotated classes and compare it with the actual database schema
 * reachable through the supplied connection properties.</p>
 *
 * @since 2.1.0
 * @see SchemaValidatorFactory
 * @see PersistenceLinter
 */
public interface SchemaValidator extends PersistenceLinter {

    /**
     * Validates the persistence schema against the database described by
     * the given connection properties.
     *
     * <p>The map is expected to contain at least the keys
     * {@code "driver"}, {@code "url"}, {@code "username"}, and
     * {@code "password"}.</p>
     *
     * @param connectionProperties JDBC connection properties used to
     *                             reach the target database
     * @throws RuntimeException if the schema does not match the entity
     *                          metamodel
     */
    void validateSchema(Map<String, String> connectionProperties);

}
