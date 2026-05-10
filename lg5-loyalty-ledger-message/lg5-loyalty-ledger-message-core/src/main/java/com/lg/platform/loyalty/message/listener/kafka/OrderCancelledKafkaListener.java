package com.lg.platform.loyalty.message.listener.kafka;

import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerInputPort;
import com.lg.platform.loyalty.message.mapper.InboundOrderEventAvroMapper;
import com.lg.platform.order.kafka.avro.model.OrderCancelledAvroModel;
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
 * Batch consumer for the {@code order-cancelled} topic (REQ-004, REQ-005).
 *
 * <p>Mirrors {@link OrderPaidKafkaListener} structurally — same NO-OP
 * swallow contract for {@link OptimisticLockingFailureException}
 * (RULE-010, two concurrent batches racing on the same balance row)
 * and {@link DataIntegrityViolationException} (ADR-003, dedup unique
 * index caught a replay). Differs only in the inbound Avro type and
 * the topic / consumer-group property keys.
 *
 * <p>{@code batch-listener: true} is enabled in {@code
 * kafka-consumer-config.batch-listener=true} (application.yaml); the
 * framework's {@code KafkaConsumerConfig} wires the listener container
 * factory under the bean name {@code kafkaListenerContainerFactory}
 * which is the Spring Kafka default and is therefore selected
 * implicitly.
 */
@Slf4j
@Component
public class OrderCancelledKafkaListener implements KafkaConsumer<OrderCancelledAvroModel> {

    private final LoyaltyLedgerInputPort loyaltyLedgerInputPort;
    private final InboundOrderEventAvroMapper mapper;

    public OrderCancelledKafkaListener(final LoyaltyLedgerInputPort loyaltyLedgerInputPort,
                                       final InboundOrderEventAvroMapper mapper) {
        this.loyaltyLedgerInputPort = loyaltyLedgerInputPort;
        this.mapper = mapper;
    }

    @Override
    @KafkaListener(
            id = "${loyalty-ledger-service.consumer-groups.order-cancelled}",
            topics = "${loyalty-ledger-service.topics.inbound.order-cancelled}"
    )
    public void receive(@Payload final List<OrderCancelledAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) final List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) final List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) final List<Long> offsets) {
        log.info("Received {} OrderCancelled messages, keys={}, partitions={}, offsets={}",
                messages.size(), keys, partitions, offsets);

        for (final OrderCancelledAvroModel avro : messages) {
            try {
                loyaltyLedgerInputPort.process(mapper.toCommand(avro));
            } catch (final OptimisticLockingFailureException e) {
                log.debug("Optimistic lock on OrderCancelled orderId={} eventId={} — NO-OP",
                        avro.getOrderId(), avro.getMessageId());
            } catch (final DataIntegrityViolationException e) {
                log.debug("Replay of OrderCancelled eventId={} caught by dedup index — NO-OP",
                        avro.getMessageId());
            }
        }
    }
}
