package com.lg.platform.loyalty.acceptance.support;

import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import com.lg.platform.loyalty.domain.valueobject.OrderId;
import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-scenario, mutable state container shared across step
 * definition classes (TASK-019).
 *
 * <p>Cucumber-Spring instantiates this bean once per scenario via
 * the {@code cucumber-glue} scope wired by {@link ScenarioScope}, so
 * mutations from a {@code Given} step are visible in a {@code When}
 * or {@code Then} step in the same scenario, and a fresh instance
 * is provisioned for the next scenario — sidestepping the leakage
 * trap that singleton step beans would create.
 *
 * <p>Holds:
 * <ul>
 *   <li>{@code customerId} — the random {@link CustomerId} the
 *       scenario is operating on (most scenarios touch exactly one
 *       customer; isolation is by random UUID, NOT by DB
 *       truncation, because the framework's
 *       {@code MovementJpaRepository} has no {@code deleteAll}
 *       method — RULE-013 append-only — and a single-context
 *       Cucumber JVM cannot drop schemas between scenarios).</li>
 *   <li>{@code ordersByAlias} — Gherkin scenarios reference orders
 *       by short aliases ({@code "order-A"}, {@code "order-B"}); a
 *       random {@link OrderId} is allocated on first use and
 *       reused on every subsequent reference inside the same
 *       scenario. This lets a multi-step scenario (paid → cancelled
 *       → paid-again) coordinate without leaking concrete UUIDs
 *       into the feature text.</li>
 *   <li>{@code eventIdsByAlias} — same pattern for
 *       {@code originatingEventId}s: a scenario asserting
 *       idempotency (REQ-003 / REQ-006) MUST replay the
 *       <em>same</em> event id on the second call but a different
 *       event id when the spec wants a legitimate re-paid (Q9/R5).</li>
 *   <li>{@code lastCommand} — the last {@link LoyaltyLedgerCommand}
 *       dispatched, retained so a {@code Then} step can
 *       cross-check derived fields (eg. {@code eventReceivedAt}
 *       flowed into the outbox payload).</li>
 *   <li>{@code lastResponse} / {@code lastResponseSpec} —
 *       RestAssured plumbing for REST-driven scenarios
 *       (REQ-009 / REQ-010 / REQ-013): the {@code requestSpec} is
 *       built once in {@code RestSetupHooks} and reused; the
 *       latest {@code Response} is stashed for {@code Then}
 *       assertions on status, headers, and body.</li>
 *   <li>{@code expectedSwallowed} — REQ-015 marker. When set, the
 *       Kafka swallow scenario asserts a SECOND
 *       {@code OrderPaidAvroModel} with the SAME
 *       {@code originatingEventId} produced no additional
 *       movement / outbox row AND the listener did not throw
 *       (verified by Awaitility on movement-count stability).</li>
 * </ul>
 *
 * <p>Lombok {@link Getter}/{@link Setter} keep the surface noise
 * minimal; the maps are plain mutable {@link HashMap}s — no
 * concurrency concern because Cucumber executes scenario steps
 * sequentially on a single thread.
 */
@Component
@ScenarioScope
@Getter
@Setter
public class World {

    private CustomerId customerId;

    private final Map<String, OrderId> ordersByAlias = new HashMap<>();

    private final Map<String, UUID> eventIdsByAlias = new HashMap<>();

    private LoyaltyLedgerCommand lastCommand;

    private RequestSpecification requestSpec;

    private Response lastResponse;

    private boolean expectedSwallowed;

    /**
     * Lazily allocates a random {@link OrderId} for the given
     * Gherkin alias. Subsequent calls with the same alias return
     * the same id — necessary for multi-step scenarios that
     * sequence credit + debit on the SAME order (Cases D / F / G).
     */
    public OrderId orderFor(final String alias) {
        return ordersByAlias.computeIfAbsent(alias, k -> OrderId.random());
    }

    /**
     * Lazily allocates a random event id (UUID v4) for the given
     * alias. {@code "evt-paid-1"} on first use generates a fresh
     * UUID; subsequent uses in the same scenario return that UUID
     * — the substrate REQ-003 / REQ-006 idempotency assertions
     * stand on (replay = same alias = same id; legitimate re-paid
     * = different alias = different id).
     */
    public UUID eventIdFor(final String alias) {
        return eventIdsByAlias.computeIfAbsent(alias, k -> UUID.randomUUID());
    }

    /**
     * Convenience: lazily allocates the customer id on first
     * request. Most scenarios scope a fresh random customer per
     * scenario so the IT-layer database state from prior scenarios
     * (which we cannot truncate, see class javadoc) is irrelevant
     * to assertions on counts / balance for THIS customer.
     */
    public CustomerId customerOrNew() {
        if (customerId == null) {
            customerId = CustomerId.random();
        }
        return customerId;
    }
}
