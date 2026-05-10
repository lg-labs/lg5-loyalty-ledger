package com.lg.platform.loyalty.dataaccess.balance.repository;

import com.lg.platform.loyalty.dataaccess.balance.entity.CustomerBalanceJpaEntity;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link CustomerBalanceJpaEntity}.
 *
 * <p>Like {@code MovementJpaRepository}, this extends only the bare
 * {@link Repository} marker so that no {@code delete*} method is
 * generated — the projection is never deleted (data-model.md
 * §CustomerBalance). Updates ARE allowed (the projection is mutated by
 * {@code applyDelta}), and Spring Data's
 * {@code save(...)} performs INSERT-or-UPDATE based on whether the
 * primary key already exists.
 */
public interface CustomerBalanceJpaRepository extends Repository<CustomerBalanceJpaEntity, UUID> {

    CustomerBalanceJpaEntity save(CustomerBalanceJpaEntity entity);

    Optional<CustomerBalanceJpaEntity> findById(UUID id);
}
