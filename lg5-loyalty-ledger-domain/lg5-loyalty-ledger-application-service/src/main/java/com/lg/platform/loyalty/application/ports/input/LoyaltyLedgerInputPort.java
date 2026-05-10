package com.lg.platform.loyalty.application.ports.input;

/**
 * Input-port (driving side) of the application-service. Implemented
 * by the {@link com.lg.platform.loyalty.application.LoyaltyLedgerHandler}
 * (TASK-011) and consumed by the Kafka listener layer (TASK-009/010/010b).
 *
 * <p>The contract is intentionally minimal: a single
 * {@code process(LoyaltyLedgerCommand)} entry point. The handler is
 * responsible for the entire dedup-gate → movement-append →
 * balance-update → outbox-append sequence under one
 * {@code @Transactional} boundary (data-model.md §Idempotency strategy).
 */
public interface LoyaltyLedgerInputPort {
    void process(LoyaltyLedgerCommand command);
}
