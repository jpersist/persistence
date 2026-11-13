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

package persist.jakarta.gradle.util

import persistence.tools.spi.PersistenceServiceProvider

/**
 * Utility class for resolving {@link PersistenceServiceProvider} implementations via SPI.
 * <p>
 * Checks whether the {@code io.github.jpersist.tools.api} JPMS module is present in the boot
 * layer (indicating a modular runtime execution) and resolves the provider using the
 * {@link ModuleLayer}. Otherwise, resolves the provider from the current thread's context classloader.
 * </p>
 *
 * @since 1.9.0
 */
class PersistenceServiceResolver {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private PersistenceServiceResolver() {}

    /**
     * Resolves a versioned persistence service provider instance for the specified JPA version and service type.
     *
     * @param <T>           The specific {@link PersistenceServiceProvider} interface type.
     * @param jpaVersion    The JPA specification version string (e.g. {@code "3.0"}).
     * @param providerClass The class object of the service provider interface to resolve.
     * @return The resolved provider instance.
     */
    static <T extends PersistenceServiceProvider> T resolvePersistenceServiceProvider(String jpaVersion, Class<T> providerClass) {
        ModuleLayer bootLayer = ModuleLayer.boot()
        if (bootLayer.findModule("io.github.jpersist.tools.api").isPresent()) {
            return PersistenceServiceProvider.getServiceProvider(jpaVersion, providerClass, bootLayer)
        }
        return PersistenceServiceProvider.getServiceProvider(jpaVersion, providerClass, Thread.currentThread().getContextClassLoader())
    }

}
