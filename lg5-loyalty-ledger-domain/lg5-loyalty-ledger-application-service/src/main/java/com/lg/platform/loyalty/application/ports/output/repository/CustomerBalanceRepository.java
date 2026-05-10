package com.lg.platform.loyalty.application.ports.output.repository;

import com.lg.platform.loyalty.domain.entity.CustomerBalance;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;

import java.util.Optional;

/**
 * Output port (hexagonal) for the {@code customer_balance} projection
 * (ADR-004; data-model.md §CustomerBalance).
 *
 * <p>One row per customer, created lazily on first credit/debit, never
 * deleted. The balance MAY be negative (REQ-007). The implementing
 * adapter relies on JPA optimistic locking via the entity's
 * {@code @Version} field; an
 * {@link org.springframework.dao.OptimisticLockingFailureException}
 * raised on a stale write must be **swallowed by the caller** (the
 * Kafka listener layer, RULE-010), not by this port.
 */
public interface CustomerBalanceRepository {

    /**
     * UPSERT semantics: inserts a new row when the customer is unknown,
     * otherwise updates the existing row using the {@code @Version}
     * column for optimistic locking.
     */
    CustomerBalance save(CustomerBalance customerBalance);

    Optional<CustomerBalance> findById(CustomerId customerId);
}
