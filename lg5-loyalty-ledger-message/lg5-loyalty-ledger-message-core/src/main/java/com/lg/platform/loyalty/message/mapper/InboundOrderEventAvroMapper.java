package com.lg.platform.loyalty.message.mapper;

import com.labs.lg.pentagon.common.domain.valueobject.Money;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand.OrderCancelledCommand;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand.OrderPaidCommand;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand.OrderRefundedCommand;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import com.lg.platform.loyalty.domain.valueobject.OrderId;
import com.lg.platform.order.kafka.avro.model.OrderCancelledAvroModel;
import com.lg.platform.order.kafka.avro.model.OrderPaidAvroModel;
import com.lg.platform.order.kafka.avro.model.OrderRefundedAvroModel;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Pure POJO mapper translating the three inbound Avro records into the
 * corresponding {@link LoyaltyLedgerCommand} input-port commands
 * (TASK-008).
 *
 * <p>Lives in {@code message-core} because it is a wire-translation
 * concern: it depends on Avro types (which {@code application-service}
 * must NOT) and on the input-port {@link LoyaltyLedgerCommand} (which
 * {@code message-model} must NOT). It carries NO Spring annotations
 * (RULE-005) — instantiation is the listener layer's responsibility
 * (TASK-009/010/010b instantiate it as a bean via {@code @Component}
 * on the listener and a constructor-injected {@code new
 * InboundOrderEventMapper()}-style bean, or — preferred — a
 * dedicated {@code @Configuration} class. The food-ordering reference
 * uses {@code @Component} on the mapper itself; we keep the mapper
 * annotation-free here to honor RULE-005 strictly and let the listener
 * config wire it.).
 *
 * <p>Conversions:
 * <ul>
 *   <li>{@code messageId} (UUID) → {@code eventId} on the command
 *       (this is the dedup key — same UUID a producer would replay).</li>
 *   <li>{@code customerId} / {@code orderId} (UUID) → {@link CustomerId}
 *       / {@link OrderId} value objects.</li>
 *   <li>{@code paidAmount} (BigDecimal, OrderPaid only) → {@link Money}
 *       (full precision preserved; floor-to-int-EUR is a
 *       <em>domain</em> decision performed by the handler via
 *       {@code LoyaltyPoints.floorEurosFrom(...)}).</li>
 *   <li>{@code createdAt} (Avro {@code timestamp-millis} → {@link Instant})
 *       → {@link ZonedDateTime} at UTC for the
 *       {@code originatingEventReceivedAt} audit field (REQ-014).
 *       The wire timestamp is the producer-side commit time;
 *       Postgres stores {@code timestamptz} so zone normalization
 *       happens at the DB layer.</li>
 * </ul>
 */
public final class InboundOrderEventAvroMapper {

    public OrderPaidCommand toCommand(final OrderPaidAvroModel avro) {
        return new OrderPaidCommand(
                avro.getMessageId(),
                new CustomerId(avro.getCustomerId()),
                new OrderId(avro.getOrderId()),
                toZonedDateTime(avro.getCreatedAt()),
                new Money(avro.getPaidAmount()));
    }

    public OrderCancelledCommand toCommand(final OrderCancelledAvroModel avro) {
        return new OrderCancelledCommand(
                avro.getMessageId(),
                new CustomerId(avro.getCustomerId()),
                new OrderId(avro.getOrderId()),
                toZonedDateTime(avro.getCreatedAt()));
    }

    public OrderRefundedCommand toCommand(final OrderRefundedAvroModel avro) {
        return new OrderRefundedCommand(
                avro.getMessageId(),
                new CustomerId(avro.getCustomerId()),
                new OrderId(avro.getOrderId()),
                toZonedDateTime(avro.getCreatedAt()));
    }

    private static ZonedDateTime toZonedDateTime(final Instant instant) {
        return instant == null ? null : instant.atZone(ZoneOffset.UTC);
    }
}
