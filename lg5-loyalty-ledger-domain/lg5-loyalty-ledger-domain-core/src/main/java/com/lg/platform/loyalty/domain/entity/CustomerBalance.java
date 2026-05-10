package com.lg.platform.loyalty.domain.entity;

import com.labs.lg.pentagon.common.domain.entity.AggregateRoot;
import com.lg.platform.loyalty.domain.exception.LoyaltyLedgerDomainException;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;

import java.time.ZonedDateTime;

/**
 * Materialized projection of one customer's current point balance (ADR-004).
 * <p>
 * One instance per customer; created on first credit/debit, never deleted.
 * The balance may be negative (REQ-007, REQ-008).
 */
public class CustomerBalance extends AggregateRoot<CustomerId> {

    private long balance;
    private ZonedDateTime lastUpdatedAt;
    private int version;

    private CustomerBalance(final CustomerId customerId,
                            final long balance,
                            final ZonedDateTime lastUpdatedAt,
                            final int version) {
        super.setId(customerId);
        this.balance = balance;
        this.lastUpdatedAt = lastUpdatedAt;
        this.version = version;
    }

    /** Initial state for a customer first seen. */
    public static CustomerBalance empty(final CustomerId customerId) {
        return new CustomerBalance(customerId, 0L, ZonedDateTime.now(), 0);
    }

    /**
     * Adds {@code delta} to the balance and refreshes {@code lastUpdatedAt}.
     * <p>The result may be negative (REQ-007).
     *
     * <p>Intentionally does NOT mutate {@code version}: that field is a
     * passive optimistic-locking token whose lifecycle is owned end-to-end
     * by Hibernate's {@code @Version} on the JPA entity. Bumping it here
     * would conflict with Hibernate's WHERE-clause check on every UPDATE
     * and break optimistic locking. (Pattern matches
     * {@code food-ordering-system}; RULE-008 requires <em>exposing</em>
     * a {@code version} field, not incrementing it in the domain.)
     *
     * @throws LoyaltyLedgerDomainException if {@code delta == 0}.
     */
    public void applyDelta(final int delta) {
        if (delta == 0) {
            throw new LoyaltyLedgerDomainException(
                    "CustomerBalance.applyDelta requires delta != 0");
        }
        this.balance += delta;
        this.lastUpdatedAt = ZonedDateTime.now();
    }

    public long getBalance() {
        return balance;
    }

    public ZonedDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public int getVersion() {
        return version;
    }
}
