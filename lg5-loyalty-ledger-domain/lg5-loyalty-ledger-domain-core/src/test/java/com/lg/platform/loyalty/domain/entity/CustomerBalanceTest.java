package com.lg.platform.loyalty.domain.entity;

import com.lg.platform.loyalty.domain.exception.LoyaltyLedgerDomainException;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerBalanceTest {

    private final CustomerId customerId = CustomerId.random();

    @Test
    void empty_returnsBalanceOfZero_andVersionZero() {
        final CustomerBalance cb = CustomerBalance.empty(customerId);

        assertThat(cb.getId()).isEqualTo(customerId);
        assertThat(cb.getBalance()).isZero();
        assertThat(cb.getVersion()).isZero();
        assertThat(cb.getLastUpdatedAt()).isNotNull();
    }

    @Test
    void applyDelta_withPositiveDelta_increasesBalance() {
        final CustomerBalance cb = CustomerBalance.empty(customerId);

        cb.applyDelta(15);

        assertThat(cb.getBalance()).isEqualTo(15L);
        // Domain does NOT bump version; Hibernate owns @Version end-to-end.
        assertThat(cb.getVersion()).isZero();
    }

    @Test
    void applyDelta_withNegativeDelta_thatGoesBelowZero_isAccepted() {
        // REQ-007: balance may go negative without exception.
        final CustomerBalance cb = CustomerBalance.empty(customerId);
        cb.applyDelta(5);

        cb.applyDelta(-12);

        assertThat(cb.getBalance()).isEqualTo(-7L);
        assertThat(cb.getVersion()).isZero();
    }

    @Test
    void applyDelta_withZeroDelta_throwsLoyaltyLedgerDomainException() {
        final CustomerBalance cb = CustomerBalance.empty(customerId);

        assertThatThrownBy(() -> cb.applyDelta(0))
                .isInstanceOf(LoyaltyLedgerDomainException.class);
    }

    @Test
    void multipleApplyDelta_yieldsAlgebraicSum() {
        final CustomerBalance cb = CustomerBalance.empty(customerId);
        cb.applyDelta(100);
        cb.applyDelta(-150);
        cb.applyDelta(50);

        assertThat(cb.getBalance()).isZero();
        // Version remains 0 at the domain level (Hibernate owns it).
        assertThat(cb.getVersion()).isZero();
    }
}
