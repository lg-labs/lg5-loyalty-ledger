package com.lg.platform.loyalty.acceptance.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labs.lg.pentagon.common.domain.valueobject.Money;
import com.lg.platform.loyalty.acceptance.support.World;
import com.lg.platform.loyalty.application.outbox.model.OutboxMessage;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerCommand;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerInputPort;
import com.lg.platform.loyalty.application.ports.output.repository.CustomerBalanceRepository;
import com.lg.platform.loyalty.application.ports.output.repository.OutboxRepository;
import com.lg.platform.loyalty.dataaccess.movement.entity.MovementJpaEntity;
import com.lg.platform.loyalty.dataaccess.movement.repository.MovementJpaRepository;
import com.lg.platform.loyalty.domain.valueobject.BalanceUpdateCause;
import com.lg.platform.loyalty.domain.valueobject.OrderId;
import com.lg5.spring.outbox.OutboxStatus;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions covering the domain-write paths exercised
 * directly through the {@link LoyaltyLedgerInputPort} (TASK-019).
 *
 * <p><b>Why drive through the input port instead of Kafka.</b>
 * The IT-layer kafka listener IT
 * ({@code OrderPaidKafkaListenerIT} et al) and the publisher IT
 * ({@code CustomerBalanceUpdatedKafkaPublisherIT}) already prove
 * Kafka E2E for one event each; reproducing that round-trip
 * inside Cucumber for every requirement would multiply scenario
 * runtime by &gt;20× without raising coverage. ATDD scenarios
 * here therefore drive the input port directly and assert on:
 * <ul>
 *   <li>{@code MovementJpaRepository} — REQ-001/004 movement
 *       counts &amp; deltas, REQ-013 append-only invariant
 *       (number-of-rows monotonically non-decreasing).</li>
 *   <li>{@code CustomerBalanceRepository} — REQ-001 / REQ-002
 *       (floor-to-12), REQ-007 (negative balance observable).</li>
 *   <li>{@code OutboxRepository} — REQ-011 / REQ-012 (outbound
 *       payload by inspecting the persisted JSON, the same shape
 *       the publisher consumes).</li>
 * </ul>
 * REQ-015 (Kafka swallow) cannot be observed at this layer
 * because the swallow happens inside the listener; that scenario
 * is the sole user of {@code KafkaTestSupport}.
 *
 * <p>RULE-005: stock {@code @Component} only. Cucumber-Spring
 * picks up step-def classes via classpath scan; the
 * {@code @CucumberContextConfiguration} on
 * {@code CucumberHooks} drives the scan.
 */
@Slf4j
@Component
public class LedgerSteps {

    private final World world;
    private final LoyaltyLedgerInputPort handler;
    private final CustomerBalanceRepository customerBalanceRepository;
    private final MovementJpaRepository movementJpaRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public LedgerSteps(final World world,
                       final LoyaltyLedgerInputPort handler,
                       final CustomerBalanceRepository customerBalanceRepository,
                       final MovementJpaRepository movementJpaRepository,
                       final OutboxRepository outboxRepository,
                       final ObjectMapper objectMapper) {
        this.world = world;
        this.handler = handler;
        this.customerBalanceRepository = customerBalanceRepository;
        this.movementJpaRepository = movementJpaRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    // ─────────────────────────── GIVEN ────────────────────────────

    @Given("a fresh customer")
    public void aFreshCustomer() {
        // Force allocation; subsequent steps reuse via World.
        world.customerOrNew();
    }

    /**
     * Convenience seeding step: appends a credit on the named
     * order so a downstream {@code When} can assert on a debit /
     * cancel without spelling out the prerequisite paid event.
     */
    @Given("the customer has been credited {string} on order {string}")
    public void givenCredited(final String amount, final String orderAlias) {
        final UUID eventId = UUID.randomUUID();
        handler.process(new LoyaltyLedgerCommand.OrderPaidCommand(
                eventId,
                world.customerOrNew(),
                world.orderFor(orderAlias),
                ZonedDateTime.now(),
                new Money(new BigDecimal(amount))));
    }

    // ─────────────────────────── WHEN ─────────────────────────────

    @When("OrderPaid {string} arrives for order {string} with event {string}")
    public void orderPaidArrives(final String amount,
                                 final String orderAlias,
                                 final String eventAlias) {
        final var cmd = new LoyaltyLedgerCommand.OrderPaidCommand(
                world.eventIdFor(eventAlias),
                world.customerOrNew(),
                world.orderFor(orderAlias),
                ZonedDateTime.now(),
                new Money(new BigDecimal(amount)));
        world.setLastCommand(cmd);
        try {
            handler.process(cmd);
        } catch (final org.springframework.dao.DataIntegrityViolationException dup) {
            // ADR-003: replay caught at the dedup unique index.
            // The Kafka listener swallows this in production
            // (RULE-010); driving through the input port surfaces
            // it to us. REQ-003/006 scenarios assert that the
            // SECOND call did not produce a new movement —
            // swallowing here keeps the scenario flowing so the
            // Then steps can verify the no-side-effect invariant.
            log.debug("Dedup constraint blocked replay of eventId={}", cmd.eventId());
        }
    }

    @When("OrderCancelled arrives for order {string} with event {string}")
    public void orderCancelledArrives(final String orderAlias, final String eventAlias) {
        final var cmd = new LoyaltyLedgerCommand.OrderCancelledCommand(
                world.eventIdFor(eventAlias),
                world.customerOrNew(),
                world.orderFor(orderAlias),
                ZonedDateTime.now());
        world.setLastCommand(cmd);
        try {
            handler.process(cmd);
        } catch (final org.springframework.dao.DataIntegrityViolationException dup) {
            log.debug("Dedup constraint blocked replay of eventId={}", cmd.eventId());
        }
    }

    @When("OrderRefunded arrives for order {string} with event {string}")
    public void orderRefundedArrives(final String orderAlias, final String eventAlias) {
        final var cmd = new LoyaltyLedgerCommand.OrderRefundedCommand(
                world.eventIdFor(eventAlias),
                world.customerOrNew(),
                world.orderFor(orderAlias),
                ZonedDateTime.now());
        world.setLastCommand(cmd);
        try {
            handler.process(cmd);
        } catch (final org.springframework.dao.DataIntegrityViolationException dup) {
            log.debug("Dedup constraint blocked replay of eventId={}", cmd.eventId());
        }
    }

    /**
     * REQ-010 paging fixture: emit N OrderPaid events of 1 EUR
     * each on distinct orders so the paged GET exercise has
     * deterministic input. 1 EUR floors to +1 credit (REQ-001),
     * giving a balance of N at the end.
     */
    @When("the customer receives {int} OrderPaid events of {string} each")
    public void customerReceivesNPayments(final int count, final String amount) {
        for (int i = 0; i < count; i++) {
            handler.process(new LoyaltyLedgerCommand.OrderPaidCommand(
                    UUID.randomUUID(),
                    world.customerOrNew(),
                    OrderId.random(),
                    ZonedDateTime.now(),
                    new Money(new BigDecimal(amount))));
        }
    }

    // ─────────────────────────── THEN ─────────────────────────────

    @Then("the customer balance is {long}")
    public void customerBalanceIs(final long expected) {
        // A missing CustomerBalance row is semantically equivalent
        // to balance=0 (REQ-002 zero-credit path never writes the
        // row; REQ-005 cancel-without-prior-credit likewise).
        // Asserting through Optional.map keeps both branches in one
        // assertion.
        final long actual = customerBalanceRepository.findById(world.customerOrNew())
                .map(b -> b.getBalance())
                .orElse(0L);
        assertThat(actual).isEqualTo(expected);
    }

    @Then("the customer has {int} movement(s) on order {string}")
    public void movementsOnOrder(final int expected, final String orderAlias) {
        final List<MovementJpaEntity> rows = movementJpaRepository
                .findByOriginatingOrderIdOrderByAppendedAtAsc(world.orderFor(orderAlias).getValue());
        assertThat(rows).hasSize(expected);
    }

    @Then("the customer has {int} movement(s) total")
    public void movementsTotal(final int expected) {
        final long actual = movementJpaRepository.countByCustomerId(world.customerOrNew().getValue());
        assertThat(actual).isEqualTo(expected);
    }

    @Then("a movement was appended with delta {int} and cause {string} on order {string}")
    public void movementAppendedWith(final int delta,
                                     final String cause,
                                     final String orderAlias) {
        final List<MovementJpaEntity> rows = movementJpaRepository
                .findByOriginatingOrderIdOrderByAppendedAtAsc(world.orderFor(orderAlias).getValue());
        assertThat(rows)
                .as("movements on order " + orderAlias)
                .anySatisfy(m -> {
                    assertThat(m.getDelta()).isEqualTo(delta);
                    assertThat(m.getCause()).isEqualTo(BalanceUpdateCause.valueOf(cause));
                });
    }

    /**
     * REQ-011 / REQ-012: outbox payload field-by-field assertion.
     * Mirrors {@code LoyaltyLedgerHandlerHappyPathIT.caseA_*} but
     * locates the row by saga-id ({@code originatingEventId}, set
     * by the handler when it constructs the outbox row).
     */
    @Then("the outbox payload for event {string} has newBalance={long} delta={int} cause={string} originatingEventType={string}")
    public void outboxPayload(final String eventAlias,
                              final long newBalance,
                              final int delta,
                              final String cause,
                              final String eventType) throws Exception {
        final UUID eventId = world.eventIdFor(eventAlias);
        // Look across BOTH STARTED and COMPLETED — the always-on
        // 200ms scheduler may have flipped the row to COMPLETED
        // by the time this assertion runs.
        final OutboxMessage row = findOutboxRowBySagaId(eventId);
        final JsonNode payload = objectMapper.readTree(row.payload());
        assertThat(payload.get("customerId").asText())
                .isEqualTo(world.customerOrNew().getValue().toString());
        assertThat(payload.get("newBalance").asLong()).isEqualTo(newBalance);
        assertThat(payload.get("delta").asInt()).isEqualTo(delta);
        assertThat(payload.get("cause").asText()).isEqualTo(cause);
        assertThat(payload.get("originatingEventType").asText()).isEqualTo(eventType);
        // REQ-014: origin trace flows through to the payload.
        assertThat(payload.get("originatingEventId").asText())
                .isEqualTo(eventId.toString());
        assertThat(payload.get("originatingOrderId").asText())
                .isEqualTo(world.getLastCommand().orderId().getValue().toString());
    }

    @Then("exactly {int} outbox row(s) carry event {string}")
    public void exactlyNOutboxRowsForEvent(final int expected, final String eventAlias) {
        final UUID eventId = world.eventIdFor(eventAlias);
        final long count = allOutboxRows().stream()
                .filter(r -> r.sagaId().equals(eventId))
                .count();
        assertThat(count).isEqualTo(expected);
    }

    private OutboxMessage findOutboxRowBySagaId(final UUID sagaId) {
        return allOutboxRows().stream()
                .filter(r -> r.sagaId().equals(sagaId))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No outbox row for sagaId=" + sagaId
                                + " in any status (STARTED, COMPLETED, FAILED)"));
    }

    private List<OutboxMessage> allOutboxRows() {
        // Concatenate the three status buckets — a race with the
        // outbox scheduler (200 ms) is otherwise possible.
        final List<OutboxMessage> all = new java.util.ArrayList<>();
        all.addAll(outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.STARTED));
        all.addAll(outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.COMPLETED));
        all.addAll(outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.FAILED));
        return all;
    }
}
