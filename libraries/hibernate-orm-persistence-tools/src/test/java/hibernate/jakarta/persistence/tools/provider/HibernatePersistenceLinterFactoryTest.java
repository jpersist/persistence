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
import persistence.tools.spi.PersistenceLinterFactory;

import static org.assertj.core.api.Assertions.assertThat;

class HibernatePersistenceLinterFactoryTest {

    @Test
    void getSupportedNamespace_returnsJakarta() {
        var factory = new HibernateNamedObjectParserFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void jakartaNamedObjectParserFactory_isPersistenceLinterFactory() {
        var factory = new HibernateNamedObjectParserFactory();

        assertThat(factory).isInstanceOf(PersistenceLinterFactory.class);
    }

    @Test
    void jakartaNamedObjectParserFactory_isJakartaPersistenceLinterFactory() {
        var factory = new HibernateNamedObjectParserFactory();

        assertThat(factory).isInstanceOf(HibernatePersistenceLinterFactory.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_isPersistenceLinterFactory() {
        var factory = new HibernateSchemaValidatorFactory();

        assertThat(factory).isInstanceOf(PersistenceLinterFactory.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_isJakartaPersistenceLinterFactory() {
        var factory = new HibernateSchemaValidatorFactory();

        assertThat(factory).isInstanceOf(HibernatePersistenceLinterFactory.class);
    }

    @Test
    void jakartaSchemaValidatorFactory_getSupportedNamespace_returnsJakarta() {
        var factory = new HibernateSchemaValidatorFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("jakarta");
    }

}
