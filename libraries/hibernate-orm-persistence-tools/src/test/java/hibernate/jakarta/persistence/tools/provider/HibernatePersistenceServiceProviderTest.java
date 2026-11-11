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

package hibernate.jakarta.persistence.tools.provider;

import org.junit.jupiter.api.Test;
import persistence.tools.spi.PersistenceServiceProvider;

import static org.assertj.core.api.Assertions.assertThat;

class HibernatePersistenceServiceProviderTest {

    @Test
    void getSupportedNamespace_returnsJakarta() {
        var provider = new HibernateNamedObjectParserFactory();

        assertThat(provider.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void jakartaNamedObjectParserFactory_isPersistenceServiceProvider() {
        var provider = new HibernateNamedObjectParserFactory();

        assertThat(provider).isInstanceOf(PersistenceServiceProvider.class);
    }

    @Test
    void jakartaNamedObjectParserFactory_isJakartaPersistenceServiceProvider() {
        var provider = new HibernateNamedObjectParserFactory();

        assertThat(provider).isInstanceOf(HibernatePersistenceServiceProvider.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_isPersistenceServiceProvider() {
        var provider = new HibernateSchemaValidatorFactory();

        assertThat(provider).isInstanceOf(PersistenceServiceProvider.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_isJakartaPersistenceServiceProvider() {
        var provider = new HibernateSchemaValidatorFactory();

        assertThat(provider).isInstanceOf(HibernatePersistenceServiceProvider.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_getSupportedNamespace_returnsJakarta() {
        var provider = new HibernateSchemaValidatorFactory();

        assertThat(provider.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void jakartaPersistenceLinterFactory_extendsJakartaPersistenceServiceProvider() {
        var factory = new HibernateNamedObjectParserFactory();

        assertThat(factory).isInstanceOf(HibernatePersistenceLinterFactory.class);
        assertThat(HibernatePersistenceLinterFactory.class.getSuperclass()).isEqualTo(HibernatePersistenceServiceProvider.class);
    }

}
