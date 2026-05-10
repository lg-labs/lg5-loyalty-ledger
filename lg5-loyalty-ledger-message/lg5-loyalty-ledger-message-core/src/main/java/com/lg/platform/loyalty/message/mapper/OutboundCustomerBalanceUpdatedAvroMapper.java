package com.lg.platform.loyalty.message.mapper;

import com.lg.platform.loyalty.application.outbox.payload.CustomerBalanceUpdatedEventPayload;
import com.lg.platform.loyalty.kafka.avro.model.BalanceUpdateCause;
import com.lg.platform.loyalty.kafka.avro.model.CustomerBalanceUpdatedAvroModel;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Pure POJO mapper translating the JSON outbox payload
 * {@link CustomerBalanceUpdatedEventPayload} into the outbound Avro
 * record {@link CustomerBalanceUpdatedAvroModel} (TASK-012).
 *
 * <p>Lives in {@code message-core} alongside its inbound sibling
 * {@link InboundOrderEventAvroMapper} and follows the same RULE-005
 * contract: NO Spring annotations on the mapper class itself; the bean
 * is wired by {@link com.lg.platform.loyalty.message.config.MessagingBeansConfig}.
 *
 * <p>Conversion notes:
 * <ul>
 *   <li>{@code messageId} is supplied by the caller (the TASK-013
 *       outbox scheduler passes {@code OutboxMessage.id()}). Keeping
 *       the Avro {@code messageId} = outbox-row UUID gives downstream
 *       consumers a stable dedup key that survives republish on
 *       restart (a re-run of the scheduler against the same STARTED
 *       row produces an identical Kafka message — by design).</li>
 *   <li>{@code cause} (JSON String) → {@link BalanceUpdateCause} via
 *       {@code valueOf}, with a defensive fallback to
 *       {@link BalanceUpdateCause#UNKNOWN} on any unmapped symbol
 *       (ADR-005 forward-compat: producers may add new cause symbols
 *       in JSON before the Avro enum is grown; the schema's
 *       {@code default: UNKNOWN} mirrors this on the read path).
 *       A {@code null} cause also collapses to {@code UNKNOWN}.</li>
 *   <li>{@code occurredAt} ({@link ZonedDateTime}) → {@code Instant}
 *       (Avro {@code timestamp-millis}). The wire-side zone is lost
 *       by definition; UTC is the canonical Postgres + Kafka
 *       timeline.</li>
 *   <li>UUID-typed Avro fields with {@code logicalType: uuid} are
 *       represented as {@link UUID} on the generated builder when the
 *       Avro maven plugin's {@code stringType=String} is overridden
 *       — but the existing inbound mappers rely on
 *       {@code setMessageId(UUID)} working directly, confirming the
 *       generator produces {@link UUID}-typed setters here too.</li>
 * </ul>
 */
public final class OutboundCustomerBalanceUpdatedAvroMapper {

    public CustomerBalanceUpdatedAvroModel toAvro(
            final UUID messageId,
            final CustomerBalanceUpdatedEventPayload payload) {
        return CustomerBalanceUpdatedAvroModel.newBuilder()
                .setMessageId(messageId)
                .setCustomerId(payload.customerId())
                .setNewBalance(payload.newBalance())
                .setDelta(payload.delta())
                .setCause(toCause(payload.cause()))
                .setOriginatingOrderId(payload.originatingOrderId())
                .setOriginatingEventId(payload.originatingEventId())
                .setOriginatingEventType(payload.originatingEventType())
                .setOccurredAt(payload.occurredAt() == null ? null : payload.occurredAt().toInstant())
                .build();
    }

    private static BalanceUpdateCause toCause(final String symbol) {
        if (symbol == null) {
            return BalanceUpdateCause.UNKNOWN;
        }
        try {
            return BalanceUpdateCause.valueOf(symbol);
        } catch (final IllegalArgumentException ex) {
            return BalanceUpdateCause.UNKNOWN;
        }
    }
}
