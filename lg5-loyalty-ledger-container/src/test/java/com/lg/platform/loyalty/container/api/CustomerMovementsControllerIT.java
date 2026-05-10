package com.lg.platform.loyalty.container.api;

import com.lg.platform.loyalty.container.boot.RestBootstrap;
import com.lg.platform.loyalty.dataaccess.movement.entity.MovementJpaEntity;
import com.lg.platform.loyalty.dataaccess.movement.repository.MovementJpaRepository;
import com.lg.platform.loyalty.domain.valueobject.BalanceUpdateCause;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * REST IT for {@code GET /loyalty/customers/{customerId}/movements}
 * (TASK-016, REQ-010 + REQ-013).
 *
 * <p>Seeds 60 movements for one customer with strictly distinct
 * {@code appended_at} timestamps so the page boundary tests can rely
 * on the {@code DESC} ordering being unambiguous. The
 * {@link MovementJpaRepository} is autowired directly (rather than
 * the {@code MovementLedgerRepository} output port) because
 * {@code Movement.ofCredit/ofDebit} hard-codes {@code appendedAt =
 * ZonedDateTime.now()} which would collide at sub-millisecond
 * resolution under a tight loop. Using the JPA entity straight gives
 * the test full control over the timestamp axis without weakening
 * the production write path.
 *
 * <p>Postgres-only — same shape as
 * {@link CustomerBalanceControllerIT}; the message layer is not
 * involved.
 */
@TestPropertySource(properties = {
        "testcontainers.postgres.enabled=true",
        "spring.datasource.url=",
        "spring.datasource.username=",
        "spring.datasource.password="
})
class CustomerMovementsControllerIT extends RestBootstrap {

    private static final int N = 60;
    private static final int PAGE_SIZE = 20;

    @Autowired private MovementJpaRepository movementJpaRepository;

    @Test
    void getMovements_three_pages_cover_all_60_in_descending_order_no_overlap() {
        final CustomerId customerId = CustomerId.random();
        final List<MovementJpaEntity> seeded = seedMovements(customerId, N);

        // Sanity: the seed succeeded.
        assertThat(movementJpaRepository.countByCustomerId(customerId.getValue())).isEqualTo(N);

        final JsonPath p0 = fetchPage(customerId, 0, PAGE_SIZE);
        final JsonPath p1 = fetchPage(customerId, 1, PAGE_SIZE);
        final JsonPath p2 = fetchPage(customerId, 2, PAGE_SIZE);

        assertThat(p0.getLong("totalElements")).isEqualTo(N);
        assertThat(p1.getLong("totalElements")).isEqualTo(N);
        assertThat(p2.getLong("totalElements")).isEqualTo(N);

        final List<String> idsP0 = p0.getList("movements.id", String.class);
        final List<String> idsP1 = p1.getList("movements.id", String.class);
        final List<String> idsP2 = p2.getList("movements.id", String.class);
        assertThat(idsP0).hasSize(PAGE_SIZE);
        assertThat(idsP1).hasSize(PAGE_SIZE);
        assertThat(idsP2).hasSize(PAGE_SIZE);

        // No overlap across pages.
        final Set<String> all = new HashSet<>();
        all.addAll(idsP0);
        all.addAll(idsP1);
        all.addAll(idsP2);
        assertThat(all).hasSize(N);

        // Each page is descending by appendedAt.
        assertDescendingByAppendedAt(p0);
        assertDescendingByAppendedAt(p1);
        assertDescendingByAppendedAt(p2);

        // The boundary between pages is also descending: last of page N >= first of page N+1.
        assertThat(lastAppendedAt(p0)).isAfterOrEqualTo(firstAppendedAt(p1));
        assertThat(lastAppendedAt(p1)).isAfterOrEqualTo(firstAppendedAt(p2));

        // Page 0's first row is the most recently appended seed row.
        // Postgres `timestamptz` stores microsecond precision, so the
        // value round-tripped through the DB is truncated relative to
        // the in-memory seed (which carries nanos from
        // ZonedDateTime.now()). Compare both sides truncated to micros.
        final ZonedDateTime newestSeed = seeded.stream()
                .map(MovementJpaEntity::getAppendedAt)
                .max(ZonedDateTime::compareTo).orElseThrow()
                .truncatedTo(ChronoUnit.MICROS);
        assertThat(firstAppendedAt(p0).truncatedTo(ChronoUnit.MICROS)).isEqualTo(newestSeed);
    }

    @Test
    void getMovements_out_of_range_page_returns_200_with_empty_movements_and_correct_total() {
        final CustomerId customerId = CustomerId.random();
        seedMovements(customerId, N);

        RestAssured.given(requestSpecification)
                .accept("application/vnd.api.v1+json")
                .when()
                .get("/loyalty/customers/{customerId}/movements?page=999&size=20",
                        customerId.getValue().toString())
                .then()
                .statusCode(200)
                .contentType("application/vnd.api.v1+json")
                .body("page", equalTo(999))
                .body("size", equalTo(20))
                .body("totalElements", equalTo(N))
                .body("movements", hasSize(0));
    }

    @Test
    void getMovements_unknown_customer_returns_200_with_empty_array_and_zero_total() {
        // REQ-010 / REST best-practice: an empty collection is NOT a 404.
        // (TASK-017 will refine 404 only for the GET balance endpoint.)
        final UUID unknown = UUID.randomUUID();
        assertThat(movementJpaRepository.countByCustomerId(unknown)).isZero();

        RestAssured.given(requestSpecification)
                .accept("application/vnd.api.v1+json")
                .when()
                .get("/loyalty/customers/{customerId}/movements", unknown.toString())
                .then()
                .statusCode(200)
                .contentType("application/vnd.api.v1+json")
                .body("totalElements", equalTo(0))
                .body("movements", hasSize(0));
    }

    // ---- helpers ----

    /**
     * Inserts {@code n} movements for {@code customerId} with strictly
     * distinct {@code appended_at} timestamps spaced 1 ms apart, all
     * causes = {@code ORDER_PAID}, all deltas = +1. Returns the
     * inserted entities.
     */
    private List<MovementJpaEntity> seedMovements(final CustomerId customerId, final int n) {
        final ZonedDateTime base = ZonedDateTime.now().minusSeconds(1);
        return java.util.stream.IntStream.range(0, n)
                .mapToObj(i -> MovementJpaEntity.builder()
                        .id(UUID.randomUUID())
                        .customerId(customerId.getValue())
                        .delta(1)
                        .cause(BalanceUpdateCause.ORDER_PAID)
                        .originatingOrderId(UUID.randomUUID())
                        .originatingEventId(UUID.randomUUID())
                        .originatingEventType("OrderPaid")
                        .originatingEventReceivedAt(base.plusNanos(i * 1_000_000L))
                        .appendedAt(base.plusNanos(i * 1_000_000L))
                        .version(0)
                        .build())
                .map(movementJpaRepository::save)
                .toList();
    }

    private JsonPath fetchPage(final CustomerId customerId, final int page, final int size) {
        return RestAssured.given(requestSpecification)
                .accept("application/vnd.api.v1+json")
                .when()
                .get("/loyalty/customers/{customerId}/movements?page={page}&size={size}",
                        customerId.getValue().toString(), page, size)
                .then()
                .statusCode(200)
                .contentType("application/vnd.api.v1+json")
                .extract().jsonPath();
    }

    private void assertDescendingByAppendedAt(final JsonPath page) {
        final List<String> appendedAts = page.getList("movements.appendedAt", String.class);
        for (int i = 1; i < appendedAts.size(); i++) {
            final ZonedDateTime prev = ZonedDateTime.parse(appendedAts.get(i - 1));
            final ZonedDateTime curr = ZonedDateTime.parse(appendedAts.get(i));
            assertThat(prev).as("appendedAt at index %d must be >= index %d", i - 1, i)
                    .isAfterOrEqualTo(curr);
        }
    }

    private ZonedDateTime firstAppendedAt(final JsonPath page) {
        return ZonedDateTime.parse(page.getList("movements.appendedAt", String.class).get(0));
    }

    private ZonedDateTime lastAppendedAt(final JsonPath page) {
        final List<String> ats = page.getList("movements.appendedAt", String.class);
        return ZonedDateTime.parse(ats.get(ats.size() - 1));
    }
}
