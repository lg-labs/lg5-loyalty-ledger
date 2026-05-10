package com.lg.platform.loyalty.domain.valueobject;

/**
 * Outcome of processing one inbound business event (ADR-003 / REQ-014).
 */
public enum ProcessedInputEventOutcome {
    MOVEMENT_APPENDED,
    NOOP_ZERO_CREDIT,
    NOOP_DEBIT_WITHOUT_CREDIT
}
