package com.lg.platform.loyalty.application;

import com.lg.platform.loyalty.application.exception.CustomerBalanceNotFoundException;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerQueryService;
import com.lg.platform.loyalty.application.ports.output.repository.CustomerBalanceRepository;
import com.lg.platform.loyalty.domain.entity.CustomerBalance;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public LoyaltyLedgerQueryServiceImpl(final CustomerBalanceRepository customerBalanceRepository) {
        this.customerBalanceRepository = customerBalanceRepository;
    }

    @Override
    public CustomerBalance getBalance(final CustomerId customerId) {
        return customerBalanceRepository.findById(customerId)
                .orElseThrow(() -> new CustomerBalanceNotFoundException(customerId));
    }
}
