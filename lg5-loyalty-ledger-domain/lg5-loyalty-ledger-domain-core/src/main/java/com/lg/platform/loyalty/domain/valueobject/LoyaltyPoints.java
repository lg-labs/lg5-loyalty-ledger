package com.lg.platform.loyalty.domain.valueobject;

import com.labs.lg.pentagon.common.domain.valueobject.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Loyalty-point arithmetic helper (REQ-002 / PRD Q1).
 *
 * <p>The "amount → points" rule is one floored integer EUR per point:
 * a paid amount of {@code 12.95 EUR} grants {@code 12} points, and
 * {@code 0.50 EUR} grants {@code 0} points (which the handler turns
 * into {@code outcome=NOOP_ZERO_CREDIT} per REQ-002 / Q1 — no movement,
 * no outbox).
 *
 * <p>Living in {@code domain-core} (not in the message-side mapper)
 * keeps REQ-002 expressed as a domain invariant: changing the rule
 * (e.g., introducing decimal points) would touch this class only,
 * not the wire-translation layer.
 */
public final class LoyaltyPoints {

    private LoyaltyPoints() {
        // utility
    }

    /**
     * Floors {@code paidAmount} to the nearest non-negative integer
     * number of EUR points. The result MAY be {@code 0} when the
     * amount is below {@code 1.00}; the caller is expected to short
     * circuit a zero result into {@code NOOP_ZERO_CREDIT} (the
     * domain forbids {@code Movement.delta == 0}).
     */
    public static int floorEurosFrom(final Money paidAmount) {
        if (paidAmount == null || paidAmount.getAmount() == null) {
            return 0;
        }
        final BigDecimal floored = paidAmount.getAmount().setScale(0, RoundingMode.FLOOR);
        // Cap at Integer.MAX_VALUE; Movement.delta is int. Realistic
        // paidAmount values are well below this, so we do not throw.
        if (floored.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) >= 0) {
            return Integer.MAX_VALUE;
        }
        if (floored.signum() < 0) {
            return 0;
        }
        return floored.intValueExact();
    }
}
