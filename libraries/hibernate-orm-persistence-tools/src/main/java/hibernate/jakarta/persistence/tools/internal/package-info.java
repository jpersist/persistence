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
 * Internal helper components and Hibernate ORM&nbsp;6 SPI implementations for the
 * Jakarta Persistence provider module.
 *
 * <p>Types in this package are encapsulated within the JPMS module and opened
 * selectively to {@code org.hibernate.orm.core} for reflective instantiation
 * (such as {@link hibernate.jakarta.persistence.tools.internal.NoOpConnectionProvider}).
 * They are not part of the public API and must not be referenced directly by
 * external consumers.</p>
 *
 * @since 2.1.0
 */
package hibernate.jakarta.persistence.tools.internal;
