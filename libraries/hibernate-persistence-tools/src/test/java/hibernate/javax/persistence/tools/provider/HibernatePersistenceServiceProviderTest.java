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

import org.junit.jupiter.api.Test;
import persistence.tools.spi.PersistenceServiceProvider;

import static org.assertj.core.api.Assertions.assertThat;

class HibernatePersistenceServiceProviderTest {

    @Test
    void getSupportedNamespace_returnsJavax() {
        var provider = new HibernateNamedObjectParserFactory();

        assertThat(provider.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void legacyNamedObjectParserFactory_isPersistenceServiceProvider() {
        var provider = new HibernateNamedObjectParserFactory();

        assertThat(provider).isInstanceOf(PersistenceServiceProvider.class);
    }

    @Test
    void legacySchemaValidatorFactory_isPersistenceServiceProvider() {
        var provider = new HibernateSchemaValidatorFactory();

        assertThat(provider).isInstanceOf(PersistenceServiceProvider.class);
    }

    @Test
    void legacySchemaValidatorFactory_getSupportedNamespace_returnsJavax() {
        var provider = new HibernateSchemaValidatorFactory();

        assertThat(provider.getSupportedNamespace()).isEqualTo("javax");
    }

    @Test
    void legacyPersistenceLinterFactory_implementsPersistenceServiceProvider() {
        assertThat(PersistenceServiceProvider.class).isAssignableFrom(HibernatePersistenceLinterFactory.class);
    }

}
