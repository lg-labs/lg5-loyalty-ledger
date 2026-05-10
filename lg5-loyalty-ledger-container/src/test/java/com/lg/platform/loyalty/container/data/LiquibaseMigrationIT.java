package com.lg.platform.loyalty.container.data;

import com.lg.platform.loyalty.container.boot.Bootstrap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Liquibase changelog at
 * {@code db/changelog/db.changelog-master.yaml} (TASK-004) materializes the
 * data model in {@code data-model.md §232-358}: the {@code loyalty} schema,
 * its 4 tables (with expected nullability), 3 Postgres ENUMs (with the
 * expected symbol lists), and 4 indexes (incl. the unique dedup index that
 * powers ADR-003).
 *
 * <p>Deviation note (TASK-004): the framework wires Liquibase (via
 * {@code lg5-spring-data-jpa}); the spec calls it "Flyway DDL" but the
 * canonical lg5 convention — confirmed against
 * {@code food-ordering-system/restaurant-service} and {@code blank-service} —
 * is Liquibase YAML changelogs. We use Liquibase to honor RULE-018
 * (use the framework's wiring) and document the deviation in the
 * commit body.
 */
@TestPropertySource(properties = {
        "testcontainers.postgres.enabled=true",
        // override application.yaml's localhost URL so Spring uses the
        // testcontainer's @ServiceConnection-injected URL
        "spring.datasource.url=",
        "spring.datasource.username=",
        "spring.datasource.password="
})
class LiquibaseMigrationIT extends Bootstrap {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void it_should_create_the_loyalty_schema() {
        final List<String> schemas = jdbcTemplate.queryForList(
                "SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'loyalty'",
                String.class);
        assertThat(schemas).containsExactly("loyalty");
    }

    @Test
    void it_should_create_all_four_tables_in_loyalty_schema() {
        final Set<String> tables = jdbcTemplate.queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'loyalty'",
                        String.class)
                .stream().collect(Collectors.toSet());
        assertThat(tables).containsExactlyInAnyOrder(
                "movement", "customer_balance", "processed_input_event", "outbox");
    }

    @Test
    void it_should_create_loyalty_cause_enum_with_expected_symbols() {
        assertEnumSymbols("loyalty_cause",
                Set.of("ORDER_PAID", "ORDER_CANCELLED", "ORDER_REFUNDED"));
    }

    @Test
    void it_should_create_processed_input_outcome_enum_with_expected_symbols() {
        assertEnumSymbols("processed_input_outcome",
                Set.of("MOVEMENT_APPENDED", "NOOP_ZERO_CREDIT", "NOOP_DEBIT_WITHOUT_CREDIT"));
    }

    @Test
    void it_should_create_outbox_status_enum_with_expected_symbols() {
        assertEnumSymbols("outbox_status",
                Set.of("STARTED", "COMPLETED", "FAILED"));
    }

    @Test
    void it_should_create_movement_table_with_expected_columns_and_nullability() {
        final Map<String, String> nullability = nullabilityFor("movement");
        assertThat(nullability).containsAllEntriesOf(Map.ofEntries(
                Map.entry("id", "NO"),
                Map.entry("customer_id", "NO"),
                Map.entry("delta", "NO"),
                Map.entry("cause", "NO"),
                Map.entry("originating_order_id", "NO"),
                Map.entry("originating_event_id", "NO"),
                Map.entry("originating_event_type", "NO"),
                Map.entry("originating_event_received_at", "NO"),
                Map.entry("appended_at", "NO"),
                Map.entry("version", "NO")));
    }

    @Test
    void it_should_create_customer_balance_table_with_expected_columns_and_nullability() {
        final Map<String, String> nullability = nullabilityFor("customer_balance");
        assertThat(nullability).containsAllEntriesOf(Map.of(
                "customer_id", "NO",
                "balance", "NO",
                "last_updated_at", "NO",
                "version", "NO"));
    }

    @Test
    void it_should_create_processed_input_event_table_with_movement_id_nullable() {
        final Map<String, String> nullability = nullabilityFor("processed_input_event");
        assertThat(nullability).containsAllEntriesOf(Map.ofEntries(
                Map.entry("id", "NO"),
                Map.entry("originating_event_id", "NO"),
                Map.entry("originating_event_type", "NO"),
                Map.entry("originating_order_id", "NO"),
                Map.entry("originating_customer_id", "NO"),
                Map.entry("received_at", "NO"),
                Map.entry("outcome", "NO"),
                Map.entry("movement_id", "YES"),   // nullable per data-model §279
                Map.entry("version", "NO")));
    }

    @Test
    void it_should_create_outbox_table_with_expected_columns_and_nullability() {
        final Map<String, String> nullability = nullabilityFor("outbox");
        assertThat(nullability).containsAllEntriesOf(Map.ofEntries(
                Map.entry("id", "NO"),
                Map.entry("saga_id", "NO"),
                Map.entry("type", "NO"),
                Map.entry("payload", "NO"),
                Map.entry("outbox_status", "NO"),
                Map.entry("created_at", "NO"),
                Map.entry("version", "NO")));
    }

    @Test
    void it_should_create_all_four_indexes_in_loyalty_schema() {
        final Set<String> indexes = jdbcTemplate.queryForList(
                        "SELECT indexname FROM pg_indexes WHERE schemaname = 'loyalty'",
                        String.class)
                .stream().collect(Collectors.toSet());
        // PK indexes are auto-created with name `<table>_pkey` plus our 4 explicit ones.
        assertThat(indexes).contains(
                "idx_movement_customer_appended",
                "idx_movement_originating_order",
                "uq_processed_event_type_id",
                "idx_outbox_status_created");
    }

    @Test
    void it_should_enforce_unique_constraint_on_processed_event_type_and_id() {
        final String sql = "SELECT indexdef FROM pg_indexes "
                + "WHERE schemaname = 'loyalty' AND indexname = 'uq_processed_event_type_id'";
        final String indexdef = jdbcTemplate.queryForObject(sql, String.class);
        assertThat(indexdef)
                .as("uq_processed_event_type_id must be UNIQUE — this index implements ADR-003 dedup")
                .containsIgnoringCase("UNIQUE");
    }

    private void assertEnumSymbols(final String enumTypeName, final Set<String> expectedSymbols) {
        final Set<String> actual = jdbcTemplate.queryForList(
                        "SELECT e.enumlabel "
                                + "FROM pg_type t "
                                + "JOIN pg_enum e ON t.oid = e.enumtypid "
                                + "WHERE t.typname = ?",
                        String.class, enumTypeName)
                .stream().collect(Collectors.toSet());
        assertThat(actual)
                .as("ENUM %s symbols", enumTypeName)
                .containsExactlyInAnyOrderElementsOf(expectedSymbols);
    }

    private Map<String, String> nullabilityFor(final String tableName) {
        final List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT column_name, is_nullable FROM information_schema.columns "
                        + "WHERE table_schema = 'loyalty' AND table_name = ?",
                tableName);
        return rows.stream().collect(Collectors.toMap(
                r -> (String) r.get("column_name"),
                r -> (String) r.get("is_nullable")));
    }
}
