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

import hibernate.jakarta.persistence.tools.HibernateSchemaValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import hibernate.jakarta.persistence.tools.TestEntity;
import persistence.tools.spi.SchemaValidatorFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HibernateSchemaValidatorFactoryTest {

    @Test
    void getSupportedNamespace_returnsJakarta() {
        var factory = new HibernateSchemaValidatorFactory();

        assertThat(factory.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void newSchemaValidator_returnsNonNullInstance() {
        var factory = new HibernateSchemaValidatorFactory();

        var validator = factory.newSchemaValidator(List.of(TestEntity.class.getName()));

        assertThat(validator).isNotNull().isInstanceOf(HibernateSchemaValidator.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void newSchemaValidatorFactory_withLegacyVersions_throwsIllegalStateException(String version) {
        assertThatThrownBy(() -> SchemaValidatorFactory.newSchemaValidatorFactory(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void newSchemaValidatorFactory_withJakartaVersions_returnsNonNullInstance(String version) {
        var factory = SchemaValidatorFactory.newSchemaValidatorFactory(version);

        assertThat(factory).isInstanceOf(HibernateSchemaValidatorFactory.class);
    }

}
