package com.lg.platform.loyalty.domain.valueobject;

/**
 * Cause of a {@code Movement} / {@code CustomerBalance} change.
 * <p>
 * No {@code UNKNOWN} symbol here — that's an Avro-only forward-compat
 * symbol per ADR-005, not a domain concept.
 */
public enum BalanceUpdateCause {
    ORDER_PAID,
    ORDER_CANCELLED,
    ORDER_REFUNDED
}
