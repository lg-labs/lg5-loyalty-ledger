package com.lg.platform.loyalty.container.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lg.platform.loyalty.application.outbox.model.OutboxMessage;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerInputPort;
import com.lg.platform.loyalty.application.ports.output.repository.CustomerBalanceRepository;
import com.lg.platform.loyalty.application.ports.output.repository.OutboxRepository;
import com.lg.platform.loyalty.container.boot.Bootstrap;
import com.lg.platform.loyalty.dataaccess.movement.entity.MovementJpaEntity;
import com.lg.platform.loyalty.dataaccess.movement.repository.MovementJpaRepository;
import com.lg.platform.loyalty.domain.valueobject.BalanceUpdateCause;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import com.lg.platform.loyalty.domain.valueobject.OrderId;
import com.lg5.spring.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.labs.lg.pentagon.common.domain.valueobject.Money;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cases A, D, F, G from TASK-011 — the mutation paths that append a
 * {@code Movement}, update {@code CustomerBalance}, and queue an outbox
 * row. Postgres-only (no Kafka container — see TASK-013 for the
 * scheduler ITs).
 *
 * <p><strong>Case H deliberately dropped</strong>: the spec text
 * (balance +5 → debit -12 → balance -7) is unreachable through the v1
 * public input-port API — any +12 debit requires a prior +12 credit on
 * the same order, which would push the balance to +17 before the
 * cancel. REQ-007 negative-balance coverage is satisfied by
 * {@code CustomerBalanceRepositoryIT.sequence_of_deltas_yields_expected_final_balance_and_version}
 * (+100, -150, +50 → -50 mid-sequence) at the repo layer, which is
 * closer to the invariant.
 *
 * <p>Bootstrap supplies a fallback {@code LoyaltyLedgerInputPort}
 * Mockito mock via {@code DefaultMocks @ConditionalOnMissingBean}; the
 * real {@code LoyaltyLedgerHandler @Service} shipped in TASK-011 wins
 * the gate, so this IT autowires the production implementation
 * transparently.
 */
@TestPropertySource(properties = {
        "testcontainers.postgres.enabled=true",
        "spring.datasource.url=",
        "spring.datasource.username=",
        "spring.datasource.password="
})
class LoyaltyLedgerHandlerHappyPathIT extends Bootstrap {

    @Autowired private LoyaltyLedgerInputPort handler;
    @Autowired private CustomerBalanceRepository customerBalanceRepository;
    @Autowired private MovementJpaRepository movementJpaRepository;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private ObjectMapper objectMapper;

    /**
     * Case A — OrderPaid 12.95 EUR for a fresh customer (balance 0).
     * Expects: 1 movement (delta=+12, cause=ORDER_PAID), balance=12, 1
     * outbox row with payload newBalance=12, delta=+12,
     * cause="ORDER_PAID".
     */
    @Test
    void caseA_orderPaid_floors_to_12_credits_and_queues_outbox() throws Exception {
        final CustomerId customerId = CustomerId.random();
        final OrderId orderId = OrderId.random();
        final UUID eventId = UUID.randomUUID();
        final int outboxBefore = outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.STARTED).size();

        handler.process(orderPaidCommand(eventId, customerId, orderId, "12.95"));

        // Movement.
        final List<MovementJpaEntity> movements = movementsForOrder(orderId);
        assertThat(movements).hasSize(1);
        assertThat(movements.get(0).getDelta()).isEqualTo(12);
        assertThat(movements.get(0).getCause()).isEqualTo(BalanceUpdateCause.ORDER_PAID);

        // Balance.
        assertThat(customerBalanceRepository.findById(customerId).orElseThrow().getBalance()).isEqualTo(12L);

        // Outbox.
        final List<OutboxMessage> outboxRows = outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.STARTED);
        assertThat(outboxRows).hasSize(outboxBefore + 1);
        final OutboxMessage row = outboxRows.stream()
                .filter(r -> r.sagaId().equals(eventId))
                .findFirst().orElseThrow();
        assertThat(row.type()).isEqualTo("CustomerBalanceUpdated");
        final JsonNode payload = objectMapper.readTree(row.payload());
        assertThat(payload.get("customerId").asText()).isEqualTo(customerId.getValue().toString());
        assertThat(payload.get("newBalance").asLong()).isEqualTo(12L);
        assertThat(payload.get("delta").asInt()).isEqualTo(12);
        assertThat(payload.get("cause").asText()).isEqualTo("ORDER_PAID");
        assertThat(payload.get("originatingEventType").asText()).isEqualTo("OrderPaid");
    }

    /**
     * Case D — OrderCancelled for an order with prior credit +12.
     * Expects: 1 new debit movement (delta=-12, cause=ORDER_CANCELLED),
     * balance back to 0, new outbox row with cause="ORDER_CANCELLED".
     */
    @Test
    void caseD_orderCancelled_after_credit_appends_debit_and_zeroes_balance() throws Exception {
        final CustomerId customerId = CustomerId.random();
        final OrderId orderId = OrderId.random();
        handler.process(orderPaidCommand(UUID.randomUUID(), customerId, orderId, "12.00"));
        assertThat(customerBalanceRepository.findById(customerId).orElseThrow().getBalance()).isEqualTo(12L);

        final UUID cancelEventId = UUID.randomUUID();
        handler.process(orderCancelledCommand(cancelEventId, customerId, orderId));

        final List<MovementJpaEntity> movements = movementsForOrder(orderId);
        assertThat(movements).hasSize(2);
        final MovementJpaEntity debit = movements.stream()
                .filter(m -> m.getDelta() < 0).findFirst().orElseThrow();
        assertThat(debit.getDelta()).isEqualTo(-12);
        assertThat(debit.getCause()).isEqualTo(BalanceUpdateCause.ORDER_CANCELLED);

        assertThat(customerBalanceRepository.findById(customerId).orElseThrow().getBalance()).isZero();

        final OutboxMessage cancelRow = outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.STARTED).stream()
                .filter(r -> r.sagaId().equals(cancelEventId))
                .findFirst().orElseThrow();
        final JsonNode payload = objectMapper.readTree(cancelRow.payload());
        assertThat(payload.get("delta").asInt()).isEqualTo(-12);
        assertThat(payload.get("newBalance").asLong()).isZero();
        assertThat(payload.get("cause").asText()).isEqualTo("ORDER_CANCELLED");
    }

    /**
     * Case F — OrderRefunded for an order with prior credit +12. Same
     * shape as Case D but with cause=ORDER_REFUNDED.
     */
    @Test
    void caseF_orderRefunded_after_credit_appends_debit_with_refund_cause() throws Exception {
        final CustomerId customerId = CustomerId.random();
        final OrderId orderId = OrderId.random();
        handler.process(orderPaidCommand(UUID.randomUUID(), customerId, orderId, "12.00"));

        final UUID refundEventId = UUID.randomUUID();
        handler.process(orderRefundedCommand(refundEventId, customerId, orderId));

        final List<MovementJpaEntity> movements = movementsForOrder(orderId);
        assertThat(movements).hasSize(2);
        final MovementJpaEntity debit = movements.stream()
                .filter(m -> m.getDelta() < 0).findFirst().orElseThrow();
        assertThat(debit.getDelta()).isEqualTo(-12);
        assertThat(debit.getCause()).isEqualTo(BalanceUpdateCause.ORDER_REFUNDED);

        final OutboxMessage refundRow = outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.STARTED).stream()
                .filter(r -> r.sagaId().equals(refundEventId))
                .findFirst().orElseThrow();
        final JsonNode payload = objectMapper.readTree(refundRow.payload());
        assertThat(payload.get("cause").asText()).isEqualTo("ORDER_REFUNDED");
    }

    /**
     * Case G — OrderPaid → OrderCancelled → OrderPaid again on the same
     * orderId with DISTINCT event ids (REQ-003 + ADR-003: dedup is by
     * event id, not by orderId). Expected: two credits + one debit
     * appended in the order received; final balance = +12.
     */
    @Test
    void caseG_paid_cancelled_paid_again_distinct_event_ids_finals_at_plus_12() {
        final CustomerId customerId = CustomerId.random();
        final OrderId orderId = OrderId.random();
        handler.process(orderPaidCommand(UUID.randomUUID(), customerId, orderId, "12.00"));
        handler.process(orderCancelledCommand(UUID.randomUUID(), customerId, orderId));
        handler.process(orderPaidCommand(UUID.randomUUID(), customerId, orderId, "12.00"));

        assertThat(customerBalanceRepository.findById(customerId).orElseThrow().getBalance()).isEqualTo(12L);
        final List<MovementJpaEntity> movements = movementsForOrder(orderId);
        assertThat(movements).hasSize(3);
        assertThat(movements.stream().mapToInt(MovementJpaEntity::getDelta).sum()).isEqualTo(12);
        assertThat(movements.stream().filter(m -> m.getDelta() > 0).count()).isEqualTo(2);
        assertThat(movements.stream().filter(m -> m.getDelta() < 0).count()).isEqualTo(1);
    }

    // ---- helpers ----

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

    private LoyaltyLedgerCommand.OrderRefundedCommand orderRefundedCommand(final UUID eventId,
                                                                           final CustomerId customerId,
                                                                           final OrderId orderId) {
        return new LoyaltyLedgerCommand.OrderRefundedCommand(
                eventId, customerId, orderId, ZonedDateTime.now());
    }

    private List<MovementJpaEntity> movementsForOrder(final OrderId orderId) {
        return movementJpaRepository.findByOriginatingOrderIdOrderByAppendedAtAsc(orderId.getValue());
    }
}
