package com.lg.platform.loyalty.domain.entity;

import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import com.lg.platform.loyalty.domain.valueobject.MovementId;
import com.lg.platform.loyalty.domain.valueobject.OrderId;
import com.lg.platform.loyalty.domain.valueobject.ProcessedInputEventOutcome;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessedInputEventTest {

    private final UUID eventId = UUID.randomUUID();
    private final OrderId orderId = OrderId.random();
    private final CustomerId customerId = CustomerId.random();

    @Test
    void forMovementAppended_setsOutcomeAndMovementId() {
        final MovementId movementId = MovementId.random();

        final ProcessedInputEvent ev = ProcessedInputEvent.forMovementAppended(
                eventId, "OrderPaid", orderId, customerId, movementId);

        assertThat(ev.getId()).isNotNull();
        assertThat(ev.getOriginatingEventId()).isEqualTo(eventId);
        assertThat(ev.getOriginatingEventType()).isEqualTo("OrderPaid");
        assertThat(ev.getOriginatingOrderId()).isEqualTo(orderId);
        assertThat(ev.getOriginatingCustomerId()).isEqualTo(customerId);
        assertThat(ev.getOutcome()).isEqualTo(ProcessedInputEventOutcome.MOVEMENT_APPENDED);
        assertThat(ev.getMovementId()).isEqualTo(movementId);
        assertThat(ev.getVersion()).isZero();
        assertThat(ev.getReceivedAt()).isNotNull();
    }

    @Test
    void forNoopZeroCredit_setsOutcomeAndNullMovementId() {
        final ProcessedInputEvent ev = ProcessedInputEvent.forNoopZeroCredit(
                eventId, "OrderPaid", orderId, customerId);

        assertThat(ev.getOutcome()).isEqualTo(ProcessedInputEventOutcome.NOOP_ZERO_CREDIT);
        assertThat(ev.getMovementId()).isNull();
        assertThat(ev.getVersion()).isZero();
        assertThat(ev.getReceivedAt()).isNotNull();
    }

    @Test
    void forNoopDebitWithoutCredit_setsOutcomeAndNullMovementId() {
        final ProcessedInputEvent ev = ProcessedInputEvent.forNoopDebitWithoutCredit(
                eventId, "OrderCancelled", orderId, customerId);

        assertThat(ev.getOutcome()).isEqualTo(ProcessedInputEventOutcome.NOOP_DEBIT_WITHOUT_CREDIT);
        assertThat(ev.getMovementId()).isNull();
        assertThat(ev.getVersion()).isZero();
        assertThat(ev.getReceivedAt()).isNotNull();
    }
}
