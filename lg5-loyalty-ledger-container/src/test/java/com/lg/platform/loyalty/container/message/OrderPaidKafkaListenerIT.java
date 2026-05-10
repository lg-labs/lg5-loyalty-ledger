package com.lg.platform.loyalty.container.message;

import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerInputPort;
import com.lg.platform.loyalty.container.boot.Bootstrap;
import com.lg.platform.order.kafka.avro.model.OrderPaidAvroModel;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * IT for {@link com.lg.platform.loyalty.message.listener.kafka.OrderPaidKafkaListener}.
 *
 * <p>Brings up a Postgres testcontainer (full app context boots Liquibase
 * + JPA wiring) plus a Confluent Kafka + Schema Registry container pair
 * (RULE-013 gates flipped via {@code @TestPropertySource}). The
 * application-service input port is replaced with a Mockito bean
 * because the real handler ships in TASK-011.
 *
 * <p>The bootstrap-servers and schema-registry-url come from the
 * framework's {@code ConfluentKafkaContainerCustomConfig} which
 * publishes them into the Spring {@code Environment} as
 * {@code kafka-config.bootstrap-servers} and
 * {@code kafka-config.schema-registry-url} respectively.
 */
@Slf4j
@TestPropertySource(properties = {
        "testcontainers.postgres.enabled=true",
        "testcontainers.kafka.enabled=true",
        "spring.datasource.url=",
        "spring.datasource.username=",
        "spring.datasource.password="
        // NOTE: do NOT add per-IT-unique property overrides here. The
        // Spring TestContext cache key is derived from the full
        // @TestPropertySource set, so any IT-local property string would
        // force a fresh ApplicationContext (and a fresh testcontainer
        // network) per IT class — which collides with the previous
        // class's still-tearing-down `kafka` network alias and breaks
        // the SR container's KafkaStore boot (Connection reset →
        // /subjects 200 wait timeout). All Kafka listener ITs in this
        // package therefore declare the *same* property set so they
        // share one context and one container set across the whole
        // module's IT phase.
})
class OrderPaidKafkaListenerIT extends Bootstrap {

    @Value("${kafka-config.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka-config.schema-registry-url}")
    private String schemaRegistryUrl;

    @Value("${loyalty-ledger-service.topics.inbound.order-paid}")
    private String orderPaidTopic;

    @MockitoBean
    private LoyaltyLedgerInputPort loyaltyLedgerInputPort;

    @Test
    void receive_invokes_input_port_exactly_once_with_mapped_command() throws Exception {
        final UUID eventId = UUID.randomUUID();
        final UUID customerId = UUID.randomUUID();
        final UUID orderId = UUID.randomUUID();
        final OrderPaidAvroModel msg = OrderPaidAvroModel.newBuilder()
                .setMessageId(eventId)
                .setCustomerId(customerId)
                .setOrderId(orderId)
                .setPaidAmount(new BigDecimal("12.95"))
                .setCreatedAt(Instant.parse("2026-05-10T05:30:00Z"))
                .build();

        try (final KafkaProducer<String, Object> producer = newAvroProducer()) {
            producer.send(new ProducerRecord<>(orderPaidTopic, customerId.toString(), msg)).get();
            producer.flush();
        }

        final ArgumentCaptor<LoyaltyLedgerCommand> captor =
                ArgumentCaptor.forClass(LoyaltyLedgerCommand.class);
        await().atMost(60, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(loyaltyLedgerInputPort, times(1)).process(captor.capture()));

        final LoyaltyLedgerCommand cmd = captor.getValue();
        assertThat(cmd).isInstanceOf(LoyaltyLedgerCommand.OrderPaidCommand.class);
        final LoyaltyLedgerCommand.OrderPaidCommand op = (LoyaltyLedgerCommand.OrderPaidCommand) cmd;
        assertThat(op.eventId()).isEqualTo(eventId);
        assertThat(op.customerId().getValue()).isEqualTo(customerId);
        assertThat(op.orderId().getValue()).isEqualTo(orderId);
        assertThat(op.paidAmount().getAmount()).isEqualByComparingTo(new BigDecimal("12.95"));
        assertThat(op.eventType()).isEqualTo("OrderPaid");
    }

    private KafkaProducer<String, Object> newAvroProducer() {
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaProducer<>(props);
    }
}
