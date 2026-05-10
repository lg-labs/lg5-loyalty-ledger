package com.lg.platform.loyalty.domain.entity;

import com.labs.lg.pentagon.common.domain.entity.AggregateRoot;
import com.lg.platform.loyalty.domain.exception.LoyaltyLedgerDomainException;
import com.lg.platform.loyalty.domain.valueobject.BalanceUpdateCause;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import com.lg.platform.loyalty.domain.valueobject.MovementId;
import com.lg.platform.loyalty.domain.valueobject.OrderId;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Immutable ledger entry — a credit or a debit applied to one customer
 * at one point in time, originated by exactly one inbound business event.
 *
 * <p>Invariants (data-model.md §Movement):
 * <ul>
 *   <li>{@code delta != 0}</li>
 *   <li>{@code cause == ORDER_PAID} ⇔ {@code delta > 0}</li>
 *   <li>{@code cause ∈ {ORDER_CANCELLED, ORDER_REFUNDED}} ⇔ {@code delta < 0}</li>
 * </ul>
 *
 * <p>REQ-013: append-only; no public mutator method.
 */
public final class Movement extends AggregateRoot<MovementId> {

    private final CustomerId customerId;
    private final int delta;
    private final BalanceUpdateCause cause;
    private final OrderId originatingOrderId;
    private final UUID originatingEventId;
    private final String originatingEventType;
    private final ZonedDateTime originatingEventReceivedAt;
    private final ZonedDateTime appendedAt;
    private final int version;

    private Movement(final MovementId id,
                     final CustomerId customerId,
                     final int delta,
                     final BalanceUpdateCause cause,
                     final OrderId originatingOrderId,
                     final UUID originatingEventId,
                     final String originatingEventType,
                     final ZonedDateTime originatingEventReceivedAt,
                     final ZonedDateTime appendedAt,
                     final int version) {
        super.setId(id);
        this.customerId = customerId;
        this.delta = delta;
        this.cause = cause;
        this.originatingOrderId = originatingOrderId;
        this.originatingEventId = originatingEventId;
        this.originatingEventType = originatingEventType;
        this.originatingEventReceivedAt = originatingEventReceivedAt;
        this.appendedAt = appendedAt;
        this.version = version;
    }

    /**
     * Creates a credit movement (positive delta, cause = {@code ORDER_PAID}).
     *
     * @throws LoyaltyLedgerDomainException if {@code delta <= 0}.
     */
    public static Movement ofCredit(final CustomerId customerId,
                                    final OrderId originatingOrderId,
                                    final UUID originatingEventId,
                                    final String originatingEventType,
                                    final ZonedDateTime originatingEventReceivedAt,
                                    final int delta) {
        if (delta == 0) {
            throw new LoyaltyLedgerDomainException(
                    "Movement.delta must not be zero (REQ-002): zero-credit must be skipped before instantiation");
        }
        if (delta < 0) {
            throw new LoyaltyLedgerDomainException(
                    "Movement.ofCredit requires delta > 0 (cause ORDER_PAID); got delta=" + delta);
        }
        return new Movement(
                MovementId.random(),
                customerId,
                delta,
                BalanceUpdateCause.ORDER_PAID,
                originatingOrderId,
                originatingEventId,
                originatingEventType,
                originatingEventReceivedAt,
                ZonedDateTime.now(),
                0);
    }

    /**
     * Creates a debit movement (negative delta, cause ∈ {ORDER_CANCELLED, ORDER_REFUNDED}).
     *
     * @throws LoyaltyLedgerDomainException if {@code delta >= 0} or {@code cause == ORDER_PAID}.
     */
    public static Movement ofDebit(final CustomerId customerId,
                                   final OrderId originatingOrderId,
                                   final UUID originatingEventId,
                                   final String originatingEventType,
                                   final ZonedDateTime originatingEventReceivedAt,
                                   final int delta,
                                   final BalanceUpdateCause cause) {
        if (delta == 0) {
            throw new LoyaltyLedgerDomainException(
                    "Movement.delta must not be zero (REQ-002)");
        }
        if (delta > 0) {
            throw new LoyaltyLedgerDomainException(
                    "Movement.ofDebit requires delta < 0; got delta=" + delta);
        }
        if (cause == null || cause == BalanceUpdateCause.ORDER_PAID) {
            throw new LoyaltyLedgerDomainException(
                    "Movement.ofDebit requires cause ∈ {ORDER_CANCELLED, ORDER_REFUNDED}; got cause=" + cause);
        }
        return new Movement(
                MovementId.random(),
                customerId,
                delta,
                cause,
                originatingOrderId,
                originatingEventId,
                originatingEventType,
                originatingEventReceivedAt,
                ZonedDateTime.now(),
                0);
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public int getDelta() {
        return delta;
    }

    public BalanceUpdateCause getCause() {
        return cause;
    }

    public OrderId getOriginatingOrderId() {
        return originatingOrderId;
    }

    public UUID getOriginatingEventId() {
        return originatingEventId;
    }

    public String getOriginatingEventType() {
        return originatingEventType;
    }

    public ZonedDateTime getOriginatingEventReceivedAt() {
        return originatingEventReceivedAt;
    }

    public ZonedDateTime getAppendedAt() {
        return appendedAt;
    }

    public int getVersion() {
        return version;
    }
}
