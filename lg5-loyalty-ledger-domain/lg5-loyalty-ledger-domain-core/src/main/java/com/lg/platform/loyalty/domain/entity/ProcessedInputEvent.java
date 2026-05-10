package com.lg.platform.loyalty.domain.entity;

import com.labs.lg.pentagon.common.domain.entity.AggregateRoot;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import com.lg.platform.loyalty.domain.valueobject.MovementId;
import com.lg.platform.loyalty.domain.valueobject.OrderId;
import com.lg.platform.loyalty.domain.valueobject.ProcessedInputEventId;
import com.lg.platform.loyalty.domain.valueobject.ProcessedInputEventOutcome;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Dedup guard + audit record of every inbound business event (ADR-003).
 * <p>One instance per {@code (eventType, eventId)}; uniqueness enforces dedup.
 * <p>Invariant: {@code outcome == MOVEMENT_APPENDED} ⇔ {@code movementId != null}.
 */
public final class ProcessedInputEvent extends AggregateRoot<ProcessedInputEventId> {

    private final UUID originatingEventId;
    private final String originatingEventType;
    private final OrderId originatingOrderId;
    private final CustomerId originatingCustomerId;
    private final ZonedDateTime receivedAt;
    private final ProcessedInputEventOutcome outcome;
    private final MovementId movementId;
    private final int version;

    private ProcessedInputEvent(final ProcessedInputEventId id,
                                final UUID originatingEventId,
                                final String originatingEventType,
                                final OrderId originatingOrderId,
                                final CustomerId originatingCustomerId,
                                final ZonedDateTime receivedAt,
                                final ProcessedInputEventOutcome outcome,
                                final MovementId movementId,
                                final int version) {
        super.setId(id);
        this.originatingEventId = originatingEventId;
        this.originatingEventType = originatingEventType;
        this.originatingOrderId = originatingOrderId;
        this.originatingCustomerId = originatingCustomerId;
        this.receivedAt = receivedAt;
        this.outcome = outcome;
        this.movementId = movementId;
        this.version = version;
    }

    public static ProcessedInputEvent forMovementAppended(final UUID eventId,
                                                          final String eventType,
                                                          final OrderId orderId,
                                                          final CustomerId customerId,
                                                          final MovementId movementId) {
        return new ProcessedInputEvent(
                ProcessedInputEventId.random(),
                eventId,
                eventType,
                orderId,
                customerId,
                ZonedDateTime.now(),
                ProcessedInputEventOutcome.MOVEMENT_APPENDED,
                movementId,
                0);
    }

    public static ProcessedInputEvent forNoopZeroCredit(final UUID eventId,
                                                        final String eventType,
                                                        final OrderId orderId,
                                                        final CustomerId customerId) {
        return new ProcessedInputEvent(
                ProcessedInputEventId.random(),
                eventId,
                eventType,
                orderId,
                customerId,
                ZonedDateTime.now(),
                ProcessedInputEventOutcome.NOOP_ZERO_CREDIT,
                null,
                0);
    }

    public static ProcessedInputEvent forNoopDebitWithoutCredit(final UUID eventId,
                                                                final String eventType,
                                                                final OrderId orderId,
                                                                final CustomerId customerId) {
        return new ProcessedInputEvent(
                ProcessedInputEventId.random(),
                eventId,
                eventType,
                orderId,
                customerId,
                ZonedDateTime.now(),
                ProcessedInputEventOutcome.NOOP_DEBIT_WITHOUT_CREDIT,
                null,
                0);
    }

    public UUID getOriginatingEventId() {
        return originatingEventId;
    }

    public String getOriginatingEventType() {
        return originatingEventType;
    }

    public OrderId getOriginatingOrderId() {
        return originatingOrderId;
    }

    public CustomerId getOriginatingCustomerId() {
        return originatingCustomerId;
    }

    public ZonedDateTime getReceivedAt() {
        return receivedAt;
    }

    public ProcessedInputEventOutcome getOutcome() {
        return outcome;
    }

    public MovementId getMovementId() {
        return movementId;
    }

    public int getVersion() {
        return version;
    }
}
