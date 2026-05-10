package com.lg.platform.loyalty.application.ports.input;

import com.lg.platform.loyalty.domain.entity.CustomerBalance;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;

/**
 * Read-side input port (driving side) of the application service.
 *
 * <p>Mirrors the {@link LoyaltyLedgerInputPort} write-side contract
 * but for queries. The REST controllers in the {@code api} module
 * depend on this interface (NOT on JPA repositories directly) — the
 * hexagonal boundary keeps the data-access internals invisible to
 * adapters in upper layers.
 *
 * <p>{@link #getBalance(CustomerId)} (REQ-009 / TASK-015) returns the
 * current materialised projection or throws
 * {@link com.lg.platform.loyalty.application.exception.CustomerBalanceNotFoundException}
 * when no row exists.
 *
 * <p>The interface is intentionally narrow at TASK-015; TASK-016 will
 * add a {@code getMovementsPage} method here when the read-side
 * pagination contract lands. Implementation:
 * {@link com.lg.platform.loyalty.application.LoyaltyLedgerQueryServiceImpl}.
 */
public interface LoyaltyLedgerQueryService {

    CustomerBalance getBalance(CustomerId customerId);
}
