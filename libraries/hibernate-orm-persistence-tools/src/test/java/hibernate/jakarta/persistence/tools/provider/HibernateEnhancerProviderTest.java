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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import persistence.tools.spi.EnhancerProvider;
import persistence.tools.spi.PersistenceServiceProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HibernateEnhancerProviderTest {

    @Test
    void getSupportedNamespace_returnsJakarta() {
        var provider = new HibernateEnhancerProvider();

        assertThat(provider.getSupportedNamespace()).isEqualTo("jakarta");
    }

    @Test
    void jakartaHibernateEnhancerProvider_isPersistenceServiceProvider() {
        var provider = new HibernateEnhancerProvider();

        assertThat(provider).isInstanceOf(PersistenceServiceProvider.class);
    }

    @Test
    void jakartaHibernateEnhancerProvider_isHibernateEnhancerProvider() {
        var provider = new HibernateEnhancerProvider();

        assertThat(provider).isInstanceOf(EnhancerProvider.class);
    }

    @Test
    void jakartaHibernateEnhancerProvider_isJakartaPersistenceServiceProvider() {
        var provider = new HibernateEnhancerProvider();

        assertThat(provider).isInstanceOf(HibernatePersistenceServiceProvider.class);
    }

    @Test
    void jakartaHibernateEnhancerProvider_extendsJakartaPersistenceServiceProvider() {
        assertThat(HibernateEnhancerProvider.class.getSuperclass())
            .isEqualTo(HibernatePersistenceServiceProvider.class);
    }

    @Test
    void jakartaHibernateEnhancerProvider_implementsHibernateEnhancerProvider() {
        assertThat(EnhancerProvider.class)
            .isAssignableFrom(HibernateEnhancerProvider.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.0", "2.1", "2.2"})
    void getHibernateEnhancerProvider_withLegacyVersions_throwsIllegalStateException(String version) {
        assertThatThrownBy(() -> EnhancerProvider.getEnhancerProvider(version))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("javax");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0", "3.1", "3.2"})
    void getHibernateEnhancerProvider_withJakartaVersions_returnsNonNullInstance(String version) {
        var provider = EnhancerProvider.getEnhancerProvider(version);

        assertThat(provider).isInstanceOf(HibernateEnhancerProvider.class);
    }

}
