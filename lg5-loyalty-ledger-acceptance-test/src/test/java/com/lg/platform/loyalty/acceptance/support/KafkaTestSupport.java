package com.lg.platform.loyalty.acceptance.support;

import com.lg.platform.order.kafka.avro.model.OrderPaidAvroModel;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Confluent Avro Kafka producer adapter used exclusively by the
 * REQ-015 swallow scenario (TASK-019).
 *
 * <p>REQ-015 requires that an inbound replay of an
 * already-processed event id surfaces the
 * {@link org.springframework.dao.DataIntegrityViolationException}
 * raised by the {@code uq_processed_event_type_id} unique
 * constraint on {@code processed_input_event} — and that the
 * {@link com.lg.platform.loyalty.message.listener.kafka.OrderPaidKafkaListener}
 * SWALLOWS that exception (NO-OP DEBUG log) instead of rethrowing.
 * Rethrowing would push the offset back and cause Kafka to redeliver
 * the same batch indefinitely (RULE-010 + ADR-003).
 *
 * <p>Why a SEPARATE producer instead of going straight through
 * the input port:
 * <ul>
 *   <li>The dedup row is written by {@code LoyaltyLedgerHandler}
 *       in the SAME Postgres transaction as the movement +
 *       balance + outbox writes; calling
 *       {@code LoyaltyLedgerInputPort.process(...)} a second
 *       time with the same eventId would surface the
 *       {@code DataIntegrityViolationException} <em>to the
 *       caller</em> (us) — the swallow happens one layer further
 *       out, in the Kafka listener. To prove the listener
 *       swallows, we MUST re-enter through the listener.</li>
 *   <li>The pattern mirrors
 *       {@code container/.../OrderPaidKafkaListenerIT.newAvroProducer()}
 *       — same serializer, same {@code acks=all}, same property
 *       keys. Reusing the proven shape avoids a fresh debug
 *       loop.</li>
 * </ul>
 *
 * <p>The producer is a long-lived singleton (one per
 * Spring context, hence one per ATDD JVM): construction touches
 * Schema Registry to fetch / register schemas which is non-trivial.
 * {@link PreDestroy} closes it cleanly so the testcontainer Kafka
 * broker can shut down without dangling connections.
 */
@Slf4j
@Component
public class KafkaTestSupport {

    @Value("${kafka-config.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka-config.schema-registry-url}")
    private String schemaRegistryUrl;

    @Value("${loyalty-ledger-service.topics.inbound.order-paid}")
    private String orderPaidTopic;

    private volatile KafkaProducer<String, Object> producer;

    /**
     * Synchronously sends a single {@link OrderPaidAvroModel} to
     * the inbound {@code order-paid} topic and waits for the broker
     * ACK. Synchronous because the REQ-015 scenario asserts on
     * downstream side-effects that strictly happen-after the
     * broker has accepted the record.
     *
     * @param eventId    Avro {@code messageId} — also the inbound
     *                   {@code originatingEventId} the listener
     *                   maps into the dedup column. PASSING THE
     *                   SAME value twice is the whole point of
     *                   the REQ-015 scenario.
     * @param customerId Avro {@code customerId} (UUID).
     * @param orderId    Avro {@code orderId} (UUID).
     * @param amount     Decimal string fed into {@code paidAmount};
     *                   currency is implicitly EUR (the upstream
     *                   producer ships a {@code BigDecimal}, no
     *                   ISO code).
     */
    public void sendOrderPaid(final UUID eventId,
                              final UUID customerId,
                              final UUID orderId,
                              final String amount) {
        final OrderPaidAvroModel msg = OrderPaidAvroModel.newBuilder()
                .setMessageId(eventId)
                .setCustomerId(customerId)
                .setOrderId(orderId)
                .setPaidAmount(new BigDecimal(amount))
                .setCreatedAt(Instant.now())
                .build();
        try {
            producer().send(new ProducerRecord<>(orderPaidTopic, customerId.toString(), msg)).get();
            producer().flush();
            log.debug("Sent OrderPaid eventId={} customerId={} orderId={} amount={} to topic {}",
                    eventId, customerId, orderId, amount, orderPaidTopic);
        } catch (final InterruptedException | ExecutionException e) {
            // Producer hard-fail is a TEST INFRA failure, not a
            // domain failure — rethrow so the scenario fails fast
            // instead of letting Awaitility waste 60s polling.
            throw new IllegalStateException("Failed to send Avro OrderPaid", e);
        }
    }

    private KafkaProducer<String, Object> producer() {
        if (producer == null) {
            synchronized (this) {
                if (producer == null) {
                    final Properties props = new Properties();
                    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
                    props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
                    props.put(ProducerConfig.ACKS_CONFIG, "all");
                    producer = new KafkaProducer<>(props);
                }
            }
        }
        return producer;
    }

    @PreDestroy
    void close() {
        if (producer != null) {
            producer.close();
        }
    }
}
