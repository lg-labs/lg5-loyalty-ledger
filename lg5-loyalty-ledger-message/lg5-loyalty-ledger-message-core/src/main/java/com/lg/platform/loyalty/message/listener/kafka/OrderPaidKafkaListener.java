package com.lg.platform.loyalty.message.listener.kafka;

import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerInputPort;
import com.lg.platform.loyalty.message.mapper.InboundOrderEventAvroMapper;
import com.lg.platform.order.kafka.avro.model.OrderPaidAvroModel;
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
 * Batch consumer for the {@code order-paid} topic (REQ-001).
 *
 * <p>Per-message contract:
 * <ul>
 *   <li>Maps the inbound Avro record to a {@link
 *       com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand
 *       OrderPaidCommand} via the {@link InboundOrderEventAvroMapper} (RULE-005:
 *       mapper class carries no Spring annotations).</li>
 *   <li>Invokes the application-service input port exactly once per message.</li>
 *   <li>Catches {@link OptimisticLockingFailureException} (concurrent update
 *       on the same balance row, RULE-010) and {@link
 *       DataIntegrityViolationException} (replay caught by the dedup unique
 *       index, ADR-003) and logs them at {@code DEBUG} as NO-OP without
 *       rethrowing — rethrowing would push the offset back and cause Kafka
 *       to redeliver the same batch indefinitely.</li>
 * </ul>
 *
 * <p>{@code batch-listener: true} is enabled in {@code
 * kafka-consumer-config.batch-listener=true} (application.yaml); the
 * framework's {@code KafkaConsumerConfig} already wires the listener
 * container factory under the bean name {@code
 * kafkaListenerContainerFactory} which is the Spring Kafka default so it
 * is selected implicitly.
 */
@Slf4j
@Component
public class OrderPaidKafkaListener implements KafkaConsumer<OrderPaidAvroModel> {

    private final LoyaltyLedgerInputPort loyaltyLedgerInputPort;
    private final InboundOrderEventAvroMapper mapper;

    public OrderPaidKafkaListener(final LoyaltyLedgerInputPort loyaltyLedgerInputPort,
                                  final InboundOrderEventAvroMapper mapper) {
        this.loyaltyLedgerInputPort = loyaltyLedgerInputPort;
        this.mapper = mapper;
    }

    @Override
    @KafkaListener(
            id = "${loyalty-ledger-service.consumer-groups.order-paid}",
            topics = "${loyalty-ledger-service.topics.inbound.order-paid}"
    )
    public void receive(@Payload final List<OrderPaidAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) final List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) final List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) final List<Long> offsets) {
        log.info("Received {} OrderPaid messages, keys={}, partitions={}, offsets={}",
                messages.size(), keys, partitions, offsets);

        for (final OrderPaidAvroModel avro : messages) {
            try {
                loyaltyLedgerInputPort.process(mapper.toCommand(avro));
            } catch (final OptimisticLockingFailureException e) {
                // Another thread already committed the balance update for this
                // customer/order; safe to skip (RULE-010).
                log.debug("Optimistic lock on OrderPaid orderId={} eventId={} — NO-OP",
                        avro.getOrderId(), avro.getMessageId());
            } catch (final DataIntegrityViolationException e) {
                // Dedup unique index (uq_processed_event_type_id) caught a
                // replay; the first delivery already produced the movement
                // (ADR-003).
                log.debug("Replay of OrderPaid eventId={} caught by dedup index — NO-OP",
                        avro.getMessageId());
            }
        }
    }
}
