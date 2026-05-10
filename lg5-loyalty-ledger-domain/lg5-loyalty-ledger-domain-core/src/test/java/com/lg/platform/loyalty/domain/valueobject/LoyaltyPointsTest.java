package com.lg.platform.loyalty.domain.valueobject;

import com.labs.lg.pentagon.common.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the REQ-002 / PRD Q1 floor-to-int-EUR rule.
 */
class LoyaltyPointsTest {

    @Test
    void floors_12_95_to_12() {
        assertThat(LoyaltyPoints.floorEurosFrom(new Money(new BigDecimal("12.95"))))
                .isEqualTo(12);
    }

    @Test
    void floors_0_50_to_0() {
        assertThat(LoyaltyPoints.floorEurosFrom(new Money(new BigDecimal("0.50"))))
                .isEqualTo(0);
    }

    @Test
    void floors_exact_1_00_to_1() {
        assertThat(LoyaltyPoints.floorEurosFrom(new Money(new BigDecimal("1.00"))))
                .isEqualTo(1);
    }

    @Test
    void treats_null_amount_as_zero() {
        assertThat(LoyaltyPoints.floorEurosFrom(null)).isEqualTo(0);
    }

    @Test
    void clamps_negative_to_zero() {
        assertThat(LoyaltyPoints.floorEurosFrom(new Money(new BigDecimal("-5.00"))))
                .isEqualTo(0);
    }
}
