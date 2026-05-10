package com.lg.platform.loyalty.application.outbox.scheduler;

import com.lg.platform.loyalty.application.outbox.model.OutboxMessage;
import com.lg.platform.loyalty.application.ports.output.repository.OutboxRepository;
import com.lg5.spring.outbox.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application-service helper bridging the outbox scheduler to the
 * {@link OutboxRepository} (TASK-013). Mirrors food-ordering's
 * {@code OrderOutboxHelper} layout but adapts to the
 * {@link OutboxMessage} immutable record + the surface already
 * provided by {@link OutboxRepository#markCompleted(java.util.UUID)}
 * / {@link OutboxRepository#markFailed(java.util.UUID)}.
 *
 * <p>Lives in {@code application-service} (not in {@code message-core})
 * because it manipulates an output-port; the Kafka adapter never
 * touches the outbox table directly (hexagonal hygiene).
 *
 * <p>The {@link #updateOutboxMessage(OutboxMessage, OutboxStatus)} call
 * is made from the {@code KafkaProducer} send-result callback, which
 * runs on the producer thread <em>outside</em> the scheduler's
 * transaction (Spring's {@code @Transactional} on
 * {@code processOutboxMessage()} only spans the read + dispatch
 * phase). The {@code @Transactional(REQUIRES_NEW)}-equivalent here is
 * the default propagation: a fresh transaction is opened by the
 * outer Spring proxy when the callback re-enters this bean. The
 * single SQL UPDATE that {@code markCompleted}/{@code markFailed}
 * performs already carries the {@code @Version} optimistic-lock
 * predicate (RULE-008), so a concurrent re-run that completed first
 * surfaces as {@link OptimisticLockingFailureException}, which we
 * swallow as a NO-OP (the row reached the desired terminal state).
 */
@Slf4j
@Component
public class OutboxSchedulerHelper {

    private final OutboxRepository outboxRepository;

    public OutboxSchedulerHelper(final OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional(readOnly = true)
    public List<OutboxMessage> findStarted() {
        return outboxRepository.findAllByStatusOrderByCreatedAtAsc(OutboxStatus.STARTED);
    }

    /**
     * Callback target invoked by the Kafka adapter after the
     * send-result is observed. Translates the framework-level
     * {@link OutboxStatus} into the corresponding repository update.
     * Optimistic-lock collisions (concurrent scheduler tick already
     * marked the row terminal) are swallowed (RULE-010 spirit:
     * republish on restart is harmless because a {@code COMPLETED}
     * row will never be picked up again by {@link #findStarted()}).
     */
    @Transactional
    public void updateOutboxMessage(final OutboxMessage outboxMessage, final OutboxStatus status) {
        try {
            switch (status) {
                case COMPLETED -> {
                    outboxRepository.markCompleted(outboxMessage.id());
                    log.debug("Outbox {} → COMPLETED (sagaId={}, type={})",
                            outboxMessage.id(), outboxMessage.sagaId(), outboxMessage.type());
                }
                case FAILED -> {
                    outboxRepository.markFailed(outboxMessage.id());
                    log.warn("Outbox {} → FAILED (sagaId={}, type={})",
                            outboxMessage.id(), outboxMessage.sagaId(), outboxMessage.type());
                }
                case STARTED -> log.warn("Ignoring no-op transition outbox {} → STARTED (was already STARTED)",
                        outboxMessage.id());
                default -> log.warn("Ignoring unknown outbox status transition for {}: {}",
                        outboxMessage.id(), status);
            }
        } catch (final OptimisticLockingFailureException ex) {
            // A concurrent scheduler tick already drove this row to a
            // terminal status. Swallow per RULE-010-style NO-OP: the
            // row is in (or beyond) the desired state.
            log.debug("Optimistic-lock NO-OP marking outbox {} as {} (already terminal)",
                    outboxMessage.id(), status);
        }
    }
}
