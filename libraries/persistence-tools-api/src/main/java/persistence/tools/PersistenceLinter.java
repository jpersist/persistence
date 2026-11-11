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

/**
 * Marker interface for all persistence linting operations.
 *
 * <p>This is the root type in the linter hierarchy. Concrete linter
 * contracts such as {@link NamedObjectParser} and {@link SchemaValidator}
 * extend this interface to declare specific build-time validation
 * capabilities.</p>
 *
 * @since 2.1.0
 * @see NamedObjectParser
 * @see SchemaValidator
 */
public interface PersistenceLinter {
}
