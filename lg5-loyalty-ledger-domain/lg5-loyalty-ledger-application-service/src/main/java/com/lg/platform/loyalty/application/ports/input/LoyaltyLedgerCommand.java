package com.lg.platform.loyalty.application.ports.input;

import com.labs.lg.pentagon.common.domain.valueobject.Money;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import com.lg.platform.loyalty.domain.valueobject.OrderId;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Input-port command (hexagonal) carrying one inbound business event
 * after it has been deserialized from Avro and translated into
 * domain-language by the message-core mapper (TASK-008).
 *
 * <p>The application-service handler (TASK-011) accepts one of the
 * three concrete subtypes via pattern matching. The
 * {@code eventType} constant is mirrored on the wire-side
 * {@code Movement.originatingEventType} (REQ-014 trace).
 *
 * <p>Sealed to keep the case-discrimination switch in the handler
 * exhaustive at compile time — adding a fourth inbound event would
 * fail to compile the handler until the new branch is implemented.
 */
public sealed interface LoyaltyLedgerCommand
        permits LoyaltyLedgerCommand.OrderPaidCommand,
                LoyaltyLedgerCommand.OrderCancelledCommand,
                LoyaltyLedgerCommand.OrderRefundedCommand {

    UUID eventId();

    String eventType();

    CustomerId customerId();

    OrderId orderId();

    ZonedDateTime eventReceivedAt();

    record OrderPaidCommand(
            UUID eventId,
            CustomerId customerId,
            OrderId orderId,
            ZonedDateTime eventReceivedAt,
            Money paidAmount) implements LoyaltyLedgerCommand {

        public static final String EVENT_TYPE = "OrderPaid";

        @Override
        public String eventType() {
            return EVENT_TYPE;
        }
    }

    record OrderCancelledCommand(
            UUID eventId,
            CustomerId customerId,
            OrderId orderId,
            ZonedDateTime eventReceivedAt) implements LoyaltyLedgerCommand {

        public static final String EVENT_TYPE = "OrderCancelled";

        @Override
        public String eventType() {
            return EVENT_TYPE;
        }
    }

    record OrderRefundedCommand(
            UUID eventId,
            CustomerId customerId,
            OrderId orderId,
            ZonedDateTime eventReceivedAt) implements LoyaltyLedgerCommand {

        public static final String EVENT_TYPE = "OrderRefunded";

        @Override
        public String eventType() {
            return EVENT_TYPE;
        }
    }
}
