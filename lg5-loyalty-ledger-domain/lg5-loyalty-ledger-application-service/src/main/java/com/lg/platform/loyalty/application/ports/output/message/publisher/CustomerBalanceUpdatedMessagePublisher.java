package com.lg.platform.loyalty.application.ports.output.message.publisher;

import com.lg.platform.loyalty.application.outbox.model.OutboxMessage;
import com.lg5.spring.outbox.OutboxStatus;

import java.util.function.BiConsumer;

/**
 * Output port owned by the application-service for publishing one
 * outbox row to the message bus (TASK-013).
 *
 * <p>The adapter implementation lives in {@code message-core}
 * ({@code CustomerBalanceUpdatedKafkaPublisher}) and is wired by the
 * scheduler ({@code CustomerBalanceUpdatedOutboxScheduler}). The
 * scheduler passes a callback that the adapter invokes after the
 * Kafka send-result is observed: {@code COMPLETED} on success,
 * {@code FAILED} on a delivery error. The scheduler's callback
 * applies the corresponding {@code outboxRepository.markCompleted /
 * markFailed} update.
 *
 * <p>This port is generic in shape but currently has only one
 * concrete event ({@code CustomerBalanceUpdated}), so the name
 * encodes that. Future event types (PRD §Out-of-scope) would either
 * widen this port to a {@code DomainEventMessagePublisher} keyed by
 * {@code outboxMessage.type()} or introduce a sibling port.
 */
public interface CustomerBalanceUpdatedMessagePublisher {

    void publish(OutboxMessage outboxMessage,
                 BiConsumer<OutboxMessage, OutboxStatus> outboxCallback);
}
