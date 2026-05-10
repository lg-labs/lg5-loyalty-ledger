package com.lg.platform.loyalty.message.publisher.kafka;

import com.lg.platform.loyalty.application.outbox.model.OutboxMessage;
import com.lg.platform.loyalty.application.outbox.payload.CustomerBalanceUpdatedEventPayload;
import com.lg.platform.loyalty.application.ports.output.message.publisher.CustomerBalanceUpdatedMessagePublisher;
import com.lg.platform.loyalty.kafka.avro.model.CustomerBalanceUpdatedAvroModel;
import com.lg.platform.loyalty.message.mapper.OutboundCustomerBalanceUpdatedAvroMapper;
import com.lg5.spring.kafka.producer.KafkaMessageHelper;
import com.lg5.spring.kafka.producer.service.KafkaProducer;
import com.lg5.spring.outbox.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

/**
 * Kafka adapter for the
 * {@link CustomerBalanceUpdatedMessagePublisher} output port
 * (TASK-013). Consumes an {@link OutboxMessage} (JSON payload),
 * deserialises it to {@link CustomerBalanceUpdatedEventPayload},
 * maps to {@link CustomerBalanceUpdatedAvroModel} via the TASK-012
 * mapper (using the outbox-row UUID as the Avro {@code messageId}
 * for replay-stable dedup), and ships it through the framework's
 * generic {@link KafkaProducer} with {@code customerId} as the
 * partition key (RULE-018: per-customer ordering).
 *
 * <p>The send-result callback is supplied by the framework's
 * {@link KafkaMessageHelper#getKafkaCallback}: on success it invokes
 * {@code outboxCallback.accept(message, COMPLETED)}, on error it
 * invokes {@code outboxCallback.accept(message, FAILED)}. The
 * scheduler binds {@code outboxCallback} to
 * {@code OutboxSchedulerHelper::updateOutboxMessage}, which performs
 * the {@code markCompleted}/{@code markFailed} repository call in a
 * fresh transaction (the producer thread runs outside the
 * scheduler's @Transactional boundary).
 */
@Slf4j
@Component
public class CustomerBalanceUpdatedKafkaPublisher implements CustomerBalanceUpdatedMessagePublisher {

    private final OutboundCustomerBalanceUpdatedAvroMapper mapper;
    private final KafkaProducer<String, CustomerBalanceUpdatedAvroModel> kafkaProducer;
    private final KafkaMessageHelper kafkaMessageHelper;
    private final String topicName;

    public CustomerBalanceUpdatedKafkaPublisher(
            final OutboundCustomerBalanceUpdatedAvroMapper mapper,
            final KafkaProducer<String, CustomerBalanceUpdatedAvroModel> kafkaProducer,
            final KafkaMessageHelper kafkaMessageHelper,
            @Value("${loyalty-ledger-service.topics.outbound.customer-balance-updated}") final String topicName) {
        this.mapper = mapper;
        this.kafkaProducer = kafkaProducer;
        this.kafkaMessageHelper = kafkaMessageHelper;
        this.topicName = topicName;
    }

    @Override
    public void publish(final OutboxMessage outboxMessage,
                        final BiConsumer<OutboxMessage, OutboxStatus> outboxCallback) {

        final CustomerBalanceUpdatedEventPayload payload = kafkaMessageHelper.stringToObjectClass(
                outboxMessage.payload(), CustomerBalanceUpdatedEventPayload.class);

        try {
            final CustomerBalanceUpdatedAvroModel avro = mapper.toAvro(outboxMessage.id(), payload);
            final String key = payload.customerId().toString();

            kafkaProducer.send(
                    topicName,
                    key,
                    avro,
                    kafkaMessageHelper.getKafkaCallback(
                            topicName,
                            avro,
                            outboxMessage,
                            outboxCallback,
                            outboxMessage.id().toString()));

            log.info("Sent CustomerBalanceUpdatedAvroModel to topic={} key={} messageId={} sagaId={}",
                    topicName, key, outboxMessage.id(), outboxMessage.sagaId());
        } catch (final Exception e) {
            log.error("Error preparing CustomerBalanceUpdatedAvroModel for outbox={} sagaId={}: {}",
                    outboxMessage.id(), outboxMessage.sagaId(), e.getMessage(), e);
            // Synchronous failure (mapping / serialization). Mark the
            // outbox row FAILED via the same callback contract so the
            // scheduler does not loop on the row forever (replay still
            // possible: an operator can flip the status back to
            // STARTED after fixing the data).
            outboxCallback.accept(outboxMessage, OutboxStatus.FAILED);
        }
    }
}
