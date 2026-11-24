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

import hibernate.javax.persistence.tools.HibernateEntityModelLinter;
import persistence.tools.EntityModelLinter;
import persistence.tools.spi.EntityModelLinterFactory;

import java.util.List;
import java.util.Map;

/**
 * Hibernate&nbsp;5 provider factory for {@link HibernateEntityModelLinter}.
 *
 * <p>This factory is registered as a {@link java.util.ServiceLoader} provider and creates
 * {@link HibernateEntityModelLinter} instances targeting the {@code "javax"} persistence namespace.</p>
 *
 * @since 1.2.0
 * @see HibernateEntityModelLinter
 * @see EntityModelLinterFactory
 * @see HibernatePersistenceLinterFactory
 */
public class HibernateEntityModelLinterFactory extends HibernatePersistenceLinterFactory implements EntityModelLinterFactory {

    /**
     * Creates a new {@code HibernateEntityModelLinterFactory}.
     */
    public HibernateEntityModelLinterFactory() {
        // Default no args constructor
    }

    /** {@inheritDoc} */
    @Override
    public EntityModelLinter newEntityModelLinter(List<String> entityClassNames, Map<String, String> settings) {
        return HibernateEntityModelLinter.of(entityClassNames, Thread.currentThread().getContextClassLoader());
    }

}
