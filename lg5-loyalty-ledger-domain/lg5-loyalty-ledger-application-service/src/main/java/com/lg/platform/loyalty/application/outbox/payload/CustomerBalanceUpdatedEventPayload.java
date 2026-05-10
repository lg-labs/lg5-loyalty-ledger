package com.lg.platform.loyalty.application.outbox.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lg.platform.loyalty.domain.event.CustomerBalanceUpdatedEvent;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * JSON wire shape stored in {@code outbox.payload} (jsonb) for a
 * {@code CustomerBalanceUpdated} domain event (data-model.md
 * §Outbox payloads / RULE-008).
 *
 * <p>Distinct from {@link CustomerBalanceUpdatedEvent} (the in-memory
 * domain event in {@code domain-core}) by design: RULE-008 mandates a
 * separate payload type so the wire shape can evolve independently
 * of the domain. The mapping is 1:1 in v1; the {@code cause} enum is
 * serialized as its symbol name (a plain {@link String}) so the
 * downstream Avro mapper (TASK-012) can absorb future enum-symbol
 * additions / removals without breaking the JSON round-trip.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerBalanceUpdatedEventPayload(
        UUID customerId,
        long newBalance,
        int delta,
        String cause,
        UUID originatingOrderId,
        UUID originatingEventId,
        String originatingEventType,
        ZonedDateTime occurredAt) {

    public static CustomerBalanceUpdatedEventPayload from(final CustomerBalanceUpdatedEvent event) {
        return new CustomerBalanceUpdatedEventPayload(
                event.customerId().getValue(),
                event.newBalance(),
                event.delta(),
                event.cause().name(),
                event.originatingOrderId().getValue(),
                event.originatingEventId(),
                event.originatingEventType(),
                event.occurredAt());
    }
}
