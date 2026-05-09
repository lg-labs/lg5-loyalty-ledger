---
kind: adr
feature: 001-loyalty-ledger
adr-id: ADR-003
status: accepted
date: 2026-05-10
description: Idempotency of inbound order events is keyed on the originating event id, not on (orderId, eventType), enabling legitimate paid → cancelled → paid-again sequences.
---

# ADR-003 — Idempotency by originating event id (not by orderId)

- **Status:** Accepted
- **Date:** 2026-05-10
- **Deciders:** lglabs (PO), lglabs (tech lead)
- **Consulted:** —
- **Informed:** —

## Context

REQ-003, REQ-006, REQ-015, and the resolved decision Q9 in PRD §9
require that:

1. Replays of the **same** inbound event (same event id, redelivered by
   Kafka or by an upstream retry) must produce **at most one** ledger
   movement.
2. A legitimate `paid → cancelled → paid-again` sequence on the **same
   order id** must produce two credits and one debit, because the second
   `order paid` is a different business event with a different event id.

This rules out the simplest idempotency key — `(orderId, eventType)` —
because under that key, a re-payment of a previously-cancelled order
would be silently swallowed as a duplicate, contradicting Q9.

The Kafka listener pattern ships with the framework: batch listener
that swallows `OptimisticLockingFailureException` and not-found as
NO-OPs (RULE-010). What it does not ship is the **deduplication store**
itself — the framework's saga-step pattern (RULE-009) uses the outbox
row of the *previous* saga step as the dedup key. We have no saga
(ADR-001), so we must provide our own.

## Decision

We will introduce a dedicated `processed_input_event` table (in the
service's `loyalty` schema) with a **unique constraint on
`(event_type, event_id)`**. The inbound event handler will:

1. Open a single `@Transactional` boundary.
2. Attempt to `INSERT` a row into `processed_input_event` keyed by
   `(event_type, event_id)`.
3. On constraint-violation: catch, log at `DEBUG`, and return — the
   listener treats this as a NO-OP (consistent with RULE-010 NO-OP
   semantics).
4. On insert success: append the ledger movement, update the
   `customer_balance` projection (ADR-004), and persist the outbox row.
   Commit the transaction.

The row in `processed_input_event` thus serves as both the **dedup
guard** and the **trace** (it stores `originating_order_id`,
`originating_event_id`, `originating_event_type`, `received_at`,
`movement_id` if a movement was appended, or `null` if the event was a
no-op such as REQ-005).

## Alternatives considered

- **Use the outbox itself as the dedup guard** (the saga-step pattern,
  query by `(orderId, type)`). Pros: zero new table; matches the
  framework's existing pattern. Cons: directly contradicts Q9 because
  it keys on the order, not the event; and it conflates two concerns
  (inbound dedup and outbound publish) on a single table designed for
  the latter. Rejected: contradicts REQ-003 + Q9.
- **Use a Kafka-topic-level dedup based on offset commit + exactly-once
  semantics (EOS).** Pros: no DB table. Cons: depends on broker EOS
  configuration which is platform-wide and out of scope; also breaks if
  the upstream producer ever republishes (e.g. topic rebuild) with new
  offsets but the same business event id. Rejected: depends on
  cross-cutting infra not under our control.
- **Compute a deterministic UUID from `(event_type, event_id)` and use
  it as the primary key of the movement itself.** Pros: one less table.
  Cons: pollutes the domain model with infrastructure concerns; makes
  movements' PKs non-time-sortable; harder to query "movements between
  X and Y" cleanly. Rejected.

## Consequences

- **Positive:** dedup is local, transactional, and observable (a row in
  `processed_input_event` is a record of "we saw this event"). No-op
  cases (REQ-005: cancel without prior credit) are also recorded — the
  table is the single audit log of "what arrived". Replays are O(1) on
  an indexed unique constraint.
- **Negative:** one extra table to design, migrate, and maintain. One
  extra row per inbound event, even no-ops — measurable storage growth
  proportional to inbound traffic. Mitigation: archival policy is
  out-of-scope for v1 (PRD §6) but documented as future work.
- **Neutral:** the dedup is keyed at the *application* level, not at
  the *broker* level — duplicates from broker rebalancing are caught
  exactly the same way as duplicates from upstream retries. Both are
  the same business case to us.

## Constitutional impact

- **RULE-008** (outbox mandatory; `@Version` mandatory) — **confirmed.**
  The outbox row is still appended; the dedup guard does not bypass
  the outbox. The new `processed_input_event` table also carries a
  `version` column for hygiene (no concurrent updates expected, but
  RULE-008 is honored uniformly across our persistent state).
- **RULE-009** (saga step idempotent) — **N/A.** No saga steps exist
  (cf. ADR-001).
- **RULE-010** (Kafka listener: swallow `OptimisticLockingFailureException`
  + not-found as NO-OP) — **extended.** We add `DataIntegrityViolationException`
  on the `(event_type, event_id)` unique constraint to the NO-OP swallow
  list. This is *consistent with* RULE-010's intent (do not rethrow;
  do not cause Kafka redelivery loops on duplicates) but extends the
  list of swallowable exceptions. We will document this in the
  listener's class-level Javadoc and reference RULE-010 + ADR-003.

No constitutional violations.

## Implementation notes

- PRD: [`../prd.md`](../prd.md), REQs covered: REQ-003, REQ-005,
  REQ-006, REQ-014, REQ-015.
- New table: `loyalty.processed_input_event` (see `../data-model.md`).
- Touched module: `lg5-loyalty-ledger-data-access` (entity + repo +
  Flyway DDL); `lg5-loyalty-ledger-application-service` (handler).

## Related ADRs

- ADR-001 — explains why the saga-step dedup pattern is unavailable
  here.
- ADR-004 — the same transaction also updates the materialized
  customer-balance projection.

## Definition of Done (ADR)

- [x] Status is `Accepted`.
- [x] Decision stated in active voice.
- [x] At least one alternative documented (3 alternatives).
- [x] Consequences cover positive AND negative.
- [x] Constitutional impact section names every relevant `must` rule
      (RULE-008, 009, 010).
- [x] Any `must` override is time-boxed — N/A (RULE-010 is *extended*
      consistently with its intent, not overridden).
