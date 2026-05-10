package com.lg.platform.loyalty.dataaccess.movement.repository;

import com.lg.platform.loyalty.dataaccess.movement.entity.MovementJpaEntity;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link MovementJpaEntity}.
 *
 * <p>Intentionally extends the bare {@link Repository} marker
 * (instead of {@code JpaRepository} or {@code CrudRepository}) so that
 * NO {@code update} or {@code delete} method is exposed by Spring Data
 * — the {@code movement} ledger is append-only (REQ-013, data-model.md
 * §Movement). Callers may only insert via {@link #save(MovementJpaEntity)}
 * and read via the explicitly declared finders below.
 */
public interface MovementJpaRepository extends Repository<MovementJpaEntity, UUID> {

    /** INSERT (or no-op if id already exists; ids come from the domain layer). */
    MovementJpaEntity save(MovementJpaEntity entity);

    Optional<MovementJpaEntity> findById(UUID id);

    /**
     * REQ-004: backs
     * {@link com.lg.platform.loyalty.application.ports.output.repository.MovementLedgerRepository#existsCreditFor}.
     */
    boolean existsByOriginatingOrderIdAndDeltaGreaterThan(UUID originatingOrderId, int deltaGreaterThan);
}
