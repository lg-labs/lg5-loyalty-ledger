package com.lg.platform.loyalty.application.outbox.scheduler;

import com.lg.platform.loyalty.application.outbox.model.OutboxMessage;
import com.lg.platform.loyalty.application.ports.output.message.publisher.CustomerBalanceUpdatedMessagePublisher;
import com.lg5.spring.outbox.OutboxScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Outbox scheduler implementing the framework
 * {@link OutboxScheduler} contract (RULE-011): periodically reads
 * {@code STARTED} outbox rows in creation order and dispatches each
 * to the {@link CustomerBalanceUpdatedMessagePublisher} adapter
 * ({@code message-core}). Mirrors food-ordering-system's
 * {@code OrderOutboxScheduler} layout.
 *
 * <p>Gated by {@code scheduling.enabled} (RULE-012,
 * {@code application-test.yaml} sets {@code false} by default; ITs
 * flip to {@code true} via {@code @TestPropertySource}). The fixed
 * rate and initial delay come from
 * {@code loyalty-ledger-service.outbox-scheduler-fixed-rate /
 * -initial-delay} (RULE-014).
 *
 * <p>The {@code @Transactional} on {@code processOutboxMessage()}
 * spans the {@code findStarted()} read and the synchronous
 * dispatch loop (the Kafka send is non-blocking — the producer's
 * thread executes the callback later, in its own transaction via
 * {@link OutboxSchedulerHelper#updateOutboxMessage}). This means
 * the scheduler's own transaction commits with the rows still in
 * {@code STARTED} status; the per-row state transition to
 * {@code COMPLETED}/{@code FAILED} happens asynchronously in the
 * helper. A concurrent scheduler tick observing the same
 * {@code STARTED} row will republish it (idempotent: same
 * outbox-row-id → same Avro {@code messageId}) and the eventual
 * mark-complete will collide harmlessly via optimistic locking
 * (handled in the helper).
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "scheduling.enabled", matchIfMissing = true)
public class CustomerBalanceUpdatedOutboxScheduler implements OutboxScheduler {

    private final OutboxSchedulerHelper outboxSchedulerHelper;
    private final CustomerBalanceUpdatedMessagePublisher publisher;

    public CustomerBalanceUpdatedOutboxScheduler(
            final OutboxSchedulerHelper outboxSchedulerHelper,
            final CustomerBalanceUpdatedMessagePublisher publisher) {
        this.outboxSchedulerHelper = outboxSchedulerHelper;
        this.publisher = publisher;
    }

    @Override
    @Transactional
    @Scheduled(
            fixedDelayString = "${loyalty-ledger-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${loyalty-ledger-service.outbox-scheduler-initial-delay}")
    public void processOutboxMessage() {
        final List<OutboxMessage> started = outboxSchedulerHelper.findStarted();
        if (started.isEmpty()) {
            return;
        }
        log.info("Outbox scheduler picked up {} STARTED row(s); dispatching to publisher", started.size());
        for (final OutboxMessage message : started) {
            publisher.publish(message, outboxSchedulerHelper::updateOutboxMessage);
        }
    }
}
