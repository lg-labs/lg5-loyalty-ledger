package com.lg.platform.loyalty.api.rest;

import com.lg.platform.loyalty.api.dto.ErrorDTO;
import com.lg.platform.loyalty.application.exception.CustomerBalanceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

/**
 * Centralised REST exception mapping (TASK-017, REQ-009 + REQ-015).
 *
 * <p>Three handlers, one per HTTP status:
 * <ul>
 *   <li>{@link MethodArgumentTypeMismatchException} → {@code 400
 *       INVALID_REQUEST}: covers a malformed UUID in the path or
 *       any non-coercible {@code @PathVariable} / {@code @RequestParam}.
 *       This is the symptom we hit in TASK-015 (a non-UUID in the
 *       balance path used to surface as the framework's default
 *       400 with no body shape).</li>
 *   <li>{@link CustomerBalanceNotFoundException} → {@code 404
 *       CUSTOMER_NOT_FOUND}: read-side application exception
 *       thrown by {@code LoyaltyLedgerQueryServiceImpl.getBalance}
 *       when no projection row exists yet.</li>
 *   <li>{@link Exception} catch-all → {@code 500 INTERNAL}: emits a
 *       fresh {@code traceId} ({@link UUID#randomUUID()}, since
 *       Sleuth is not on the classpath — RULE-014) and logs the
 *       full stacktrace at {@code ERROR} keyed by the same
 *       {@code traceId}, so an operator can grep the log without
 *       leaking the raw message to the client.</li>
 * </ul>
 *
 * <p>Ordered {@link Ordered#HIGHEST_PRECEDENCE} so it wins against
 * any framework-default advice that might be auto-registered by a
 * future starter upgrade (the framework's own
 * {@code com.lg5.spring.api.rest.GlobalExceptionHandler} is NOT
 * scanned in this app — only {@code com.lg.platform.loyalty},
 * {@code com.lg5.spring.kafka}, {@code com.lg5.spring.outbox}
 * are — but we lock the order defensively).
 *
 * <p>REQ-015 scope clarification: listener-side failures are
 * swallowed inside the Kafka listener per RULE-010 + ADR-003 and
 * never reach this advice. This class strictly handles the REST
 * surface.
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoyaltyLedgerExceptionAdvice {

    private static final String CONTENT_TYPE = "application/vnd.api.v1+json";

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDTO> handleTypeMismatch(final MethodArgumentTypeMismatchException ex) {
        // 4xx: do NOT mint a traceId — the client has the context
        // (the offending parameter name + value) and 4xx responses
        // are not paged into operator tooling.
        log.debug("400 INVALID_REQUEST — parameter '{}' value '{}' is not coercible to {}",
                ex.getName(), ex.getValue(),
                ex.getRequiredType() == null ? "<unknown>" : ex.getRequiredType().getSimpleName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType(CONTENT_TYPE))
                .body(new ErrorDTO(
                        "INVALID_REQUEST",
                        "Parameter '" + ex.getName() + "' is not a valid "
                                + (ex.getRequiredType() == null ? "value" : ex.getRequiredType().getSimpleName()),
                        null));
    }

    @ExceptionHandler(CustomerBalanceNotFoundException.class)
    public ResponseEntity<ErrorDTO> handleNotFound(final CustomerBalanceNotFoundException ex) {
        log.debug("404 CUSTOMER_NOT_FOUND — {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.parseMediaType(CONTENT_TYPE))
                .body(new ErrorDTO(
                        "CUSTOMER_NOT_FOUND",
                        ex.getMessage(),
                        null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> handleUnexpected(final Exception ex) {
        // 5xx: ALWAYS mint a traceId and log the stacktrace keyed
        // by it. The client gets the traceId so they can hand it to
        // support; the operator greps the log for it.
        final String traceId = UUID.randomUUID().toString();
        log.error("500 INTERNAL — traceId={} — unhandled exception", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.parseMediaType(CONTENT_TYPE))
                .body(new ErrorDTO(
                        "INTERNAL",
                        "An unexpected error occurred. Reference traceId for support.",
                        traceId));
    }
}
