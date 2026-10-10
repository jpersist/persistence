package persist.lint.query.legacy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LegacyQueryParserTest {

    private LegacyQueryParser parser;

    @BeforeEach
    void init() {
        parser = LegacyQueryParser.from(List.of(TestEntity.class.getName()));
    }

    @Test
    void from_withValidEntityClasses_returnsNonNullParser() {
        assertThat(parser).isNotNull();
    }

    @Test
    void parseQuery_withValidSelectQuery_succeeds() {
        assertThatCode(() ->
            parser.parseQuery(
                "TestEntity.findByName",
                "SELECT e FROM TestEntity e WHERE e.name = :name"))
            .doesNotThrowAnyException();
    }

    @Test
    void parseQuery_withValidSelectAllQuery_succeeds() {
        assertThatCode(() -> parser.parseQuery("TestEntity.findAll", "SELECT e FROM TestEntity e"))
            .doesNotThrowAnyException();
    }

    @Test
    void parseQuery_withInvalidProperty_throwsException() {
        assertThatThrownBy(() ->
            parser.parseQuery(
                "TestEntity.findByInvalid",
                "SELECT e FROM TestEntity e WHERE e.nonExistentField = :value"))
            .isInstanceOf(Exception.class);
    }

    @Test
    void parseQuery_withInvalidSyntax_throwsException() {
        assertThatThrownBy(() -> parser.parseQuery("TestEntity.badSyntax", "SELECTE e FROM TestEntity e"))
            .isInstanceOf(Exception.class);
    }

    @Test
    void parseQuery_withInvalidEntityName_throwsException() {
        assertThatThrownBy(() -> parser.parseQuery("Unknown.findAll", "SELECT e FROM NonExistentEntity e"))
            .isInstanceOf(Exception.class);
    }

    @Test
    void buildMetadata_withValidClasses_returnsNonNullMetadata() {
        var metadata = LegacyQueryParser.buildMetadata(List.of(TestEntity.class.getName()));

        assertThat(metadata).isNotNull();
    }

    @Test
    void buildMetadata_withEmptyClassList_returnsMetadata() {
        var metadata = LegacyQueryParser.buildMetadata(List.of());

        assertThat(metadata).isNotNull();
    }

}
