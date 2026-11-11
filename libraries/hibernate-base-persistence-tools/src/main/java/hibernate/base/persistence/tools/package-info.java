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
 * Shared persistence tooling infrastructure, configuration options, and utilities for Hibernate.
 *
 * <p>This package provides common abstractions and utility components utilized across Hibernate
 * provider implementations (such as Hibernate&nbsp;5 and Hibernate ORM&nbsp;6):</p>
 *
 * <ul>
 *   <li>{@link hibernate.base.persistence.tools.HibernateEnhancerOption} &ndash; Enumeration of
 *       bytecode enhancement configuration options and their associated property keys.</li>
 *   <li>{@link hibernate.base.persistence.tools.HibernateUtil} &ndash; Shared utility methods for
 *       class loader creation, file-to-URL conversion, and enhancer setting mapping.</li>
 * </ul>
 *
 * @since 1.0.0
 * @see persistence.tools
 */
package hibernate.base.persistence.tools;
