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

import hibernate.javax.persistence.tools.HibernateEnhancer;
import persistence.tools.Enhancer;
import persistence.tools.spi.EnhancerProvider;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * {@link EnhancerProvider} implementation for the legacy
 * {@code javax.persistence} namespace ({@code "javax"}).
 *
 * <p>This provider is registered as a {@link java.util.ServiceLoader}
 * provider and delegates to {@link HibernateEnhancer} for
 * bytecode enhancement using Hibernate&nbsp;5.</p>
 *
 * @since 1.0.0
 * @see HibernateEnhancer
 * @see EnhancerProvider
 */
public class HibernateEnhancerProvider extends HibernatePersistenceServiceProvider implements EnhancerProvider {

    /** Creates a new {@code HibernateEnhancerProvider}. */
    public HibernateEnhancerProvider() {
    }

    /** {@inheritDoc} */
    @Override
    public Enhancer getEnhancer(Collection<Set<File>> classpathFilesCollection,
                                Map<String, Object> settings) {
        return HibernateEnhancer.from(classpathFilesCollection, settings);
    }

}
