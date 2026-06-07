package com.lg.platform.loyalty.container.data;

import com.lg.platform.loyalty.application.ports.output.repository.CustomerBalanceRepository;
import com.lg.platform.loyalty.boot.Bootstrap;
import com.lg.platform.loyalty.dataaccess.balance.repository.CustomerBalanceJpaRepository;
import com.lg.platform.loyalty.domain.entity.CustomerBalance;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the {@link CustomerBalanceRepository} output port honors REQ-007
 * (negative balances allowed), REQ-008/REQ-009 (projection upsert), RULE-008
 * (JPA {@code @Version}), and the data-model.md §CustomerBalance no-delete
 * contract.
 *
 * <p>
 * Acceptance scenario from tasks.md TASK-006: apply {@code +100, -150, +50} to
 * a single customer ⇒ final balance is {@code 0}; intermediate state
 * {@code -50} is observable by a concurrent read with no exception (REQ-007).
 */
class CustomerBalanceRepositoryIT extends Bootstrap {

	@Autowired
	private CustomerBalanceRepository customerBalanceRepository;

	@Test
	void sequence_of_deltas_yields_expected_final_balance_and_version() {
		final CustomerId customerId = CustomerId.random();

		final CustomerBalance initial = CustomerBalance.empty(customerId);
		initial.applyDelta(100);
		final CustomerBalance afterFirst = this.customerBalanceRepository.save(initial);
		assertThat(afterFirst.getBalance()).isEqualTo(100L);
		// Hibernate's @Version starts at 0 on INSERT and is bumped on each UPDATE.
		assertThat(afterFirst.getVersion()).isZero();

		afterFirst.applyDelta(-150);
		final CustomerBalance afterSecond = this.customerBalanceRepository.save(afterFirst);
		// REQ-007: negative balance is allowed and observable.
		assertThat(afterSecond.getBalance()).isEqualTo(-50L);
		assertThat(afterSecond.getVersion()).isEqualTo(1);

		// Concurrent-read simulation: a fresh load while the row is at -50
		// must succeed and return -50 with no exception.
		final CustomerBalance concurrentRead = this.customerBalanceRepository.findById(customerId).orElseThrow();
		assertThat(concurrentRead.getBalance()).isEqualTo(-50L);

		afterSecond.applyDelta(50);
		final CustomerBalance afterThird = this.customerBalanceRepository.save(afterSecond);
		assertThat(afterThird.getBalance()).isEqualTo(0L);
		assertThat(afterThird.getVersion()).isEqualTo(2);
	}

	@Test
	void stale_write_raises_OptimisticLockingFailureException() {
		final CustomerId customerId = CustomerId.random();
		final CustomerBalance fresh = CustomerBalance.empty(customerId);
		fresh.applyDelta(10);
		final CustomerBalance afterFirst = this.customerBalanceRepository.save(fresh); // INSERT, version=0
		// One UPDATE so two readers can race on a non-zero version.
		afterFirst.applyDelta(5);
		this.customerBalanceRepository.save(afterFirst); // version is now 1

		// Two readers grab the row at version=1.
		final CustomerBalance readerA = this.customerBalanceRepository.findById(customerId).orElseThrow();
		final CustomerBalance readerB = this.customerBalanceRepository.findById(customerId).orElseThrow();
		assertThat(readerA.getVersion()).isEqualTo(1);
		assertThat(readerB.getVersion()).isEqualTo(1);

		// Reader A wins; row advances to version=2.
		readerA.applyDelta(5);
		this.customerBalanceRepository.save(readerA);

		// Reader B is now stale (still at version=1) — write must fail.
		readerB.applyDelta(-3);
		assertThatThrownBy(() -> this.customerBalanceRepository.save(readerB))
				.isInstanceOf(OptimisticLockingFailureException.class);
	}

	@Test
	void jpa_repository_does_not_expose_delete_methods() {
		// data-model.md §CustomerBalance: rows are never deleted.
		// Updates ARE allowed (projection mutation), so this assertion is
		// narrower than MovementJpaRepository's: only delete* is forbidden.
		for (final Method m : CustomerBalanceJpaRepository.class.getMethods()) {
			assertThat(m.getName())
					.as("CustomerBalanceJpaRepository must not expose mutating method '" + m.getName() + "'")
					.doesNotStartWith("delete");
		}
	}
}
