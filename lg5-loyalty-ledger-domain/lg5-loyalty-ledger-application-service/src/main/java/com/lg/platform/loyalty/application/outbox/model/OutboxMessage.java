package com.lg.platform.loyalty.application.outbox.model;

import com.lg5.spring.outbox.OutboxStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Application-service-side representation of one row in
 * {@code loyalty.outbox} (data-model.md §outbox / RULE-008).
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code sagaId}: reused as a <strong>correlation id</strong> per
 *       ADR-001 RULE-007 clarification — set to the originating event
 *       id so an outbox row can be traced back to its source.</li>
 *   <li>{@code type}: constant {@code "CustomerBalanceUpdated"} for v1.</li>
 *   <li>{@code payload}: serialized JSON (jsonb at the DB layer)
 *       carrying the {@code CustomerBalanceUpdatedEventPayload}.</li>
 * </ul>
 */
public record OutboxMessage(
        UUID id,
        UUID sagaId,
        String type,
        String payload,
        OutboxStatus status,
        ZonedDateTime createdAt,
        int version) {

    /** Convenience factory for a freshly-appended STARTED row. */
    public static OutboxMessage started(final UUID sagaId,
                                        final String type,
                                        final String payload) {
        return new OutboxMessage(
                UUID.randomUUID(),
                sagaId,
                type,
                payload,
                OutboxStatus.STARTED,
                ZonedDateTime.now(),
                0);
    }
}
