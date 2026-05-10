package com.lg.platform.loyalty.container.message;

import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerInputPort;
import com.lg.platform.loyalty.container.boot.Bootstrap;
import com.lg.platform.order.kafka.avro.model.OrderPaidAvroModel;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
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
@TestPropertySource(properties = {
        "testcontainers.postgres.enabled=true",
        "testcontainers.kafka.enabled=true",
        "spring.datasource.url=",
        "spring.datasource.username=",
        "spring.datasource.password=",
        // Unique consumer group id so re-runs don't interfere with prior offsets.
        "loyalty-ledger-service.consumer-groups.order-paid=order-paid-listener-it"
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
        // Framework's ConfluentKafkaContainerCustomConfig sets two waitingFor()
        // strategies on the SR container, the second of which (forListeningPort)
        // overrides the first (forHttp). Result: the container is "ready" as
        // soon as the port is bound, but the SR app inside hasn't finished
        // initialising. Poll /subjects ourselves before producing.
        awaitSchemaRegistryReady();

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

    private void awaitSchemaRegistryReady() {
        final java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
        final java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(schemaRegistryUrl + "/subjects"))
                .timeout(java.time.Duration.ofSeconds(2))
                .GET()
                .build();
        await().atMost(60, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .ignoreExceptions()
                .until(() -> http.send(req, java.net.http.HttpResponse.BodyHandlers.discarding())
                        .statusCode() == 200);
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
