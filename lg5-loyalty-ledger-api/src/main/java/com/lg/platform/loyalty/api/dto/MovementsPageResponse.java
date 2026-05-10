package com.lg.platform.loyalty.api.dto;

import java.util.List;

/**
 * REST response body for {@code GET /loyalty/customers/{customerId}/movements}
 * (TASK-016, REQ-010).
 *
 * <p>Paging contract:
 * <ul>
 *   <li>{@code page}, {@code size}: echo of the request parameters.</li>
 *   <li>{@code totalElements}: absolute count of movements for the
 *       customer (NOT the size of {@code movements}).</li>
 *   <li>{@code movements}: ordered {@code appended_at DESC, id DESC}.
 *       Empty list (NOT {@code null}) when the page is out of range
 *       or the customer has zero movements.</li>
 * </ul>
 */
public record MovementsPageResponse(List<MovementResponse> movements,
                                    int page,
                                    int size,
                                    long totalElements) {
}
