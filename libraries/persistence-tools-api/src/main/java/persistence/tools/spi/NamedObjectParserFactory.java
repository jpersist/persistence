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

import persistence.tools.NamedObjectParser;

import java.util.List;
import java.util.ServiceLoader;

/**
 * Factory for creating {@link NamedObjectParser} instances.
 *
 * <p>Implementations are discovered at runtime through the
 * {@link ServiceLoader} mechanism. Each implementation declares the
 * persistence namespace it supports (e.g.&nbsp;{@code "javax"} or
 * {@code "jakarta"}) so that the correct parser strategy can be
 * resolved for the target JPA specification version.</p>
 *
 * <p>In JPMS environments, provider modules register their implementation
 * with {@code provides persistence.tools.spi.NamedObjectParserFactory} in
 * {@code module-info.java}.</p>
 *
 * @since 2.0.0
 * @see NamedObjectParser
 */
public interface NamedObjectParserFactory extends PersistenceLinterFactory {

    /**
     * Creates a new {@link NamedObjectParser} configured with the given entity
     * class names.
     *
     * @param discoveredClassNames fully qualified class names of the JPA
     *                             entities to register in the metamodel
     * @return a ready-to-use named object parser; the caller is responsible
     *         for closing it (preferably via try-with-resources)
     */
    NamedObjectParser newNamedObjectParser(List<String> discoveredClassNames);

    /**
     * Discovers and returns the {@link NamedObjectParserFactory} implementation
     * that matches the given JPA specification version using the thread
     * context class loader.
     *
     * <p>The version string is mapped to a persistence namespace
     * ({@code "javax"} for versions 2.0–2.2, {@code "jakarta"} for
     * versions 3.0–3.2) and the matching factory is located via the
     * {@link ServiceLoader} API.</p>
     *
     * @param jpaVersion the JPA specification version (e.g.&nbsp;{@code "2.2"}
     *                   or {@code "3.1"})
     * @return the factory implementation for the requested version
     * @throws IllegalArgumentException if the version is not recognized
     * @throws IllegalStateException    if no provider is registered for
     *                                  the resolved namespace
     */
    static NamedObjectParserFactory newNamedObjectParserFactory(String jpaVersion) {
        return PersistenceLinterFactory.newPersistenceLinterFactory(jpaVersion, NamedObjectParserFactory.class);
    }

    /**
     * Discovers and returns the {@link NamedObjectParserFactory} implementation
     * that matches the given JPA specification version using the specified
     * {@link ClassLoader}.
     *
     * @param jpaVersion  the JPA specification version (e.g.&nbsp;{@code "2.2"}
     *                    or {@code "3.1"})
     * @param classLoader the class loader used to discover the factory
     * @return the factory implementation for the requested version
     * @throws IllegalArgumentException if the version is not recognized
     * @throws IllegalStateException    if no provider is registered for
     *                                  the resolved namespace
     * @since 3.1.0
     */
    static NamedObjectParserFactory newNamedObjectParserFactory(String jpaVersion, ClassLoader classLoader) {
        return PersistenceLinterFactory.newPersistenceLinterFactory(jpaVersion, NamedObjectParserFactory.class, classLoader);
    }

    /**
     * Discovers and returns the {@link NamedObjectParserFactory} implementation
     * that matches the given JPA specification version within the specified
     * {@link ModuleLayer}.
     *
     * @param jpaVersion the JPA specification version (e.g.&nbsp;{@code "2.2"}
     *                   or {@code "3.1"})
     * @param layer      the module layer used to discover the factory
     * @return the factory implementation for the requested version
     * @throws IllegalArgumentException if the version is not recognized
     * @throws IllegalStateException    if no provider is registered for
     *                                  the resolved namespace
     * @since 3.1.0
     */
    static NamedObjectParserFactory newNamedObjectParserFactory(String jpaVersion, ModuleLayer layer) {
        return PersistenceLinterFactory.newPersistenceLinterFactory(jpaVersion, NamedObjectParserFactory.class, layer);
    }

}
