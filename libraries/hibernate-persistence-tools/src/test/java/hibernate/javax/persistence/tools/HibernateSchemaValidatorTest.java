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

package hibernate.javax.persistence.tools;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HibernateSchemaValidatorTest {

    private static final List<String> ENTITY_CLASS_NAMES = List.of(
            "hibernate.javax.persistence.tools.model.Product",
            "hibernate.javax.persistence.tools.model.Supplier",
            "hibernate.javax.persistence.tools.model.Warehouse",
            "hibernate.javax.persistence.tools.model.StockMovement"
    );

    private static Map<String, String> connectionProperties(String url) {
        return Map.of(
                "driver", "org.h2.Driver",
                "url", url,
                "username", "sa",
                "password", ""
        );
    }

    @Test
    void validateSchema_withCompleteSchema_shouldSucceed() {
        var validator = HibernateSchemaValidator.of(ENTITY_CLASS_NAMES);

        var url = "jdbc:h2:mem:legacy_complete;DB_CLOSE_DELAY=-1;"
                + "INIT=RUNSCRIPT FROM 'classpath:schema/complete-schema.sql'";

        assertThatCode(() -> validator.validateSchema(connectionProperties(url)))
                .doesNotThrowAnyException();
    }

    @Test
    void validateSchema_withMissingTable_shouldThrowException() {
        var validator = HibernateSchemaValidator.of(ENTITY_CLASS_NAMES);

        var url = "jdbc:h2:mem:legacy_missing_table;DB_CLOSE_DELAY=-1;"
                + "INIT=RUNSCRIPT FROM 'classpath:schema/missing-table-schema.sql'";

        assertThatThrownBy(() -> validator.validateSchema(connectionProperties(url)))
                .hasMessageContaining("Supplier");
    }

    @Test
    void validateSchema_withMissingColumn_shouldThrowException() {
        var validator = HibernateSchemaValidator.of(ENTITY_CLASS_NAMES);

        var url = "jdbc:h2:mem:legacy_missing_column;DB_CLOSE_DELAY=-1;"
                + "INIT=RUNSCRIPT FROM 'classpath:schema/missing-column-schema.sql'";

        assertThatThrownBy(() -> validator.validateSchema(connectionProperties(url)))
                .hasMessageContaining("code");
    }

}
