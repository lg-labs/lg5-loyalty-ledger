package com.lg.platform.loyalty.application.ports.output.repository;

import com.lg.platform.loyalty.application.outbox.model.OutboxMessage;
import com.lg5.spring.outbox.OutboxStatus;

import java.util.List;
import java.util.UUID;

/**
 * Output port for the {@code loyalty.outbox} table (RULE-008,
 * data-model.md §outbox). Drives the Transactional Outbox pattern:
 * the inbound listener appends a {@code STARTED} row in the same
 * transaction as the {@code movement} / {@code customer_balance}
 * write; a separate scheduled component reads {@code STARTED} rows in
 * creation order, publishes them to Kafka, and updates the status to
 * {@code COMPLETED} (or {@code FAILED}).
 */
public interface OutboxRepository {

    OutboxMessage save(OutboxMessage outboxMessage);

    /**
     * Returns all rows with the given status in insertion order
     * (backed by {@code idx_outbox_status_created}). Used by the
     * outbox scheduler to fetch unpublished messages.
     */
    List<OutboxMessage> findAllByStatusOrderByCreatedAtAsc(OutboxStatus status);

    void deleteAllByStatus(OutboxStatus status);

    void markCompleted(UUID id);

    void markFailed(UUID id);
}
