package com.lg.platform.loyalty.domain.event;

import com.labs.lg.pentagon.common.domain.event.DomainEvent;
import com.lg.platform.loyalty.domain.valueobject.BalanceUpdateCause;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import com.lg.platform.loyalty.domain.valueobject.OrderId;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Raised after a {@code Movement} is appended and {@code CustomerBalance}
 * is updated, in the application-service handler. NOT raised for
 * REQ-002/Q1 zero-credit or REQ-005 no-op cases.
 */
public record CustomerBalanceUpdatedEvent(
        CustomerId customerId,
        long newBalance,
        int delta,
        BalanceUpdateCause cause,
        OrderId originatingOrderId,
        UUID originatingEventId,
        String originatingEventType,
        ZonedDateTime occurredAt) implements DomainEvent<CustomerId> {
}
