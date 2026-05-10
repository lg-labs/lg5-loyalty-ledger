package com.lg.platform.loyalty.dataaccess.movement.repository;

import com.lg.platform.loyalty.dataaccess.movement.entity.MovementJpaEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * REQ-004: backs
     * {@link com.lg.platform.loyalty.application.ports.output.repository.MovementLedgerRepository#sumPositiveDeltaForOrder}.
     * Uses {@code COALESCE(...,0)} so the absence of any positive
     * delta returns {@code 0} rather than {@code null}.
     */
    @Query("""
            SELECT COALESCE(SUM(m.delta), 0)
              FROM MovementJpaEntity m
             WHERE m.originatingOrderId = :orderId
               AND m.delta > 0
            """)
    int sumPositiveDeltaForOrder(@Param("orderId") UUID orderId);

    /**
     * SELECT-only convenience finder used by application-service ITs to
     * inspect the per-order movement chain (TASK-011 cases A/D/F/G).
     * Spring Data exposes this as a derived query; the append-only
     * surface contract (REQ-013) is preserved because no
     * {@code update}/{@code delete}/{@code remove} method is added.
     */
    List<MovementJpaEntity> findByOriginatingOrderIdOrderByAppendedAtAsc(UUID originatingOrderId);
}
