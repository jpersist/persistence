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

package persistence.tools.spi;

import persistence.tools.EntityModelLinter;

import java.util.List;
import java.util.Map;

/**
 * Service Provider Interface (SPI) factory for creating {@link EntityModelLinter}
 * instances bound to a specific JPA specification namespace.
 *
 * <p>Concrete provider implementations are discovered at runtime via {@link java.util.ServiceLoader}
 * using the static {@code newEntityModelLinterFactory} lookup methods.</p>
 *
 * @since 4.2.0
 * @see EntityModelLinter
 * @see PersistenceLinterFactory
 */
public interface EntityModelLinterFactory extends PersistenceLinterFactory {

    /**
     * Creates a new {@link EntityModelLinter} configured with the provider's default settings.
     *
     * @param entityClassNames the fully qualified names of the compiled JPA entity classes to inspect
     * @return a configured {@link EntityModelLinter} instance
     */
    default EntityModelLinter newEntityModelLinter(List<String> entityClassNames) {
        return newEntityModelLinter(entityClassNames, Map.of());
    }

    /**
     * Creates a new {@link EntityModelLinter} configured with custom provider settings.
     *
     * @param entityClassNames the fully qualified names of the compiled JPA entity classes to inspect
     * @param settings         provider-specific configuration properties merged over default settings
     * @return a configured {@link EntityModelLinter} instance
     */
    EntityModelLinter newEntityModelLinter(List<String> entityClassNames, Map<String, String> settings);

    /**
     * Discovers and returns an {@link EntityModelLinterFactory} supporting the given JPA specification version
     * using the current thread's context class loader.
     *
     * @param jpaVersion the target JPA version string (e.g., {@code "2.2"}, {@code "3.1"})
     * @return the matching {@link EntityModelLinterFactory} provider
     * @throws IllegalArgumentException if the JPA version is unsupported or no matching provider is found
     */
    static EntityModelLinterFactory newEntityModelLinterFactory(String jpaVersion) {
        return PersistenceLinterFactory.newPersistenceLinterFactory(jpaVersion, EntityModelLinterFactory.class);
    }

    /**
     * Discovers and returns an {@link EntityModelLinterFactory} supporting the given JPA specification version
     * using the specified {@link ClassLoader}.
     *
     * @param jpaVersion  the target JPA version string (e.g., {@code "2.2"}, {@code "3.1"})
     * @param classLoader the class loader used to locate provider implementations
     * @return the matching {@link EntityModelLinterFactory} provider
     * @throws IllegalArgumentException if the JPA version is unsupported or no matching provider is found
     */
    static EntityModelLinterFactory newEntityModelLinterFactory(String jpaVersion, ClassLoader classLoader) {
        return PersistenceLinterFactory.newPersistenceLinterFactory(jpaVersion, EntityModelLinterFactory.class, classLoader);
    }

    /**
     * Discovers and returns an {@link EntityModelLinterFactory} supporting the given JPA specification version
     * within the specified JPMS {@link ModuleLayer}.
     *
     * @param jpaVersion  the target JPA version string (e.g., {@code "2.2"}, {@code "3.1"})
     * @param moduleLayer the module layer used to locate provider implementations
     * @return the matching {@link EntityModelLinterFactory} provider
     * @throws IllegalArgumentException if the JPA version is unsupported or no matching provider is found
     */
    static EntityModelLinterFactory newEntityModelLinterFactory(String jpaVersion, ModuleLayer moduleLayer) {
        return PersistenceLinterFactory.newPersistenceLinterFactory(jpaVersion, EntityModelLinterFactory.class, moduleLayer);
    }

}
