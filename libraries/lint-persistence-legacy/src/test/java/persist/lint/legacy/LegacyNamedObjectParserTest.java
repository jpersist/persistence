package persist.lint.legacy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persist.lint.query.legacy.TestEntity;
import persist.lint.api.NamedObjectDescriptor;
import persist.lint.api.NamedObjectType;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class LegacyNamedObjectParserTest {

    private LegacyNamedObjectParser parser;

    @BeforeEach
    void init() {
        parser = LegacyNamedObjectParser.from(List.of(TestEntity.class.getName()));
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
    void parseQuery_withSinglePositionalParameter_returnsDescriptor() {
        NamedObjectDescriptor descriptor = parser.parseQuery(
            "TestEntity.findByNamePositional",
            "SELECT e FROM TestEntity e WHERE e.name = ?1");

        assertThat(descriptor).isNotNull();
        assertThat(descriptor.type()).isEqualTo(NamedObjectType.QUERY);
        assertThat(descriptor.name()).isEqualTo("TestEntity.findByNamePositional");
        assertThat(descriptor.value()).isEqualTo("SELECT e FROM TestEntity e WHERE e.name = ?1");
        assertThat(descriptor.namedParameters()).isEmpty();
        assertThat(descriptor.positionalParameters()).containsExactly(1);
    }

    @Test
    void parseQuery_withMultiplePositionalParameters_returnsDescriptor() {
        NamedObjectDescriptor descriptor = parser.parseQuery(
            "TestEntity.findByIdAndName",
            "SELECT e FROM TestEntity e WHERE e.id = ?1 AND e.name = ?2");

        assertThat(descriptor).isNotNull();
        assertThat(descriptor.type()).isEqualTo(NamedObjectType.QUERY);
        assertThat(descriptor.name()).isEqualTo("TestEntity.findByIdAndName");
        assertThat(descriptor.value()).isEqualTo("SELECT e FROM TestEntity e WHERE e.id = ?1 AND e.name = ?2");
        assertThat(descriptor.namedParameters()).isEmpty();
        assertThat(descriptor.positionalParameters()).containsExactly(1, 2);
    }

    @Test
    void parseStoredProcedure_withParameters_returnsDescriptor() {
        NamedObjectDescriptor descriptor = parser.parseStoredProcedure(
            "Order.archiveOldOrders", "sp_archive_orders", Set.of("cutoffDate", "archiveTable"));

        assertThat(descriptor).isNotNull();
        assertThat(descriptor.type()).isEqualTo(NamedObjectType.STORED_PROCEDURE);
        assertThat(descriptor.name()).isEqualTo("Order.archiveOldOrders");
        assertThat(descriptor.value()).isEqualTo("sp_archive_orders");
        assertThat(descriptor.namedParameters()).containsExactlyInAnyOrder("cutoffDate", "archiveTable");
        assertThat(descriptor.positionalParameters()).isEmpty();
    }

    @Test
    void parseStoredProcedure_withNoParameters_returnsDescriptor() {
        NamedObjectDescriptor descriptor = parser.parseStoredProcedure(
            "Cleanup.purgeExpired", "sp_purge_expired", Set.of());

        assertThat(descriptor).isNotNull();
        assertThat(descriptor.type()).isEqualTo(NamedObjectType.STORED_PROCEDURE);
        assertThat(descriptor.name()).isEqualTo("Cleanup.purgeExpired");
        assertThat(descriptor.value()).isEqualTo("sp_purge_expired");
        assertThat(descriptor.namedParameters()).isEmpty();
        assertThat(descriptor.positionalParameters()).isEmpty();
    }

    @Test
    void parseStoredProcedure_withSingleParameter_returnsDescriptor() {
        NamedObjectDescriptor descriptor = parser.parseStoredProcedure(
            "Report.generateMonthly", "sp_generate_report", Set.of("month"));

        assertThat(descriptor).isNotNull();
        assertThat(descriptor.type()).isEqualTo(NamedObjectType.STORED_PROCEDURE);
        assertThat(descriptor.name()).isEqualTo("Report.generateMonthly");
        assertThat(descriptor.value()).isEqualTo("sp_generate_report");
        assertThat(descriptor.namedParameters()).containsExactly("month");
    }

}
