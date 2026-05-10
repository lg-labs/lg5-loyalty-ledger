package com.lg.platform.loyalty.container.application;

import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerInputPort;
import com.lg.platform.loyalty.application.ports.output.repository.CustomerBalanceRepository;
import com.lg.platform.loyalty.application.ports.output.repository.OutboxRepository;
import com.lg.platform.loyalty.container.boot.Bootstrap;
import com.lg.platform.loyalty.dataaccess.movement.entity.MovementJpaEntity;
import com.lg.platform.loyalty.dataaccess.movement.repository.MovementJpaRepository;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import com.lg.platform.loyalty.domain.valueobject.OrderId;
import com.lg5.spring.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import com.labs.lg.pentagon.common.domain.valueobject.Money;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cases B, C, E from TASK-011 — the NO-OP and dedup paths that
 * deliberately do <em>not</em> append a {@code Movement} or queue an
 * outbox row.
 *
 * <ul>
 *   <li><strong>B</strong> — OrderPaid 0.50 EUR → floors to 0 →
 *       {@code NOOP_ZERO_CREDIT} (REQ-002 / Q1).</li>
 *   <li><strong>C</strong> — replay of OrderPaid (same event id)
 *       → second invocation raises {@link DataIntegrityViolationException}
 *       on the {@code uq_processed_event_type_id} unique constraint
 *       (ADR-003 dedup gate). The Kafka listener swallows this as
 *       NO-OP per RULE-010 — but at the application-service surface
 *       the exception MUST be observable so the listener can act on
 *       it.</li>
 *   <li><strong>E</strong> — OrderCancelled with no prior credit →
 *       {@code NOOP_DEBIT_WITHOUT_CREDIT} (REQ-005 / Q2).</li>
 * </ul>
 *
 * <p>Postgres-only — same testcontainer gating as the happy-path IT.
 */
@TestPropertySource(properties = {
        "testcontainers.postgres.enabled=true",
        "spring.datasource.url=",
        "spring.datasource.username=",
        "spring.datasource.password="
})
class LoyaltyLedgerHandlerEdgeCasesIT extends Bootstrap {

    @Autowired private LoyaltyLedgerInputPort handler;
    @Autowired private CustomerBalanceRepository customerBalanceRepository;
    @Autowired private MovementJpaRepository movementJpaRepository;
    @Autowired private OutboxRepository outboxRepository;

    /**
     * Case B — OrderPaid 0.50 EUR (floors to 0). No movement, no balance
     * change, no outbox row. Only side-effect: dedup row written with
     * outcome=NOOP_ZERO_CREDIT (asserted indirectly via Case C, which
     * relies on the unique constraint).
     */
    @Test
    void caseB_orderPaid_below_one_euro_is_noop_zero_credit() {
        final CustomerId customerId = CustomerId.random();
        final OrderId orderId = OrderId.random();
        final int outboxBefore = outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.STARTED).size();

        handler.process(orderPaidCommand(UUID.randomUUID(), customerId, orderId, "0.50"));

        // No movement appended.
        final List<MovementJpaEntity> movements = movementJpaRepository
                .findByOriginatingOrderIdOrderByAppendedAtAsc(orderId.getValue());
        assertThat(movements).isEmpty();

        // No balance row created.
        assertThat(customerBalanceRepository.findById(customerId)).isEmpty();

        // No outbox row queued.
        assertThat(outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.STARTED))
                .hasSize(outboxBefore);
    }

    /**
     * Case C — Replay of OrderPaid with the SAME event id. First
     * invocation succeeds (movement appended, balance updated, outbox
     * queued); second invocation hits
     * {@code uq_processed_event_type_id} on the dedup row insert and
     * raises {@link DataIntegrityViolationException}, which the
     * {@code @Transactional} boundary rolls back — so no second
     * movement and no second outbox row appear (REQ-003 + ADR-003).
     */
    @Test
    void caseC_replay_of_orderPaid_raises_DataIntegrityViolation_and_rolls_back() {
        final CustomerId customerId = CustomerId.random();
        final OrderId orderId = OrderId.random();
        final UUID eventId = UUID.randomUUID();

        handler.process(orderPaidCommand(eventId, customerId, orderId, "12.00"));
        assertThat(customerBalanceRepository.findById(customerId).orElseThrow().getBalance()).isEqualTo(12L);
        final int outboxAfterFirst =
                outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.STARTED).size();

        // Replay — same event id ⇒ DataIntegrityViolationException at
        // the dedup-row insert (the listener layer swallows this as NO-OP
        // per RULE-010, but the handler MUST surface it so the swallow
        // can happen).
        assertThatThrownBy(() -> handler.process(
                orderPaidCommand(eventId, customerId, orderId, "12.00")))
                .isInstanceOf(DataIntegrityViolationException.class);

        // Balance unchanged (txn rolled back).
        assertThat(customerBalanceRepository.findById(customerId).orElseThrow().getBalance()).isEqualTo(12L);
        // Still exactly one movement.
        assertThat(movementJpaRepository.findByOriginatingOrderIdOrderByAppendedAtAsc(orderId.getValue()))
                .hasSize(1);
        // Still exactly the same outbox count (no second row).
        assertThat(outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.STARTED))
                .hasSize(outboxAfterFirst);
    }

    /**
     * Case E — OrderCancelled for an order with NO prior credit
     * (REQ-005 / Q2). No movement appended, no balance row created, no
     * outbox row queued. Dedup row is written with
     * outcome=NOOP_DEBIT_WITHOUT_CREDIT.
     */
    @Test
    void caseE_orderCancelled_without_prior_credit_is_noop_debit_without_credit() {
        final CustomerId customerId = CustomerId.random();
        final OrderId orderId = OrderId.random();
        final int outboxBefore = outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.STARTED).size();

        handler.process(orderCancelledCommand(UUID.randomUUID(), customerId, orderId));

        assertThat(movementJpaRepository.findByOriginatingOrderIdOrderByAppendedAtAsc(orderId.getValue()))
                .isEmpty();
        assertThat(customerBalanceRepository.findById(customerId)).isEmpty();
        assertThat(outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.STARTED))
                .hasSize(outboxBefore);
    }

    // ---- helpers (duplicated from HappyPathIT to keep each IT self-contained) ----

    private LoyaltyLedgerCommand.OrderPaidCommand orderPaidCommand(final UUID eventId,
                                                                   final CustomerId customerId,
                                                                   final OrderId orderId,
                                                                   final String paidAmount) {
        return new LoyaltyLedgerCommand.OrderPaidCommand(
                eventId, customerId, orderId,
                ZonedDateTime.now(),
                new Money(new BigDecimal(paidAmount)));
    }

    private LoyaltyLedgerCommand.OrderCancelledCommand orderCancelledCommand(final UUID eventId,
                                                                             final CustomerId customerId,
                                                                             final OrderId orderId) {
        return new LoyaltyLedgerCommand.OrderCancelledCommand(
                eventId, customerId, orderId, ZonedDateTime.now());
    }
}
