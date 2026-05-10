package com.lg.platform.loyalty.dataaccess.movement.repository;

import com.lg.platform.loyalty.dataaccess.movement.entity.MovementJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * REQ-010 / TASK-016 — reverse-chronological page of movements for
     * one customer. The {@code Pageable} carries page index + size; the
     * derived-name suffix {@code OrderByAppendedAtDescIdDesc} pins the
     * sort to ({@code appended_at DESC, id DESC}) which is exactly the
     * shape of {@code idx_movement_customer_appended} (data-model.md
     * §Indexes), guaranteeing a stable order even when several
     * movements share the same {@code appended_at} (high-throughput
     * insert burst).
     *
     * <p>Returning {@link Page} (not {@link List}) lets the caller read
     * {@code totalElements} from the same round-trip: Spring Data
     * issues an additional {@code SELECT COUNT(*)} only when
     * {@link Page#getTotalElements()} is consumed, which the read
     * controller does to populate the response envelope.
     */
    Page<MovementJpaEntity> findByCustomerIdOrderByAppendedAtDescIdDesc(UUID customerId, Pageable pageable);

    /**
     * REQ-010 — absolute count for a customer. Spring Data autogenerates
     * the {@code SELECT COUNT(*)}; declared explicitly so callers do
     * not have to depend on {@link Page#getTotalElements()} when they
     * only need the total (e.g. tests that pre-seed N rows and want to
     * assert the seed succeeded).
     */
    long countByCustomerId(UUID customerId);
}
