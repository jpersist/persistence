package persist.lint.legacy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LegacySchemaValidatorTest {

    private static final List<String> ENTITY_CLASS_NAMES = List.of(
            "persist.lint.legacy.model.Product",
            "persist.lint.legacy.model.Supplier",
            "persist.lint.legacy.model.Warehouse",
            "persist.lint.legacy.model.StockMovement"
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
        var validator = LegacySchemaValidator.of(ENTITY_CLASS_NAMES);

        var url = "jdbc:h2:mem:legacy_complete;DB_CLOSE_DELAY=-1;"
                + "INIT=RUNSCRIPT FROM 'classpath:schema/complete-schema.sql'";

        assertThatCode(() -> validator.validateSchema(connectionProperties(url)))
                .doesNotThrowAnyException();
    }

    @Test
    void validateSchema_withMissingTable_shouldThrowException() {
        var validator = LegacySchemaValidator.of(ENTITY_CLASS_NAMES);

        var url = "jdbc:h2:mem:legacy_missing_table;DB_CLOSE_DELAY=-1;"
                + "INIT=RUNSCRIPT FROM 'classpath:schema/missing-table-schema.sql'";

        assertThatThrownBy(() -> validator.validateSchema(connectionProperties(url)))
                .hasMessageContaining("Supplier");
    }

    @Test
    void validateSchema_withMissingColumn_shouldThrowException() {
        var validator = LegacySchemaValidator.of(ENTITY_CLASS_NAMES);

        var url = "jdbc:h2:mem:legacy_missing_column;DB_CLOSE_DELAY=-1;"
                + "INIT=RUNSCRIPT FROM 'classpath:schema/missing-column-schema.sql'";

        assertThatThrownBy(() -> validator.validateSchema(connectionProperties(url)))
                .hasMessageContaining("code");
    }

}
