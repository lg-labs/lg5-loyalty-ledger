package com.lg.platform.loyalty.message.mapper;

import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand.OrderCancelledCommand;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand.OrderPaidCommand;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand.OrderRefundedCommand;
import com.lg.platform.order.kafka.avro.model.OrderCancelledAvroModel;
import com.lg.platform.order.kafka.avro.model.OrderPaidAvroModel;
import com.lg.platform.order.kafka.avro.model.OrderRefundedAvroModel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InboundOrderEventAvroMapper} (TASK-008).
 *
 * <p>Asserts the three field-by-field translations + that the
 * {@code paidAmount} survives the mapper without any premature floor
 * (the floor-to-int-EUR decision is a domain rule, not a wire one).
 */
class InboundOrderEventAvroMapperTest {

    private final InboundOrderEventAvroMapper mapper = new InboundOrderEventAvroMapper();

    @Test
    void OrderPaid_avro_maps_to_OrderPaidCommand_preserving_decimal_precision() {
        final UUID messageId = UUID.randomUUID();
        final UUID customerId = UUID.randomUUID();
        final UUID orderId = UUID.randomUUID();
        final BigDecimal paidAmount = new BigDecimal("12.95");
        final Instant createdAt = Instant.parse("2026-05-10T10:15:30Z");

        final OrderPaidAvroModel avro = new OrderPaidAvroModel(
                messageId, customerId, orderId, paidAmount, createdAt);

        final OrderPaidCommand cmd = mapper.toCommand(avro);

        assertThat(cmd.eventId()).isEqualTo(messageId);
        assertThat(cmd.eventType()).isEqualTo("OrderPaid");
        assertThat(cmd.customerId().getValue()).isEqualTo(customerId);
        assertThat(cmd.orderId().getValue()).isEqualTo(orderId);
        // Wire side keeps the full 2-decimal precision; the floor-to-int
        // EUR rule fires later, in the handler (LoyaltyPoints).
        assertThat(cmd.paidAmount().getAmount()).isEqualByComparingTo("12.95");
        assertThat(cmd.eventReceivedAt().toInstant()).isEqualTo(createdAt);
    }

    @Test
    void OrderCancelled_avro_maps_to_OrderCancelledCommand() {
        final UUID messageId = UUID.randomUUID();
        final UUID customerId = UUID.randomUUID();
        final UUID orderId = UUID.randomUUID();
        final Instant createdAt = Instant.parse("2026-05-10T11:00:00Z");

        final OrderCancelledAvroModel avro = new OrderCancelledAvroModel(
                messageId, customerId, orderId, createdAt);

        final OrderCancelledCommand cmd = mapper.toCommand(avro);

        assertThat(cmd.eventId()).isEqualTo(messageId);
        assertThat(cmd.eventType()).isEqualTo("OrderCancelled");
        assertThat(cmd.customerId().getValue()).isEqualTo(customerId);
        assertThat(cmd.orderId().getValue()).isEqualTo(orderId);
        assertThat(cmd.eventReceivedAt().toInstant()).isEqualTo(createdAt);
    }

    @Test
    void OrderRefunded_avro_maps_to_OrderRefundedCommand() {
        final UUID messageId = UUID.randomUUID();
        final UUID customerId = UUID.randomUUID();
        final UUID orderId = UUID.randomUUID();
        final Instant createdAt = Instant.parse("2026-05-10T11:30:00Z");

        final OrderRefundedAvroModel avro = new OrderRefundedAvroModel(
                messageId, customerId, orderId, createdAt);

        final OrderRefundedCommand cmd = mapper.toCommand(avro);

        assertThat(cmd.eventId()).isEqualTo(messageId);
        assertThat(cmd.eventType()).isEqualTo("OrderRefunded");
        assertThat(cmd.customerId().getValue()).isEqualTo(customerId);
        assertThat(cmd.orderId().getValue()).isEqualTo(orderId);
        assertThat(cmd.eventReceivedAt().toInstant()).isEqualTo(createdAt);
    }

    @Test
    void mapper_is_free_of_Spring_annotations_RULE_005() {
        // Reflective check: no annotation in the mapper class belongs
        // to the org.springframework.* package.
        assertThat(InboundOrderEventAvroMapper.class.getAnnotations())
                .allSatisfy(a -> assertThat(a.annotationType().getPackageName())
                        .as("RULE-005: mapper must not carry Spring annotations")
                        .doesNotStartWith("org.springframework"));
        // And no field is Spring-injected either.
        for (final Field f : InboundOrderEventAvroMapper.class.getDeclaredFields()) {
            assertThat(f.getAnnotations())
                    .allSatisfy(a -> assertThat(a.annotationType().getPackageName())
                            .doesNotStartWith("org.springframework"));
        }
    }
}
