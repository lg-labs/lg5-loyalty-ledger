package com.lg.platform.loyalty.message.mapper;

import com.lg.platform.loyalty.application.outbox.payload.CustomerBalanceUpdatedEventPayload;
import com.lg.platform.loyalty.kafka.avro.model.BalanceUpdateCause;
import com.lg.platform.loyalty.kafka.avro.model.CustomerBalanceUpdatedAvroModel;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OutboundCustomerBalanceUpdatedAvroMapper}
 * (TASK-012).
 *
 * <p>Covers the field-by-field translation, the three known
 * {@link BalanceUpdateCause} symbols, the ADR-005 forward-compat
 * fallback to {@code UNKNOWN} for an unmapped JSON cause string, and
 * the {@link ZonedDateTime} → {@link java.time.Instant} normalization
 * (the wire-side zone is dropped; UTC is the canonical timeline).
 */
class OutboundCustomerBalanceUpdatedAvroMapperTest {

    private final OutboundCustomerBalanceUpdatedAvroMapper mapper =
            new OutboundCustomerBalanceUpdatedAvroMapper();

    @Test
    void maps_OrderPaid_payload_field_by_field() {
        final UUID messageId = UUID.randomUUID();
        final UUID customerId = UUID.randomUUID();
        final UUID orderId = UUID.randomUUID();
        final UUID eventId = UUID.randomUUID();
        final ZonedDateTime occurredAt = ZonedDateTime.parse("2026-05-10T05:30:00Z");

        final CustomerBalanceUpdatedEventPayload payload = new CustomerBalanceUpdatedEventPayload(
                customerId, 142L, 12, "ORDER_PAID",
                orderId, eventId, "OrderPaid", occurredAt);

        final CustomerBalanceUpdatedAvroModel avro = mapper.toAvro(messageId, payload);

        assertThat(avro.getMessageId()).isEqualTo(messageId);
        assertThat(avro.getCustomerId()).isEqualTo(customerId);
        assertThat(avro.getNewBalance()).isEqualTo(142L);
        assertThat(avro.getDelta()).isEqualTo(12);
        assertThat(avro.getCause()).isEqualTo(BalanceUpdateCause.ORDER_PAID);
        assertThat(avro.getOriginatingOrderId()).isEqualTo(orderId);
        assertThat(avro.getOriginatingEventId()).isEqualTo(eventId);
        assertThat(avro.getOriginatingEventType()).isEqualTo("OrderPaid");
        assertThat(avro.getOccurredAt()).isEqualTo(occurredAt.toInstant());
    }

    @Test
    void maps_OrderCancelled_cause_symbol_and_negative_delta() {
        final CustomerBalanceUpdatedEventPayload payload = newPayload("ORDER_CANCELLED", -8, 0L);
        final CustomerBalanceUpdatedAvroModel avro = mapper.toAvro(UUID.randomUUID(), payload);
        assertThat(avro.getCause()).isEqualTo(BalanceUpdateCause.ORDER_CANCELLED);
        assertThat(avro.getDelta()).isEqualTo(-8);
        assertThat(avro.getNewBalance()).isZero();
    }

    @Test
    void maps_OrderRefunded_cause_symbol() {
        final CustomerBalanceUpdatedEventPayload payload = newPayload("ORDER_REFUNDED", -5, 7L);
        final CustomerBalanceUpdatedAvroModel avro = mapper.toAvro(UUID.randomUUID(), payload);
        assertThat(avro.getCause()).isEqualTo(BalanceUpdateCause.ORDER_REFUNDED);
    }

    @Test
    void unknown_cause_string_falls_back_to_UNKNOWN_enum() {
        final CustomerBalanceUpdatedEventPayload payload = newPayload("FUTURE_CAUSE_NOT_YET_IN_AVRO", 0, 0L);
        final CustomerBalanceUpdatedAvroModel avro = mapper.toAvro(UUID.randomUUID(), payload);
        assertThat(avro.getCause()).isEqualTo(BalanceUpdateCause.UNKNOWN);
    }

    @Test
    void null_cause_falls_back_to_UNKNOWN_enum() {
        final CustomerBalanceUpdatedEventPayload payload = newPayload(null, 0, 0L);
        final CustomerBalanceUpdatedAvroModel avro = mapper.toAvro(UUID.randomUUID(), payload);
        assertThat(avro.getCause()).isEqualTo(BalanceUpdateCause.UNKNOWN);
    }

    @Test
    void non_utc_zoned_occurredAt_is_normalized_to_Instant() {
        final ZonedDateTime nyc = ZonedDateTime.of(2026, 5, 10, 1, 30, 0, 0, ZoneOffset.ofHours(-4));
        final CustomerBalanceUpdatedEventPayload payload = new CustomerBalanceUpdatedEventPayload(
                UUID.randomUUID(), 1L, 1, "ORDER_PAID",
                UUID.randomUUID(), UUID.randomUUID(), "OrderPaid", nyc);

        final CustomerBalanceUpdatedAvroModel avro = mapper.toAvro(UUID.randomUUID(), payload);

        assertThat(avro.getOccurredAt()).isEqualTo(nyc.toInstant());
    }

    private static CustomerBalanceUpdatedEventPayload newPayload(
            final String cause, final int delta, final long newBalance) {
        return new CustomerBalanceUpdatedEventPayload(
                UUID.randomUUID(), newBalance, delta, cause,
                UUID.randomUUID(), UUID.randomUUID(), "OrderX",
                ZonedDateTime.parse("2026-05-10T05:30:00Z"));
    }
}
