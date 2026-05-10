package com.lg.platform.loyalty.api.dto;

/**
 * Uniform error envelope for the loyalty-ledger REST API
 * (TASK-017, REQ-009 + REQ-015).
 *
 * <p>Three fields by design — the framework's
 * {@code com.lg5.spring.api.rest.ErrorDTO} ships only
 * {@code (code, message)}, which is insufficient because RULE for
 * 5xx responses requires a {@code traceId} for operator
 * correlation. Sleuth/Brave is intentionally NOT on the classpath
 * of this service (RULE-014 baseline), so {@code traceId} is
 * generated on the spot via {@link java.util.UUID#randomUUID()} for
 * 5xx and is {@code null} for 4xx (where the client has enough
 * context from {@code code} alone).
 *
 * @param code     stable machine-readable code:
 *                 {@code INVALID_REQUEST} (400),
 *                 {@code CUSTOMER_NOT_FOUND} (404),
 *                 {@code INTERNAL} (500). New codes MUST be added
 *                 backwards-compatibly.
 * @param message  human-readable, sanitised description. NEVER
 *                 echo raw exception messages from 5xx — the
 *                 stacktrace stays in the server log.
 * @param traceId  populated for 5xx; {@code null} for 4xx.
 */
public record ErrorDTO(String code, String message, String traceId) {
}
