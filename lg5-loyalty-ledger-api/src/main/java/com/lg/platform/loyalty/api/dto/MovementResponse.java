package com.lg.platform.loyalty.api.dto;

import com.lg.platform.loyalty.domain.valueobject.BalanceUpdateCause;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * REST projection of one {@link com.lg.platform.loyalty.domain.entity.Movement}
 * row (TASK-016, REQ-010 + REQ-014).
 *
 * <p>Carries the originating-event traceability fields (REQ-014) so
 * that downstream consumers of the read API can correlate a movement
 * back to the inbound business event that produced it without a
 * second round-trip.
 *
 * @param id                          movement id (UUID)
 * @param customerId                  owner of the movement
 * @param delta                       signed delta (positive = credit,
 *                                    negative = debit)
 * @param cause                       {@code ORDER_PAID},
 *                                    {@code ORDER_CANCELLED}, or
 *                                    {@code ORDER_REFUNDED}
 * @param originatingOrderId          REQ-014 traceability — the
 *                                    business order that originated
 *                                    this ledger entry
 * @param originatingEventId          REQ-014 — id of the inbound event
 * @param originatingEventType        REQ-014 — type name of the
 *                                    inbound event
 * @param originatingEventReceivedAt  timestamp the inbound event was
 *                                    accepted by the listener
 * @param appendedAt                  timestamp the movement was
 *                                    written to the ledger (REQ-010
 *                                    sort key, descending)
 */
public record MovementResponse(UUID id,
                               UUID customerId,
                               int delta,
                               BalanceUpdateCause cause,
                               UUID originatingOrderId,
                               UUID originatingEventId,
                               String originatingEventType,
                               ZonedDateTime originatingEventReceivedAt,
                               ZonedDateTime appendedAt) {
}
