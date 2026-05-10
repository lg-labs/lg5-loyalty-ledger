package com.lg.platform.loyalty.dataaccess.outbox.repository;

import com.lg.platform.loyalty.dataaccess.outbox.entity.OutboxJpaEntity;
import com.lg5.spring.outbox.OutboxStatus;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link OutboxJpaEntity}.
 *
 * <p>Unlike the audit/ledger repos, the outbox table is mutated:
 * the scheduler bulk-deletes COMPLETED rows and updates STARTED →
 * COMPLETED/FAILED. We therefore expose {@code save(...)},
 * status-ordered finders (backed by {@code idx_outbox_status_created}),
 * and a {@code deleteAllByOutboxStatus} for the housekeeping path.
 * Even so we extend the bare {@link Repository} marker so we control
 * which mutating methods are visible — only the explicit ones below.
 */
public interface OutboxJpaRepository extends Repository<OutboxJpaEntity, UUID> {

    OutboxJpaEntity save(OutboxJpaEntity entity);

    Optional<OutboxJpaEntity> findById(UUID id);

    /** Backs the scheduler's "fetch unpublished, oldest first" query. */
    List<OutboxJpaEntity> findAllByOutboxStatusOrderByCreatedAtAsc(OutboxStatus status);

    void deleteAllByOutboxStatus(OutboxStatus status);
}
