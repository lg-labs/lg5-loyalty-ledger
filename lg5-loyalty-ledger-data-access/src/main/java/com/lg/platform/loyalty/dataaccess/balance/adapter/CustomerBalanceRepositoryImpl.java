package com.lg.platform.loyalty.dataaccess.balance.adapter;

import com.lg.platform.loyalty.application.ports.output.repository.CustomerBalanceRepository;
import com.lg.platform.loyalty.dataaccess.balance.mapper.CustomerBalanceDataAccessMapper;
import com.lg.platform.loyalty.dataaccess.balance.repository.CustomerBalanceJpaRepository;
import com.lg.platform.loyalty.domain.entity.CustomerBalance;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Secondary adapter implementing the {@link CustomerBalanceRepository}
 * output port.
 *
 * <p>Hibernate's {@code @Version} on {@link com.lg.platform.loyalty.dataaccess.balance.entity.CustomerBalanceJpaEntity}
 * is the single source of truth for the optimistic-locking token; the
 * domain {@code CustomerBalance.applyDelta(...)} intentionally does NOT
 * mutate {@code version} (see fix-up to TASK-003). The mapper performs
 * a straight 1:1 round-trip; on a stale write Hibernate raises
 * {@link org.springframework.dao.OptimisticLockingFailureException},
 * which the upstream Kafka listener will swallow as NO-OP per RULE-010.
 */
@Component
public class CustomerBalanceRepositoryImpl implements CustomerBalanceRepository {

    private final CustomerBalanceJpaRepository customerBalanceJpaRepository;
    private final CustomerBalanceDataAccessMapper customerBalanceDataAccessMapper;

    public CustomerBalanceRepositoryImpl(final CustomerBalanceJpaRepository customerBalanceJpaRepository,
                                         final CustomerBalanceDataAccessMapper customerBalanceDataAccessMapper) {
        this.customerBalanceJpaRepository = customerBalanceJpaRepository;
        this.customerBalanceDataAccessMapper = customerBalanceDataAccessMapper;
    }

    @Override
    public CustomerBalance save(final CustomerBalance customerBalance) {
        return customerBalanceDataAccessMapper.entityToDomain(
                customerBalanceJpaRepository.save(
                        customerBalanceDataAccessMapper.domainToEntity(customerBalance)));
    }

    @Override
    public Optional<CustomerBalance> findById(final CustomerId customerId) {
        return customerBalanceJpaRepository.findById(customerId.getValue())
                .map(customerBalanceDataAccessMapper::entityToDomain);
    }
}
