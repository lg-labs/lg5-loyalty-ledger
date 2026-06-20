package com.lg.platform.loyalty.container.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lg.platform.loyalty.application.outbox.model.OutboxMessage;
import com.lg.platform.loyalty.application.outbox.payload.CustomerBalanceUpdatedEventPayload;
import com.lg.platform.loyalty.application.outbox.scheduler.CustomerBalanceUpdatedOutboxScheduler;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerInputPort;
import com.lg.platform.loyalty.application.ports.output.repository.OutboxRepository;
import com.lg.platform.loyalty.boot.Bootstrap;
import com.lg.platform.loyalty.kafka.avro.model.BalanceUpdateCause;
import com.lg.platform.loyalty.kafka.avro.model.CustomerBalanceUpdatedAvroModel;
import com.lg5.spring.outbox.OutboxStatus;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * IT for
 * {@link com.lg.platform.loyalty.message.publisher.kafka.CustomerBalanceUpdatedKafkaPublisher}
 * driven by
 * {@link com.lg.platform.loyalty.application.outbox.scheduler.CustomerBalanceUpdatedOutboxScheduler}
 * (TASK-013).
 *
 * <p>
 * Arrange: insert a {@code STARTED} outbox row carrying a JSON
 * {@link CustomerBalanceUpdatedEventPayload}.
 *
 * <p>
 * Act: rely on the always-on test scheduler (200ms tick — overridden in the
 * shared {@code @TestPropertySource} block below) to pick the row up and
 * publish.
 *
 * <p>
 * Assert (1) a {@link CustomerBalanceUpdatedAvroModel} arrives on the outbound
 * topic with field-by-field 1:1 translation including the outbox-row UUID
 * flowing into the Avro {@code messageId}, and (2) the outbox row is marked
 * {@code COMPLETED} via the producer callback path.
 *
 * <p>
 * <b>Shared {@code @TestPropertySource} block.</b> All Kafka container ITs in
 * this package (this publisher IT plus the three listener ITs) declare the
 * <em>identical</em> property set so they share one Spring TestContext + one
 * set of testcontainers across the IT phase. The scheduler is left always-on at
 * 200ms in tests: harmless in listener ITs (the input port is
 * {@code @MockitoBean} → no outbox rows ever written → scheduler iterates an
 * empty list), required here. See OrderPaidKafkaListenerIT for the
 * network-alias collision history that motivates the byte-for-byte unification.
 */
@Slf4j
@TestPropertySource(properties = {"testcontainers.kafka.enabled=true", "testcontainers.schema-registry.enabled=true", "scheduling.enabled=true", "loyalty-ledger-service.outbox-scheduler-fixed-rate=600000",
		"loyalty-ledger-service.outbox-scheduler-initial-delay=600000"})
class CustomerBalanceUpdatedKafkaPublisherIT extends Bootstrap {

	@Value("${kafka-config.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${kafka-config.schema-registry-url}")
	private String schemaRegistryUrl;

	@Value("${loyalty-ledger-service.topics.outbound.customer-balance-updated}")
	private String outboundTopic;

	@Autowired
	private OutboxRepository outboxRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CustomerBalanceUpdatedOutboxScheduler outboxScheduler;

	/**
	 * Unused in this IT; declared solely to keep the Spring TestContext cache key
	 * byte-for-byte identical to the listener ITs in this package.
	 * {@code @MockitoBean} contributes to the cache key — without this declaration
	 * the publisher IT would trigger a fresh ApplicationContext (and a fresh set of
	 * testcontainers, with a colliding {@code kafka} network alias + an SR group
	 * with multiple members advertising the same URL). See OrderPaidKafkaListenerIT
	 * comment for the full history.
	 */
	@MockitoBean
	@SuppressWarnings("unused")
	private LoyaltyLedgerInputPort loyaltyLedgerInputPort;

	@BeforeEach
	void cleanOutbox() {
		// Guard against cross-test leakage from listener ITs (in case
		// a future change inadvertently writes outbox rows in a
		// listener IT — currently none do).
		this.outboxRepository.deleteAllByStatus(OutboxStatus.STARTED);
		this.outboxRepository.deleteAllByStatus(OutboxStatus.COMPLETED);
		this.outboxRepository.deleteAllByStatus(OutboxStatus.FAILED);
	}

	@AfterEach
	void resetOutbox() {
		this.outboxRepository.deleteAllByStatus(OutboxStatus.STARTED);
		this.outboxRepository.deleteAllByStatus(OutboxStatus.COMPLETED);
		this.outboxRepository.deleteAllByStatus(OutboxStatus.FAILED);
	}

	@Test
	void scheduler_picks_up_STARTED_row_publishes_avro_to_kafka_and_marks_COMPLETED() throws Exception {
		// ── Arrange ────────────────────────────────────────────────
		final UUID customerId = UUID.randomUUID();
		final UUID orderId = UUID.randomUUID();
		final UUID originatingEventId = UUID.randomUUID();
		final ZonedDateTime occurredAt = ZonedDateTime.parse("2026-05-10T05:30:00Z");

		final CustomerBalanceUpdatedEventPayload payload = new CustomerBalanceUpdatedEventPayload(customerId, 142L, 12,
				"ORDER_PAID", orderId, originatingEventId, "OrderPaid", occurredAt);

		final String json = this.objectMapper.writeValueAsString(payload);
		final OutboxMessage saved = this.outboxRepository
				.save(OutboxMessage.started(originatingEventId, "CustomerBalanceUpdated", json));

		// ── Subscribe BEFORE the scheduler may publish ─────────────
		// (auto-offset-reset=earliest still covers us if the publish
		// beats the subscribe, but starting before keeps polls cheap.)
		try (final KafkaConsumer<String, Object> consumer = this.newAvroConsumer()) {
			consumer.subscribe(List.of(this.outboundTopic));
			this.outboxScheduler.processOutboxMessage();

			// ── Assert (1): Avro arrives ───────────────────────────
			final CustomerBalanceUpdatedAvroModel record = await().atMost(60, TimeUnit.SECONDS)
					.pollInterval(Duration.ofMillis(250)).until(() -> this.pollOne(consumer), r -> r != null);

			assertThat(record.getMessageId()).isEqualTo(saved.id());
			assertThat(record.getCustomerId()).isEqualTo(customerId);
			assertThat(record.getNewBalance()).isEqualTo(142L);
			assertThat(record.getDelta()).isEqualTo(12);
			assertThat(record.getCause()).isEqualTo(BalanceUpdateCause.ORDER_PAID);
			assertThat(record.getOriginatingOrderId()).isEqualTo(orderId);
			assertThat(record.getOriginatingEventId()).isEqualTo(originatingEventId);
			assertThat(record.getOriginatingEventType()).isEqualTo("OrderPaid");
			assertThat(record.getOccurredAt()).isEqualTo(occurredAt.toInstant());
		}

		// ── Assert (2): outbox row is marked COMPLETED ─────────────
		await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
			final List<OutboxMessage> stillStarted = this.outboxRepository
					.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.STARTED);
			assertThat(stillStarted).as("outbox row should have transitioned out of STARTED")
					.extracting(OutboxMessage::id).doesNotContain(saved.id());
		});
	}

	private CustomerBalanceUpdatedAvroModel pollOne(final KafkaConsumer<String, Object> consumer) {
		final ConsumerRecords<String, Object> batch = consumer.poll(Duration.ofMillis(500));
		for (final ConsumerRecord<String, Object> r : batch) {
			if (r.value() instanceof CustomerBalanceUpdatedAvroModel avro) {
				return avro;
			}
		}
		return null;
	}

	private KafkaConsumer<String, Object> newAvroConsumer() {
		final Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-customer-balance-updated-" + UUID.randomUUID());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
		props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, this.schemaRegistryUrl);
		props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
		return new KafkaConsumer<>(props);
	}
}
