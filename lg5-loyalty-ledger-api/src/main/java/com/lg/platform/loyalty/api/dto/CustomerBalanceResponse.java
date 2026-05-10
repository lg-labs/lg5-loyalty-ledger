package com.lg.platform.loyalty.api.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * REST response body for {@code GET /loyalty/customers/{customerId}/balance}
 * (TASK-015, REQ-009).
 *
 * <p>1:1 projection of {@code customer_balance} (data-model.md
 * §CustomerBalance). Field order matches the implicit alphabetical
 * Jackson default — readers should NOT depend on field order in JSON.
 *
 * @param customerId    canonical customer identifier (UUID).
 * @param balance       current point balance, may be negative (REQ-007).
 * @param lastUpdatedAt timestamp of the last credit/debit applied.
 */
public record CustomerBalanceResponse(UUID customerId,
                                      long balance,
                                      ZonedDateTime lastUpdatedAt) {
}
