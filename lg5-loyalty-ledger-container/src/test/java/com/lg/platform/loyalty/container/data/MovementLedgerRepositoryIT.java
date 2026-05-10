package com.lg.platform.loyalty.container.data;

import com.lg.platform.loyalty.application.ports.output.repository.MovementLedgerRepository;
import com.lg.platform.loyalty.dataaccess.movement.repository.MovementJpaRepository;
import com.lg.platform.loyalty.domain.entity.Movement;
import com.lg.platform.loyalty.domain.valueobject.BalanceUpdateCause;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import com.lg.platform.loyalty.container.boot.Bootstrap;
import com.lg.platform.loyalty.domain.valueobject.OrderId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip + REQ-013 + REQ-004 verification for the {@code movement}
 * ledger via the {@link MovementLedgerRepository} output port.
 *
 * <p>Specifically asserts:
 * <ul>
 *   <li>A credit and a debit can be appended and read back with every
 *       field intact (mapper round-trip preserves
 *       {@code originatingEventId}, REQ-014).</li>
 *   <li>The Postgres {@code loyalty_cause} ENUM round-trips correctly
 *       through {@link jakarta.persistence.EnumType#STRING} mapping
 *       (relies on the JDBC URL's {@code stringtype=unspecified}).</li>
 *   <li>The Spring Data repository surface declares only
 *       {@code save(...)} and read methods — no {@code update*} or
 *       {@code delete*} (REQ-013).</li>
 *   <li>{@code existsCreditFor} (REQ-004) returns {@code true} only
 *       when a positive-delta movement exists for that order.</li>
 * </ul>
 */
@TestPropertySource(properties = {
        "testcontainers.postgres.enabled=true",
        "spring.datasource.url=",
        "spring.datasource.username=",
        "spring.datasource.password="
})
class MovementLedgerRepositoryIT extends Bootstrap {

    @Autowired
    private MovementLedgerRepository movementLedgerRepository;

    @Test
    void credit_movement_round_trips_through_the_port() {
        final CustomerId customerId = CustomerId.random();
        final OrderId orderId = OrderId.random();
        final UUID eventId = UUID.randomUUID();
        final ZonedDateTime receivedAt = ZonedDateTime.now().minusSeconds(5);

        final Movement saved = movementLedgerRepository.save(
                Movement.ofCredit(customerId, orderId, eventId,
                        "OrderPaidEvent", receivedAt, 100));

        final Movement found = movementLedgerRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getId().getValue()).isEqualTo(saved.getId().getValue());
        assertThat(found.getCustomerId().getValue()).isEqualTo(customerId.getValue());
        assertThat(found.getDelta()).isEqualTo(100);
        assertThat(found.getCause()).isEqualTo(BalanceUpdateCause.ORDER_PAID);
        assertThat(found.getOriginatingOrderId().getValue()).isEqualTo(orderId.getValue());
        assertThat(found.getOriginatingEventId()).isEqualTo(eventId);
        assertThat(found.getOriginatingEventType()).isEqualTo("OrderPaidEvent");
        // REQ-014 cross-check: the originating-event audit fields survive a write+read.
        assertThat(found.getOriginatingEventReceivedAt()).isEqualTo(receivedAt);
        assertThat(found.getAppendedAt()).isNotNull();
    }

    @Test
    void debit_movement_with_cancelled_cause_round_trips() {
        final CustomerId customerId = CustomerId.random();
        final OrderId orderId = OrderId.random();

        final Movement saved = movementLedgerRepository.save(
                Movement.ofDebit(customerId, orderId, UUID.randomUUID(),
                        "OrderCancelledEvent", ZonedDateTime.now(), -50,
                        BalanceUpdateCause.ORDER_CANCELLED));

        final Movement found = movementLedgerRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getDelta()).isEqualTo(-50);
        assertThat(found.getCause()).isEqualTo(BalanceUpdateCause.ORDER_CANCELLED);
    }

    @Test
    void existsCreditFor_returns_true_only_when_a_prior_credit_exists() {
        final OrderId orderWithCredit = OrderId.random();
        final OrderId orderWithoutCredit = OrderId.random();
        final CustomerId customer = CustomerId.random();

        movementLedgerRepository.save(Movement.ofCredit(
                customer, orderWithCredit, UUID.randomUUID(),
                "OrderPaidEvent", ZonedDateTime.now(), 200));

        assertThat(movementLedgerRepository.existsCreditFor(orderWithCredit)).isTrue();
        assertThat(movementLedgerRepository.existsCreditFor(orderWithoutCredit)).isFalse();
    }

    @Test
    void existsCreditFor_is_false_when_only_a_debit_exists_for_the_order() {
        // Defensive: a debit alone (delta < 0) must NOT satisfy "credit exists" (REQ-004).
        final OrderId orderId = OrderId.random();
        movementLedgerRepository.save(Movement.ofDebit(
                CustomerId.random(), orderId, UUID.randomUUID(),
                "OrderRefundedEvent", ZonedDateTime.now(), -30,
                BalanceUpdateCause.ORDER_REFUNDED));

        assertThat(movementLedgerRepository.existsCreditFor(orderId)).isFalse();
    }

    @Test
    void jpa_repository_does_not_expose_update_or_delete_methods() {
        // REQ-013 enforced at the repository surface: by extending only the
        // bare org.springframework.data.repository.Repository marker, Spring
        // Data does NOT generate delete*/deleteAll*/saveAndFlush overloads.
        // We assert this directly so an accidental refactor (e.g. switching
        // to JpaRepository) is caught at IT time.
        final Method[] declared = MovementJpaRepository.class.getMethods();
        for (final Method m : declared) {
            assertThat(m.getName())
                    .as("MovementJpaRepository must not expose mutating method '" + m.getName() + "'")
                    .doesNotStartWith("delete");
            assertThat(m.getName()).doesNotStartWith("update");
        }
    }
}
