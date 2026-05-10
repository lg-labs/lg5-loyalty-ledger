package com.lg.platform.loyalty.domain.entity;

import com.lg.platform.loyalty.domain.exception.LoyaltyLedgerDomainException;
import com.lg.platform.loyalty.domain.valueobject.BalanceUpdateCause;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import com.lg.platform.loyalty.domain.valueobject.OrderId;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovementTest {

    private final CustomerId customerId = CustomerId.random();
    private final OrderId orderId = OrderId.random();
    private final UUID eventId = UUID.randomUUID();
    private final ZonedDateTime receivedAt = ZonedDateTime.now();

    @Test
    void ofCredit_withPositiveDelta_createsMovementWithOrderPaidCause() {
        final Movement m = Movement.ofCredit(customerId, orderId, eventId, "OrderPaid", receivedAt, 12);

        assertThat(m.getId()).isNotNull();
        assertThat(m.getCustomerId()).isEqualTo(customerId);
        assertThat(m.getOriginatingOrderId()).isEqualTo(orderId);
        assertThat(m.getOriginatingEventId()).isEqualTo(eventId);
        assertThat(m.getOriginatingEventType()).isEqualTo("OrderPaid");
        assertThat(m.getOriginatingEventReceivedAt()).isEqualTo(receivedAt);
        assertThat(m.getDelta()).isEqualTo(12);
        assertThat(m.getCause()).isEqualTo(BalanceUpdateCause.ORDER_PAID);
        assertThat(m.getAppendedAt()).isNotNull();
        assertThat(m.getVersion()).isZero();
    }

    @Test
    void ofCredit_withZeroDelta_throwsLoyaltyLedgerDomainException() {
        assertThatThrownBy(() -> Movement.ofCredit(customerId, orderId, eventId, "OrderPaid", receivedAt, 0))
                .isInstanceOf(LoyaltyLedgerDomainException.class)
                .hasMessageContaining("zero");
    }

    @Test
    void ofCredit_withNegativeDelta_throwsLoyaltyLedgerDomainException() {
        assertThatThrownBy(() -> Movement.ofCredit(customerId, orderId, eventId, "OrderPaid", receivedAt, -3))
                .isInstanceOf(LoyaltyLedgerDomainException.class)
                .hasMessageContaining("delta > 0");
    }

    @Test
    void ofDebit_withNegativeDelta_andOrderCancelledCause_createsMovement() {
        final Movement m = Movement.ofDebit(customerId, orderId, eventId, "OrderCancelled",
                receivedAt, -12, BalanceUpdateCause.ORDER_CANCELLED);

        assertThat(m.getDelta()).isEqualTo(-12);
        assertThat(m.getCause()).isEqualTo(BalanceUpdateCause.ORDER_CANCELLED);
        assertThat(m.getOriginatingEventType()).isEqualTo("OrderCancelled");
        assertThat(m.getVersion()).isZero();
    }

    @Test
    void ofDebit_withNegativeDelta_andOrderRefundedCause_createsMovement() {
        final Movement m = Movement.ofDebit(customerId, orderId, eventId, "OrderRefunded",
                receivedAt, -7, BalanceUpdateCause.ORDER_REFUNDED);

        assertThat(m.getDelta()).isEqualTo(-7);
        assertThat(m.getCause()).isEqualTo(BalanceUpdateCause.ORDER_REFUNDED);
    }

    @Test
    void ofDebit_withPositiveDelta_throwsLoyaltyLedgerDomainException() {
        assertThatThrownBy(() -> Movement.ofDebit(customerId, orderId, eventId, "OrderCancelled",
                receivedAt, 5, BalanceUpdateCause.ORDER_CANCELLED))
                .isInstanceOf(LoyaltyLedgerDomainException.class)
                .hasMessageContaining("delta < 0");
    }

    @Test
    void ofDebit_withOrderPaidCause_throwsLoyaltyLedgerDomainException() {
        assertThatThrownBy(() -> Movement.ofDebit(customerId, orderId, eventId, "OrderPaid",
                receivedAt, -5, BalanceUpdateCause.ORDER_PAID))
                .isInstanceOf(LoyaltyLedgerDomainException.class)
                .hasMessageContaining("ORDER_CANCELLED");
    }

    @Test
    void ofDebit_withZeroDelta_throwsLoyaltyLedgerDomainException() {
        assertThatThrownBy(() -> Movement.ofDebit(customerId, orderId, eventId, "OrderCancelled",
                receivedAt, 0, BalanceUpdateCause.ORDER_CANCELLED))
                .isInstanceOf(LoyaltyLedgerDomainException.class);
    }

    @Test
    void version_isZeroAtCreation_andHasNoMutator() {
        final Movement m = Movement.ofCredit(customerId, orderId, eventId, "OrderPaid", receivedAt, 1);
        assertThat(m.getVersion()).isZero();
    }

    @Test
    void noPublicMutator_existsOnMovement() {
        // REQ-013: Movement is append-only — no setters declared on Movement itself.
        final String mutators = Arrays.stream(Movement.class.getDeclaredMethods())
                .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .collect(Collectors.joining(", "));

        assertThat(mutators)
                .as("Movement.class declares no public setters (REQ-013)")
                .isEmpty();
    }
}
