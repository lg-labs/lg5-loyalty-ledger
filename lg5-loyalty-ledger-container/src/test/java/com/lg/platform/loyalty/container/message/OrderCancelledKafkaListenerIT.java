package com.lg.platform.loyalty.container.message;

import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerInputPort;
import com.lg.platform.loyalty.boot.Bootstrap;
import com.lg.platform.order.kafka.avro.model.OrderCancelledAvroModel;
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

import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * IT for
 * {@link com.lg.platform.loyalty.message.listener.kafka.OrderCancelledKafkaListener}.
 *
 * <p>
 * Mirrors {@link OrderPaidKafkaListenerIT}: same Postgres + Kafka containers
 * (RULE-013 gates flipped via {@code @TestPropertySource}), same
 * {@code @MockitoBean} on the input port (real handler ships in TASK-011), same
 * single-message-then-verify-once shape. Differs only in the inbound Avro type
 * and topic property key.
 */
@Slf4j
@TestPropertySource(properties = {"testcontainers.kafka.enabled=true", "testcontainers.schema-registry.enabled=true", "scheduling.enabled=true", "loyalty-ledger-service.outbox-scheduler-fixed-rate=200",
		"loyalty-ledger-service.outbox-scheduler-initial-delay=200"
// See OrderPaidKafkaListenerIT for why no per-IT-unique
// properties: identical @TestPropertySource set across all
// kafka container ITs (listeners + TASK-013 publisher) ⇒
// shared Spring context ⇒ shared Testcontainers (no `kafka`
// network-alias collisions). Scheduler always-on is harmless
// here — input port is mocked, no outbox rows are written.
})

class OrderCancelledKafkaListenerIT extends Bootstrap {

	@Value("${kafka-config.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${kafka-config.schema-registry-url}")
	private String schemaRegistryUrl;

	@Value("${loyalty-ledger-service.topics.inbound.order-cancelled}")
	private String orderCancelledTopic;

	@MockitoBean
	private LoyaltyLedgerInputPort loyaltyLedgerInputPort;

	@Test
	void receive_invokes_input_port_exactly_once_with_mapped_command() throws Exception {
		final UUID eventId = UUID.randomUUID();
		final UUID customerId = UUID.randomUUID();
		final UUID orderId = UUID.randomUUID();
		final OrderCancelledAvroModel msg = OrderCancelledAvroModel.newBuilder().setMessageId(eventId)
				.setCustomerId(customerId).setOrderId(orderId).setCreatedAt(Instant.parse("2026-05-10T05:30:00Z"))
				.build();

		try (final KafkaProducer<String, Object> producer = this.newAvroProducer()) {
			producer.send(new ProducerRecord<>(this.orderCancelledTopic, customerId.toString(), msg)).get();
			producer.flush();
		}

		final ArgumentCaptor<LoyaltyLedgerCommand> captor = ArgumentCaptor.forClass(LoyaltyLedgerCommand.class);
		await().atMost(60, TimeUnit.SECONDS)
				.untilAsserted(() -> verify(this.loyaltyLedgerInputPort, times(1)).process(captor.capture()));

		final LoyaltyLedgerCommand cmd = captor.getValue();
		assertThat(cmd).isInstanceOf(LoyaltyLedgerCommand.OrderCancelledCommand.class);
		final LoyaltyLedgerCommand.OrderCancelledCommand oc = (LoyaltyLedgerCommand.OrderCancelledCommand) cmd;
		assertThat(oc.eventId()).isEqualTo(eventId);
		assertThat(oc.customerId().getValue()).isEqualTo(customerId);
		assertThat(oc.orderId().getValue()).isEqualTo(orderId);
		assertThat(oc.eventType()).isEqualTo("OrderCancelled");
	}

	private KafkaProducer<String, Object> newAvroProducer() {
		final Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
		props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, this.schemaRegistryUrl);
		props.put(ProducerConfig.ACKS_CONFIG, "all");
		return new KafkaProducer<>(props);
	}
}
