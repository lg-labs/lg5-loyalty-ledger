package com.lg.platform.loyalty.application.ports.output.repository;

import com.lg.platform.loyalty.domain.entity.Movement;
import com.lg.platform.loyalty.domain.valueobject.MovementId;
import com.lg.platform.loyalty.domain.valueobject.OrderId;

import java.util.Optional;

/**
 * Output port (hexagonal) for the append-only {@code movement} ledger
 * (REQ-013, ADR-004).
 *
 * <p>Intentionally exposes only read operations and a single
 * append-style {@link #save(Movement)} — there is no {@code update}
 * or {@code delete}. The implementing adapter
 * ({@code MovementLedgerRepositoryImpl}) wraps a Spring Data JPA
 * repository whose surface is similarly restricted.
 */
public interface MovementLedgerRepository {

    /**
     * Appends a new {@link Movement} row.
     * <p>The aggregate is immutable and built with a fresh {@link MovementId};
     * callers MUST NOT pass an id that already exists in the ledger.
     */
    Movement save(Movement movement);

    /**
     * Looks up a movement by primary key (used mainly by tests and
     * by the projection / outbox logic that links a
     * {@code processed_input_event} row to its movement).
     */
    Optional<Movement> findById(MovementId movementId);

    /**
     * REQ-004 / data-model.md §Idempotency: returns {@code true} when a
     * prior credit ({@code cause = ORDER_PAID}, {@code delta > 0}) exists
     * for the given originating order. The inbound handler uses this
     * predicate to short-circuit a debit-without-credit into
     * {@code outcome = NOOP_DEBIT_WITHOUT_CREDIT}.
     */
    boolean existsCreditFor(OrderId originatingOrderId);

    /**
     * REQ-004 / data-model.md §Movement: returns the sum of positive
     * deltas (credits) ever recorded for the given originating order.
     * Used by the inbound handler to compute the magnitude of the
     * compensating debit when an {@code OrderCancelled} or
     * {@code OrderRefunded} event arrives — debits in v1 always carry
     * {@code -sumOfCredits} (no partial cancel / refund per PRD
     * §Out-of-scope). Returns {@code 0} when no credit exists; the
     * handler short-circuits that case via {@link #existsCreditFor}
     * before calling this method.
     */
    int sumPositiveDeltaForOrder(OrderId originatingOrderId);
}
