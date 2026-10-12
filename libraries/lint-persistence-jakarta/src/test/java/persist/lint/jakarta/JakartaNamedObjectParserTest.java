package persist.lint.jakarta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persist.lint.query.jakarta.JakartaQueryParser;
import persist.lint.query.jakarta.TestEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JakartaNamedObjectParserTest {

    private JakartaNamedObjectParser parser;

    @BeforeEach
    void init() {
        parser = JakartaNamedObjectParser.from(List.of(TestEntity.class.getName()));
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

}
