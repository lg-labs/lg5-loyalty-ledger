package com.lg.platform.loyalty.api.rest;

import com.lg.platform.loyalty.api.dto.CustomerBalanceResponse;
import com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerQueryService;
import com.lg.platform.loyalty.domain.entity.CustomerBalance;
import com.lg.platform.loyalty.domain.valueobject.CustomerId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
