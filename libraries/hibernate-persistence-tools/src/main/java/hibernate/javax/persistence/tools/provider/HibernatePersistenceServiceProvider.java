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

package hibernate.javax.persistence.tools.provider;

import persistence.tools.spi.PersistenceServiceProvider;

/**
 * Base class for legacy {@code javax.persistence} ({@code "javax"} namespace)
 * service providers.
 *
 * <p>Subclasses inherit a fixed {@link #getSupportedNamespace()}
 * implementation that returns {@code "javax"}, ensuring they are
 * selected when a JPA&nbsp;2.x specification version is requested.</p>
 *
 * @since 1.0.0
 * @see PersistenceServiceProvider
 */
public abstract class HibernatePersistenceServiceProvider implements PersistenceServiceProvider {

    /** Creates a new {@code HibernatePersistenceServiceProvider}. */
    protected HibernatePersistenceServiceProvider() {
    }

    @Override
    public String getSupportedNamespace() {
        return "javax";
    }

}
