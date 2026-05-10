package com.lg.platform.loyalty.application.exception;

import com.lg.platform.loyalty.domain.valueobject.CustomerId;

/**
 * Thrown by the read side of the application service
 * ({@link com.lg.platform.loyalty.application.ports.input.LoyaltyLedgerQueryService})
 * when a {@code customer_balance} row is requested for a customer that
 * has never received a credit or debit (the row is created lazily on
 * first event — see {@code data-model.md §CustomerBalance}).
 *
 * <p>Mapped by the API layer's {@code @RestControllerAdvice} (TASK-017)
 * to HTTP {@code 404 Not Found} with
 * {@code ErrorDTO{code=CUSTOMER_NOT_FOUND, ...}}. The exception
 * intentionally lives in {@code application-service} (not in the
 * domain) because "the customer projection has no row yet" is a
 * read-side application concern, not a domain invariant — the
 * domain considers an absent {@code CustomerBalance} a perfectly
 * legal state (a fresh customer).
 */
public class CustomerBalanceNotFoundException extends RuntimeException {

    private final CustomerId customerId;

    public CustomerBalanceNotFoundException(final CustomerId customerId) {
        super("No customer_balance row for customerId=" + customerId.getValue());
        this.customerId = customerId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }
}
