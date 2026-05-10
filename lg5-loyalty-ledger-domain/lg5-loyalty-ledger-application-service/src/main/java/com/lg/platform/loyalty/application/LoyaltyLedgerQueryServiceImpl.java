package com.lg.platform.loyalty.application;

import com.lg.platform.loyalty.application.exception.CustomerBalanceNotFoundException;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerQueryService;
import com.lg.platform.loyalty.application.ports.output.repository.CustomerBalanceRepository;
import com.lg.platform.loyalty.application.ports.output.repository.MovementLedgerRepository;
import com.lg.platform.loyalty.domain.entity.CustomerBalance;
import com.lg.platform.loyalty.domain.entity.Movement;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only implementation of the
 * {@link LoyaltyLedgerQueryService} input port.
 *
 * <p>Annotated {@code @Transactional(readOnly = true)} at the class
 * level — Hibernate uses this as a hint to skip the dirty-checking
 * pass on session flush, which matters because the read endpoint
 * traffic is the dominant load shape for a balance projection.
 * REQ-013 append-only surface is preserved: this class never invokes
 * any {@code save}/{@code delete} on the underlying repositories.
 */
@Service
@Transactional(readOnly = true)
public class LoyaltyLedgerQueryServiceImpl implements LoyaltyLedgerQueryService {

    private final CustomerBalanceRepository customerBalanceRepository;
    private final MovementLedgerRepository movementLedgerRepository;

    public LoyaltyLedgerQueryServiceImpl(final CustomerBalanceRepository customerBalanceRepository,
                                         final MovementLedgerRepository movementLedgerRepository) {
        this.customerBalanceRepository = customerBalanceRepository;
        this.movementLedgerRepository = movementLedgerRepository;
    }

    @Override
    public CustomerBalance getBalance(final CustomerId customerId) {
        return customerBalanceRepository.findById(customerId)
                .orElseThrow(() -> new CustomerBalanceNotFoundException(customerId));
    }

    /**
     * REQ-010 / TASK-016 — paged movements. Out-of-range page is NOT
     * an error: returns an empty list with the absolute total. The
     * customer-not-found case is intentionally indistinguishable
     * from the customer-with-zero-movements case (both yield
     * {@code totalElements=0}); the spec text in TASK-016 calls for
     * an empty page (200 OK), not a 404, in line with REST best
     * practice for collection endpoints.
     */
    @Override
    public MovementsPage getMovementsPage(final CustomerId customerId, final int page, final int size) {
        final List<Movement> movements = movementLedgerRepository.findPageByCustomer(customerId, page, size);
        final long total = movementLedgerRepository.countByCustomer(customerId);
        return new MovementsPage(movements, page, size, total);
    }
}
