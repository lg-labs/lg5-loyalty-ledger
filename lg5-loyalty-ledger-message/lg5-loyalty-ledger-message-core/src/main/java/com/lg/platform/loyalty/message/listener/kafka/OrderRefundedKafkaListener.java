package com.lg.platform.loyalty.message.listener.kafka;

import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerInputPort;
import com.lg.platform.loyalty.message.mapper.InboundOrderEventAvroMapper;
import com.lg.platform.order.kafka.avro.model.OrderRefundedAvroModel;
import com.lg5.spring.kafka.consumer.KafkaConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Batch consumer for the {@code order-refunded} topic (REQ-004, REQ-005).
 *
 * <p>Mirrors {@link OrderPaidKafkaListener} and {@link
 * OrderCancelledKafkaListener} structurally — same NO-OP swallow
 * contract for {@link OptimisticLockingFailureException} (RULE-010)
 * and {@link DataIntegrityViolationException} (ADR-003 dedup index
 * caught a replay). Differs only in the inbound Avro type and the
 * topic / consumer-group property keys.
 */
@Slf4j
@Component
public class OrderRefundedKafkaListener implements KafkaConsumer<OrderRefundedAvroModel> {

    private final LoyaltyLedgerInputPort loyaltyLedgerInputPort;
    private final InboundOrderEventAvroMapper mapper;

    public OrderRefundedKafkaListener(final LoyaltyLedgerInputPort loyaltyLedgerInputPort,
                                      final InboundOrderEventAvroMapper mapper) {
        this.loyaltyLedgerInputPort = loyaltyLedgerInputPort;
        this.mapper = mapper;
    }

    @Override
    @KafkaListener(
            id = "${loyalty-ledger-service.consumer-groups.order-refunded}",
            topics = "${loyalty-ledger-service.topics.inbound.order-refunded}"
    )
    public void receive(@Payload final List<OrderRefundedAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) final List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) final List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) final List<Long> offsets) {
        log.info("Received {} OrderRefunded messages, keys={}, partitions={}, offsets={}",
                messages.size(), keys, partitions, offsets);

        for (final OrderRefundedAvroModel avro : messages) {
            try {
                loyaltyLedgerInputPort.process(mapper.toCommand(avro));
            } catch (final OptimisticLockingFailureException e) {
                log.debug("Optimistic lock on OrderRefunded orderId={} eventId={} — NO-OP",
                        avro.getOrderId(), avro.getMessageId());
            } catch (final DataIntegrityViolationException e) {
                log.debug("Replay of OrderRefunded eventId={} caught by dedup index — NO-OP",
                        avro.getMessageId());
            }
        }
    }
}
