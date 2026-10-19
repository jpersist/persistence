package persist.lint.jakarta;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JakartaSchemaValidatorTest {

    private static final List<String> ENTITY_CLASS_NAMES = List.of(
            "persist.lint.jakarta.model.Product",
            "persist.lint.jakarta.model.Supplier",
            "persist.lint.jakarta.model.Warehouse",
            "persist.lint.jakarta.model.StockMovement"
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
        var validator = JakartaSchemaValidator.of(ENTITY_CLASS_NAMES);

        var url = "jdbc:h2:mem:jakarta_complete;DB_CLOSE_DELAY=-1;"
                + "INIT=RUNSCRIPT FROM 'classpath:schema/complete-schema.sql'";

        assertThatCode(() -> validator.validateSchema(connectionProperties(url)))
                .doesNotThrowAnyException();
    }

    @Test
    void validateSchema_withMissingTable_shouldThrowException() {
        var validator = JakartaSchemaValidator.of(ENTITY_CLASS_NAMES);

        var url = "jdbc:h2:mem:jakarta_missing_table;DB_CLOSE_DELAY=-1;"
                + "INIT=RUNSCRIPT FROM 'classpath:schema/missing-table-schema.sql'";

        assertThatThrownBy(() -> validator.validateSchema(connectionProperties(url)))
                .hasMessageContaining("Supplier");
    }

    @Test
    void validateSchema_withMissingColumn_shouldThrowException() {
        var validator = JakartaSchemaValidator.of(ENTITY_CLASS_NAMES);

        var url = "jdbc:h2:mem:jakarta_missing_column;DB_CLOSE_DELAY=-1;"
                + "INIT=RUNSCRIPT FROM 'classpath:schema/missing-column-schema.sql'";

        assertThatThrownBy(() -> validator.validateSchema(connectionProperties(url)))
                .hasMessageContaining("code");
    }

}
