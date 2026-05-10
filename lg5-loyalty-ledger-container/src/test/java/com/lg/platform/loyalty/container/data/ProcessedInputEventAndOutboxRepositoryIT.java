package com.lg.platform.loyalty.container.data;

import com.lg.platform.loyalty.application.outbox.model.OutboxMessage;
import com.lg.platform.loyalty.application.ports.output.repository.MovementLedgerRepository;
import com.lg.platform.loyalty.application.ports.output.repository.OutboxRepository;
import com.lg.platform.loyalty.application.ports.output.repository.ProcessedInputEventRepository;
import com.lg.platform.loyalty.container.boot.Bootstrap;
import com.lg.platform.loyalty.domain.entity.Movement;
import com.lg.platform.loyalty.domain.entity.ProcessedInputEvent;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import com.lg.platform.loyalty.domain.valueobject.MovementId;
import com.lg.platform.loyalty.domain.valueobject.OrderId;
import com.lg.platform.loyalty.domain.valueobject.ProcessedInputEventOutcome;
import com.lg5.spring.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the dedup contract (ADR-003) on {@code processed_input_event}
 * and the standard outbox shape (RULE-008) on {@code loyalty.outbox}.
 *
 * <p>Asserted:
 * <ul>
 *   <li>A second insert with the same
 *       {@code (originatingEventType, originatingEventId)} pair raises
 *       {@link DataIntegrityViolationException} against
 *       {@code uq_processed_event_type_id} — this is the dedup mechanism
 *       the inbound listener relies on (RULE-010 NO-OP swallow).</li>
 *   <li>{@code outcome} round-trips through the
 *       {@link jakarta.persistence.EnumType#STRING}-mapped Postgres
 *       {@code processed_input_outcome} ENUM.</li>
 *   <li>{@code OutboxRepository.findAllByStatusOrderByCreatedAtAsc} returns
 *       STARTED rows in insertion order (drives the scheduler's "fetch
 *       unpublished, oldest first" query).</li>
 *   <li>{@code markCompleted} flips a row's status atomically.</li>
 * </ul>
 */
@TestPropertySource(properties = {
        "testcontainers.postgres.enabled=true",
        "spring.datasource.url=",
        "spring.datasource.username=",
        "spring.datasource.password="
})
class ProcessedInputEventAndOutboxRepositoryIT extends Bootstrap {

    @Autowired
    private ProcessedInputEventRepository processedInputEventRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private MovementLedgerRepository movementLedgerRepository;

    /**
     * Persist a real {@link Movement} and return its id so dependent
     * {@link ProcessedInputEvent} rows satisfy the
     * {@code processed_input_event_movement_fk} FK
     * (data-model.md §processed_input_event).
     */
    private MovementId aPersistedMovementId() {
        final Movement saved = movementLedgerRepository.save(Movement.ofCredit(
                CustomerId.random(), OrderId.random(), UUID.randomUUID(),
                "OrderPaidEvent", ZonedDateTime.now(), 1));
        return saved.getId();
    }

    @Test
    void duplicate_event_type_id_raises_DataIntegrityViolationException() {
        final UUID sharedEventId = UUID.randomUUID();
        final String sharedEventType = "OrderPaidEvent";

        processedInputEventRepository.save(ProcessedInputEvent.forMovementAppended(
                sharedEventId, sharedEventType, OrderId.random(), CustomerId.random(),
                aPersistedMovementId()));

        // Second insert with the SAME (eventType, eventId) — different PK,
        // different orderId/customerId, different (valid) movementId — must
        // collide on the unique index BEFORE the FK is checked.
        final ProcessedInputEvent dup = ProcessedInputEvent.forMovementAppended(
                sharedEventId, sharedEventType, OrderId.random(), CustomerId.random(),
                aPersistedMovementId());

        assertThatThrownBy(() -> processedInputEventRepository.save(dup))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void distinct_event_ids_under_same_type_are_accepted() {
        // Sanity: the unique constraint is on the PAIR; differing event ids
        // must not collide even when the type is the same.
        final String type = "OrderCancelledEvent";
        processedInputEventRepository.save(ProcessedInputEvent.forNoopDebitWithoutCredit(
                UUID.randomUUID(), type, OrderId.random(), CustomerId.random()));
        processedInputEventRepository.save(ProcessedInputEvent.forNoopDebitWithoutCredit(
                UUID.randomUUID(), type, OrderId.random(), CustomerId.random()));
    }

    @Test
    void processed_input_event_outcome_round_trips_through_postgres_enum() {
        final ProcessedInputEvent saved = processedInputEventRepository.save(
                ProcessedInputEvent.forNoopZeroCredit(
                        UUID.randomUUID(), "OrderPaidEvent",
                        OrderId.random(), CustomerId.random()));

        final ProcessedInputEvent found = processedInputEventRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getOutcome()).isEqualTo(ProcessedInputEventOutcome.NOOP_ZERO_CREDIT);
        assertThat(found.getMovementId()).isNull();
    }

    @Test
    void outbox_started_rows_are_returned_in_creation_order() throws InterruptedException {
        // Insert two STARTED rows with a small wait so createdAt ordering is
        // unambiguous at Postgres' microsecond resolution.
        final UUID firstSagaId = UUID.randomUUID();
        outboxRepository.save(OutboxMessage.started(
                firstSagaId, "CustomerBalanceUpdated", "{\"a\":1}"));
        Thread.sleep(10);
        final UUID secondSagaId = UUID.randomUUID();
        outboxRepository.save(OutboxMessage.started(
                secondSagaId, "CustomerBalanceUpdated", "{\"a\":2}"));

        final List<OutboxMessage> started =
                outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.STARTED);

        assertThat(started).extracting(OutboxMessage::sagaId)
                .containsSubsequence(firstSagaId, secondSagaId);
        assertThat(started).allMatch(m -> m.status() == OutboxStatus.STARTED);
        assertThat(started).allMatch(m -> "CustomerBalanceUpdated".equals(m.type()));
    }

    @Test
    void outbox_status_can_be_flipped_to_completed() {
        final OutboxMessage saved = outboxRepository.save(OutboxMessage.started(
                UUID.randomUUID(), "CustomerBalanceUpdated", "{\"x\":42}"));

        outboxRepository.markCompleted(saved.id());

        final List<OutboxMessage> completed =
                outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.COMPLETED);
        assertThat(completed).extracting(OutboxMessage::id).contains(saved.id());

        final List<OutboxMessage> stillStarted =
                outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.STARTED);
        assertThat(stillStarted).extracting(OutboxMessage::id).doesNotContain(saved.id());
    }
}
