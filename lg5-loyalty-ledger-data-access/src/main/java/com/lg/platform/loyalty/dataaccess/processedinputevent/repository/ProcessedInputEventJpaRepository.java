package com.lg.platform.loyalty.dataaccess.processedinputevent.repository;

import com.lg.platform.loyalty.dataaccess.processedinputevent.entity.ProcessedInputEventJpaEntity;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for {@link ProcessedInputEventJpaEntity}.
 * Bare {@link Repository} marker so no {@code delete*}/{@code update*}
 * methods are auto-generated — these rows are insert-only audit
 * records (REQ-013 spirit; no business reason to delete).
 */
public interface ProcessedInputEventJpaRepository
        extends Repository<ProcessedInputEventJpaEntity, UUID> {

    ProcessedInputEventJpaEntity save(ProcessedInputEventJpaEntity entity);

    Optional<ProcessedInputEventJpaEntity> findById(UUID id);
}
