package com.lg.platform.loyalty.domain.event;

import com.lg.platform.loyalty.domain.valueobject.BalanceUpdateCause;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import com.lg.platform.loyalty.domain.valueobject.OrderId;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerBalanceUpdatedEventTest {

    @Test
    void record_isImmutable_andEqualsByValue() {
        final CustomerId customerId = CustomerId.random();
        final OrderId orderId = OrderId.random();
        final UUID eventId = UUID.randomUUID();
        final ZonedDateTime occurredAt = ZonedDateTime.now();

        final CustomerBalanceUpdatedEvent a = new CustomerBalanceUpdatedEvent(
                customerId, 12L, 12, BalanceUpdateCause.ORDER_PAID,
                orderId, eventId, "OrderPaid", occurredAt);
        final CustomerBalanceUpdatedEvent b = new CustomerBalanceUpdatedEvent(
                customerId, 12L, 12, BalanceUpdateCause.ORDER_PAID,
                orderId, eventId, "OrderPaid", occurredAt);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.customerId()).isEqualTo(customerId);
        assertThat(a.newBalance()).isEqualTo(12L);
        assertThat(a.delta()).isEqualTo(12);
        assertThat(a.cause()).isEqualTo(BalanceUpdateCause.ORDER_PAID);
        assertThat(a.originatingOrderId()).isEqualTo(orderId);
        assertThat(a.originatingEventId()).isEqualTo(eventId);
        assertThat(a.originatingEventType()).isEqualTo("OrderPaid");
        assertThat(a.occurredAt()).isEqualTo(occurredAt);
    }
}
