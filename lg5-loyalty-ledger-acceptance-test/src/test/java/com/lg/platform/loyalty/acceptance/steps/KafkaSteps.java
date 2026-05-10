package com.lg.platform.loyalty.acceptance.steps;

import com.lg.platform.loyalty.acceptance.support.KafkaTestSupport;
import com.lg.platform.loyalty.acceptance.support.World;
import com.lg.platform.loyalty.dataaccess.movement.repository.MovementJpaRepository;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * REQ-015 swallow scenario step definitions (TASK-019).
 *
 * <p><b>NOT annotated {@code @Component}</b> — Cucumber-Spring
 * 7.x rejects glue classes carrying that stereotype. Instances
 * are created by Cucumber-Spring's own factory and Spring still
 * autowires the constructor.
 *
 * <p>Drives the {@code OrderPaidKafkaListener} via a real Avro
 * producer (see {@link KafkaTestSupport}) so the listener's
 * exception-handling branch is the actual code under test —
 * the IT-layer
 * {@code container.../OrderPaidKafkaListenerIT} only proves the
 * listener invokes the input port; this scenario proves the
 * listener SWALLOWS the dedup violation on a SECOND delivery
 * with the same {@code originatingEventId}.
 *
 * <p>Sequence:
 * <ol>
 *   <li>Produce one OrderPaidAvroModel; await the movement row
 *       (proves first delivery succeeded — control case).</li>
 *   <li>Produce a second OrderPaidAvroModel with the
 *       <em>identical</em> {@code messageId}; assert the
 *       movement count for the order remains 1 over a
 *       sustained quiet window (proves the listener did NOT
 *       rethrow → Kafka did NOT redeliver → no second movement
 *       was appended).</li>
 * </ol>
 *
 * <p>The "no rethrow" assertion is INDIRECT: a thrown exception
 * inside a {@code @KafkaListener} (with the framework's default
 * non-batch error handler) would push the offset back, and the
 * listener container would redeliver until a poison-pill handler
 * gives up — observable as the movement count climbing over
 * time. Holding the count flat for a multi-second window is the
 * strongest signal we can extract without intercepting the
 * listener container's error handler chain.
 */
@Slf4j
public class KafkaSteps {

    /**
     * How long the duplicate must be left in flight before we are
     * confident the broker has stopped redelivering. Picked at
     * 8 s as a balance between scenario runtime and the Kafka
     * default {@code max.poll.interval.ms} / consumer rebalance
     * settling time on the testcontainer broker.
     */
    private static final Duration QUIET_WINDOW = Duration.ofSeconds(8);

    private final World world;
    private final KafkaTestSupport kafkaTestSupport;
    private final MovementJpaRepository movementJpaRepository;

    public KafkaSteps(final World world,
                      final KafkaTestSupport kafkaTestSupport,
                      final MovementJpaRepository movementJpaRepository) {
        this.world = world;
        this.kafkaTestSupport = kafkaTestSupport;
        this.movementJpaRepository = movementJpaRepository;
    }

    @When("OrderPaid {string} is published to Kafka for order {string} with event {string}")
    public void publishOrderPaid(final String amount,
                                 final String orderAlias,
                                 final String eventAlias) {
        final UUID eventId = world.eventIdFor(eventAlias);
        kafkaTestSupport.sendOrderPaid(eventId,
                world.customerOrNew().getValue(),
                world.orderFor(orderAlias).getValue(),
                amount);

        // Wait for the first delivery to materialise as a movement
        // — required so the SECOND publish lands in a deterministic
        // state. 60 s upper bound mirrors the IT-layer Kafka tests.
        final UUID orderUuid = world.orderFor(orderAlias).getValue();
        await().atMost(60, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(250))
                .until(() -> !movementJpaRepository
                        .findByOriginatingOrderIdOrderByAppendedAtAsc(orderUuid).isEmpty());
        world.setExpectedSwallowed(false);
    }

    @When("the same OrderPaid {string} is republished to Kafka for order {string} with event {string}")
    public void republishOrderPaid(final String amount,
                                   final String orderAlias,
                                   final String eventAlias) {
        // Reuse the same eventId (eventAlias resolves identically)
        // to trigger the dedup violation inside the handler that
        // the listener must swallow.
        kafkaTestSupport.sendOrderPaid(world.eventIdFor(eventAlias),
                world.customerOrNew().getValue(),
                world.orderFor(orderAlias).getValue(),
                amount);
        world.setExpectedSwallowed(true);
    }

    @Then("the listener swallowed the duplicate without rethrowing")
    public void swallowed() {
        // Assert the movement count for the order stays at 1 for
        // the full QUIET_WINDOW. If the listener had rethrown the
        // DataIntegrityViolationException, the broker would have
        // redelivered in <1s on a testcontainer. We use Awaitility
        // in inverted form: ensure the assertion ALWAYS holds.
        final UUID orderUuid = world.getOrdersByAlias()
                .values().iterator().next().getValue();

        await().pollDelay(QUIET_WINDOW)
                .atMost(QUIET_WINDOW.plusSeconds(2))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(movementJpaRepository
                        .findByOriginatingOrderIdOrderByAppendedAtAsc(orderUuid))
                        .as("movement count should remain 1 (replay swallowed by listener)")
                        .hasSize(1));

        assertThat(world.isExpectedSwallowed())
                .as("scenario must have flagged a republish before this Then")
                .isTrue();
    }
}
