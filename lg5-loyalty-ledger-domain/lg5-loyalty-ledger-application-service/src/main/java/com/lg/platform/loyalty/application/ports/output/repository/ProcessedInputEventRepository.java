package com.lg.platform.loyalty.application.ports.output.repository;

import com.lg.platform.loyalty.domain.entity.ProcessedInputEvent;
import com.lg.platform.loyalty.domain.valueobject.ProcessedInputEventId;

import java.util.Optional;

/**
 * Output port for the {@code processed_input_event} dedup + audit table
 * (ADR-003).
 *
 * <p>Insert-only: the unique constraint
 * {@code uq_processed_event_type_id} on
 * {@code (originating_event_type, originating_event_id)} is the
 * mechanism by which a duplicate inbound event is rejected at write
 * time with a {@link org.springframework.dao.DataIntegrityViolationException},
 * which the Kafka listener swallows as NO-OP (RULE-010 + ADR-003).
 */
public interface ProcessedInputEventRepository {

    ProcessedInputEvent save(ProcessedInputEvent processedInputEvent);

    Optional<ProcessedInputEvent> findById(ProcessedInputEventId id);
}
