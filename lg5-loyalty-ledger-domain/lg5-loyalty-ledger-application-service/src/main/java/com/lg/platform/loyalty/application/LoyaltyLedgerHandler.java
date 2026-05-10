package com.lg.platform.loyalty.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lg.platform.loyalty.application.outbox.model.OutboxMessage;
import com.lg.platform.loyalty.application.outbox.payload.CustomerBalanceUpdatedEventPayload;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand.OrderCancelledCommand;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand.OrderPaidCommand;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand.OrderRefundedCommand;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerInputPort;
import com.lg.platform.loyalty.application.ports.output.repository.CustomerBalanceRepository;
import com.lg.platform.loyalty.application.ports.output.repository.MovementLedgerRepository;
import com.lg.platform.loyalty.application.ports.output.repository.OutboxRepository;
import com.lg.platform.loyalty.application.ports.output.repository.ProcessedInputEventRepository;
import com.lg.platform.loyalty.domain.entity.CustomerBalance;
import com.lg.platform.loyalty.domain.entity.Movement;
import com.lg.platform.loyalty.domain.entity.ProcessedInputEvent;
import com.lg.platform.loyalty.domain.event.CustomerBalanceUpdatedEvent;
import com.lg.platform.loyalty.domain.valueobject.BalanceUpdateCause;
import com.lg.platform.loyalty.domain.valueobject.LoyaltyPoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

/**
 * Concrete implementation of {@link LoyaltyLedgerInputPort} (TASK-011).
 *
 * <p>Implements the inbound-event handling algorithm specified in
 * {@code data-model.md §Idempotency strategy}:
 *
 * <ol>
 *   <li><strong>Dedup gate</strong> — insert a {@link ProcessedInputEvent}
 *       row first, with the unique constraint
 *       {@code uq_processed_event_type_id} on
 *       {@code (originating_event_type, originating_event_id)} acting
 *       as the dedup token (ADR-003). A duplicate insert raises
 *       {@code DataIntegrityViolationException} which is rethrown to
 *       the listener layer (RULE-010 + ADR-003 swallows it as NO-OP
 *       there). The insert chooses one of three factories based on
 *       the upcoming outcome — this avoids a write-then-update on the
 *       same row in the same transaction.</li>
 *   <li><strong>Branch on command type</strong>:
 *     <ul>
 *       <li>{@link OrderPaidCommand} with floored points {@code = 0}
 *           (REQ-002 / Q1 — e.g. paidAmount {@code 0.50 EUR}) → write
 *           {@code outcome = NOOP_ZERO_CREDIT}, no movement, no outbox.</li>
 *       <li>{@link OrderCancelledCommand} or {@link OrderRefundedCommand}
 *           when no prior credit exists for the order (REQ-005 / Q2)
 *           → write {@code outcome = NOOP_DEBIT_WITHOUT_CREDIT}, log
 *           a WARN, no movement, no outbox.</li>
 *       <li>Otherwise → append a {@link Movement}, update the
 *           {@link CustomerBalance} (created lazily via
 *           {@link CustomerBalance#empty(com.lg.platform.loyalty.domain.valueobject.CustomerId)}),
 *           write {@code outcome = MOVEMENT_APPENDED} with the
 *           movement id, append a {@link
 *           com.lg5.spring.outbox.OutboxStatus#STARTED STARTED}
 *           {@link OutboxMessage} carrying the JSON-serialized
 *           {@link CustomerBalanceUpdatedEventPayload}.</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p>Single {@code @Transactional} boundary — all four writes (dedup
 * row, movement, balance, outbox) commit or roll back atomically per
 * the §Idempotency strategy. The listener layer
 * ({@code OrderPaidKafkaListener} et al.) catches both
 * {@link org.springframework.dao.OptimisticLockingFailureException}
 * (concurrent balance write) and
 * {@link org.springframework.dao.DataIntegrityViolationException}
 * (replay) as NO-OP without rethrowing.
 */
@Slf4j
@Service
public class LoyaltyLedgerHandler implements LoyaltyLedgerInputPort {

    private final ProcessedInputEventRepository processedInputEventRepository;
    private final MovementLedgerRepository movementLedgerRepository;
    private final CustomerBalanceRepository customerBalanceRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public LoyaltyLedgerHandler(final ProcessedInputEventRepository processedInputEventRepository,
                                final MovementLedgerRepository movementLedgerRepository,
                                final CustomerBalanceRepository customerBalanceRepository,
                                final OutboxRepository outboxRepository,
                                final ObjectMapper objectMapper) {
        this.processedInputEventRepository = processedInputEventRepository;
        this.movementLedgerRepository = movementLedgerRepository;
        this.customerBalanceRepository = customerBalanceRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void process(final LoyaltyLedgerCommand command) {
        switch (command) {
            case OrderPaidCommand op -> handleOrderPaid(op);
            case OrderCancelledCommand oc -> handleDebit(oc, BalanceUpdateCause.ORDER_CANCELLED);
            case OrderRefundedCommand or -> handleDebit(or, BalanceUpdateCause.ORDER_REFUNDED);
        }
    }

    private void handleOrderPaid(final OrderPaidCommand command) {
        final int points = LoyaltyPoints.floorEurosFrom(command.paidAmount());

        if (points == 0) {
            // REQ-002 / Q1: zero-credit short-circuit. Insert the dedup
            // row with NOOP_ZERO_CREDIT outcome; if the event was
            // already processed, the unique constraint raises
            // DataIntegrityViolationException which the listener
            // swallows (RULE-010 + ADR-003).
            processedInputEventRepository.save(
                    ProcessedInputEvent.forNoopZeroCredit(
                            command.eventId(),
                            command.eventType(),
                            command.orderId(),
                            command.customerId()));
            log.info("OrderPaid eventId={} order={} customer={} → NOOP_ZERO_CREDIT (paidAmount<1 EUR)",
                    command.eventId(), command.orderId().getValue(), command.customerId().getValue());
            return;
        }

        appendMovementAndOutbox(command, points, BalanceUpdateCause.ORDER_PAID);
    }

    private void handleDebit(final LoyaltyLedgerCommand command, final BalanceUpdateCause cause) {
        if (!movementLedgerRepository.existsCreditFor(command.orderId())) {
            // REQ-005 / Q2: cancel/refund without prior credit is a NO-OP.
            processedInputEventRepository.save(
                    ProcessedInputEvent.forNoopDebitWithoutCredit(
                            command.eventId(),
                            command.eventType(),
                            command.orderId(),
                            command.customerId()));
            log.warn("{} eventId={} order={} customer={} → NOOP_DEBIT_WITHOUT_CREDIT (no prior credit)",
                    command.eventType(), command.eventId(),
                    command.orderId().getValue(), command.customerId().getValue());
            return;
        }

        // The debit amount equals the prior credit's delta. Per
        // data-model.md §Movement, debits carry the SAME absolute
        // value as the credit they reverse — the loyalty domain has
        // no notion of partial cancel/refund in v1 (PRD §Out-of-scope).
        // We compute the debit by summing the credit deltas for this
        // orderId; in v1 there is exactly one credit per order so we
        // can just look it up.
        final int creditDelta = lookupCreditDeltaForOrder(command);
        appendMovementAndOutbox(command, -creditDelta, cause);
    }

    private int lookupCreditDeltaForOrder(final LoyaltyLedgerCommand command) {
        // The movement-port surface only exposes existsCreditFor(...) +
        // findById(...). Looking up the credit's delta requires
        // querying by orderId; we delegate via a dedicated port
        // method on MovementLedgerRepository — added on a follow-up
        // — for now we recompute by scanning the customer's recent
        // movements. To keep this PR focused and the surface small,
        // we cheat: the credit must exist (existsCreditFor returned
        // true), and the only credit cause is ORDER_PAID. We use a
        // sum over the order's movements via a helper port method.
        //
        // PRAGMATIC v1 IMPLEMENTATION: re-issue the query via the
        // JPA repository through a dedicated method. We add it now.
        return movementLedgerRepository.sumPositiveDeltaForOrder(command.orderId());
    }

    private void appendMovementAndOutbox(final LoyaltyLedgerCommand command,
                                         final int delta,
                                         final BalanceUpdateCause cause) {
        // 1. Append movement.
        final Movement movement = (cause == BalanceUpdateCause.ORDER_PAID)
                ? Movement.ofCredit(
                        command.customerId(),
                        command.orderId(),
                        command.eventId(),
                        command.eventType(),
                        command.eventReceivedAt(),
                        delta)
                : Movement.ofDebit(
                        command.customerId(),
                        command.orderId(),
                        command.eventId(),
                        command.eventType(),
                        command.eventReceivedAt(),
                        delta,
                        cause);
        final Movement saved = movementLedgerRepository.save(movement);

        // 2. Insert dedup row referencing the movement id.
        processedInputEventRepository.save(
                ProcessedInputEvent.forMovementAppended(
                        command.eventId(),
                        command.eventType(),
                        command.orderId(),
                        command.customerId(),
                        saved.getId()));

        // 3. Update customer balance (UPSERT).
        final CustomerBalance balance = customerBalanceRepository.findById(command.customerId())
                .orElseGet(() -> CustomerBalance.empty(command.customerId()));
        balance.applyDelta(delta);
        final CustomerBalance savedBalance = customerBalanceRepository.save(balance);

        // 4. Append outbox row carrying the JSON event payload.
        final CustomerBalanceUpdatedEvent event = new CustomerBalanceUpdatedEvent(
                command.customerId(),
                savedBalance.getBalance(),
                delta,
                cause,
                command.orderId(),
                command.eventId(),
                command.eventType(),
                ZonedDateTime.now());
        final CustomerBalanceUpdatedEventPayload payload =
                CustomerBalanceUpdatedEventPayload.from(event);
        outboxRepository.save(
                OutboxMessage.started(
                        command.eventId(),
                        "CustomerBalanceUpdated",
                        toJson(payload)));

        log.info("{} eventId={} order={} customer={} delta={} balance={} → MOVEMENT_APPENDED + outbox queued",
                command.eventType(), command.eventId(),
                command.orderId().getValue(), command.customerId().getValue(),
                delta, savedBalance.getBalance());
    }

    private String toJson(final CustomerBalanceUpdatedEventPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (final JsonProcessingException e) {
            // Should never happen: payload is a record of primitives +
            // String + UUID + ZonedDateTime, all of which Jackson
            // handles natively. Wrap in unchecked so the @Transactional
            // boundary rolls back the entire dedup → movement →
            // balance set; the inbound listener will then redeliver.
            throw new IllegalStateException(
                    "Failed to serialize CustomerBalanceUpdatedEventPayload to JSON", e);
        }
    }
}
