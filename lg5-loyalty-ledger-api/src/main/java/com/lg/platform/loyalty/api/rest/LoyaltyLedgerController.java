package com.lg.platform.loyalty.api.rest;

import com.lg.platform.loyalty.api.dto.CustomerBalanceResponse;
import com.lg.platform.loyalty.api.dto.MovementResponse;
import com.lg.platform.loyalty.api.dto.MovementsPageResponse;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerQueryService;
import com.lg.platform.loyalty.domain.entity.CustomerBalance;
import com.lg.platform.loyalty.domain.entity.Movement;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Read-side REST controller for the loyalty ledger.
 *
 * <p>Stock Spring annotations only ({@code @RestController},
 * {@code @GetMapping}) — no custom framework annotations (RULE-005).
 * The class-level {@code produces = "application/vnd.api.v1+json"}
 * applies to every handler method, satisfying RULE-006 for the
 * happy-path responses; the {@code @RestControllerAdvice} from
 * TASK-017 propagates the same content type to error responses.
 *
 * <p>The controller depends only on the
 * {@link LoyaltyLedgerQueryService} input port — no JPA repository
 * is injected here, preserving the hexagonal boundary. The handler
 * method is read-only and carries no {@code @Transactional}
 * write semantics; transaction demarcation lives one layer deeper
 * on {@link com.lg.platform.loyalty.application.LoyaltyLedgerQueryServiceImpl}
 * (read-only).
 */
@Slf4j
@RestController
@RequestMapping(value = "/loyalty/customers", produces = "application/vnd.api.v1+json")
public class LoyaltyLedgerController {

    /**
     * Defensive upper bound on {@code size} for the movements
     * endpoint. A request with {@code size > MAX_PAGE_SIZE} is silently
     * clamped down (NOT rejected) — TASK-016 acceptance does not
     * require a 400 here, and clamping is the more forgiving REST
     * default. Adjust if a stricter SLA emerges.
     */
    private static final int MAX_PAGE_SIZE = 100;

    private final LoyaltyLedgerQueryService queryService;

    public LoyaltyLedgerController(final LoyaltyLedgerQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * REQ-009 / TASK-015 — current balance for one customer.
     *
     * <p>Returns {@code 200 OK} with the projection body. A malformed
     * UUID path variable surfaces as
     * {@link org.springframework.web.method.annotation.MethodArgumentTypeMismatchException}
     * which the {@code @RestControllerAdvice} maps to {@code 400}; an
     * unknown customer surfaces as
     * {@link com.lg.platform.loyalty.application.exception.CustomerBalanceNotFoundException}
     * which the same advice maps to {@code 404}.
     */
    @GetMapping("/{customerId}/balance")
    public ResponseEntity<CustomerBalanceResponse> getCustomerBalance(
            @PathVariable("customerId") final UUID customerId) {
        log.debug("GET balance for customerId={}", customerId);
        final CustomerBalance balance = queryService.getBalance(new CustomerId(customerId));
        return ResponseEntity.ok(new CustomerBalanceResponse(
                balance.getId().getValue(),
                balance.getBalance(),
                balance.getLastUpdatedAt()));
    }

    /**
     * REQ-010 / TASK-016 — paged movements in reverse-chronological
     * order ({@code appended_at DESC, id DESC}).
     *
     * <p>An out-of-range {@code page} returns {@code 200 OK} with an
     * empty {@code movements} array and the absolute
     * {@code totalElements} (NOT {@code 404}). A non-positive
     * {@code page} or {@code size} is normalised: {@code page} clamps
     * to {@code 0}, {@code size} clamps into {@code [1, MAX_PAGE_SIZE]}.
     * A {@code size > MAX_PAGE_SIZE} is silently clamped down.
     */
    @GetMapping("/{customerId}/movements")
    public ResponseEntity<MovementsPageResponse> getCustomerMovements(
            @PathVariable("customerId") final UUID customerId,
            @RequestParam(name = "page", defaultValue = "0") final int page,
            @RequestParam(name = "size", defaultValue = "20") final int size) {
        final int safePage = Math.max(0, page);
        final int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        log.debug("GET movements for customerId={} page={} size={} (normalised={}/{})",
                customerId, page, size, safePage, safeSize);

        final LoyaltyLedgerQueryService.MovementsPage pageResult =
                queryService.getMovementsPage(new CustomerId(customerId), safePage, safeSize);

        final List<MovementResponse> movementBodies = pageResult.movements().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(new MovementsPageResponse(
                movementBodies,
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements()));
    }

    private MovementResponse toResponse(final Movement movement) {
        return new MovementResponse(
                movement.getId().getValue(),
                movement.getCustomerId().getValue(),
                movement.getDelta(),
                movement.getCause(),
                movement.getOriginatingOrderId().getValue(),
                movement.getOriginatingEventId(),
                movement.getOriginatingEventType(),
                movement.getOriginatingEventReceivedAt(),
                movement.getAppendedAt());
    }
}
