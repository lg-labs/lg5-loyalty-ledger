package com.lg.platform.loyalty.application.ports.input;

import com.lg.platform.loyalty.domain.entity.CustomerBalance;
import com.lg.platform.loyalty.domain.entity.Movement;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;

import java.util.List;

/**
 * Read-side input port (driving side) of the application service.
 *
 * <p>Mirrors the {@link LoyaltyLedgerInputPort} write-side contract
 * but for queries. The REST controllers in the {@code api} module
 * depend on this interface (NOT on JPA repositories directly) — the
 * hexagonal boundary keeps the data-access internals invisible to
 * adapters in upper layers.
 *
 * <ul>
 *   <li>{@link #getBalance(CustomerId)} (REQ-009 / TASK-015) — current
 *       projection or {@link com.lg.platform.loyalty.application.exception.CustomerBalanceNotFoundException}.</li>
 *   <li>{@link #getMovementsPage(CustomerId, int, int)} (REQ-010 /
 *       TASK-016) — single page in reverse-chronological order
 *       ({@code appended_at DESC, id DESC}). Out-of-range pages
 *       return an empty list with the absolute total in
 *       {@link MovementsPage#totalElements()} — never throws.</li>
 * </ul>
 *
 * <p>Implementation: {@link com.lg.platform.loyalty.application.LoyaltyLedgerQueryServiceImpl}.
 */
public interface LoyaltyLedgerQueryService {

    CustomerBalance getBalance(CustomerId customerId);

    MovementsPage getMovementsPage(CustomerId customerId, int page, int size);

    /**
     * Single page of movements + paging metadata.
     *
     * <p>{@code totalElements} is the absolute count for the customer
     * (used by the controller to populate the response envelope and
     * lets clients detect an out-of-range page request without a
     * second round-trip).
     */
    record MovementsPage(List<Movement> movements,
                         int page,
                         int size,
                         long totalElements) {
    }
}
