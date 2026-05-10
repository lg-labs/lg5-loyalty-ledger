---
kind: adr
feature: 001-loyalty-ledger
adr-id: ADR-004
status: accepted
date: 2026-05-10
description: Materialize the customer balance as a separate projection (customer_balance) updated in the same transaction as the ledger append, so REQ-009 (get-balance) is O(1).
---

# ADR-004 — Materialized customer-balance projection

- **Status:** Accepted
- **Date:** 2026-05-10
- **Deciders:** lglabs (PO), lglabs (tech lead)
- **Consulted:** —
- **Informed:** —

## Context

REQ-009 requires "get current balance for a customer" as a non-mutating
read. REQ-010 requires "list movements, paged, reverse-chronological"
also as a non-mutating read. The success metric in PRD §4 sets
`p95 < 5s` end-to-end for the *write* path and "≥ 99.9% lookups
served successfully" for the read path; while it does not pin a read
latency target explicitly, the implied target is "fast enough that
support agents (PRD §3) do not notice".

The naive approach — derive the balance on demand by summing all
movements for that customer — is O(n) over the customer's history. For
a low-volume customer this is fine; for a high-volume customer this
becomes a tail-latency problem and a hot-path SQL on the `movement`
table.

The ledger is append-only (REQ-013). Movements are immutable. This
makes a materialized projection of the balance a safe and natural
optimization: it never disagrees with the ledger as long as it is
updated in the same transaction.

## Decision

We will maintain a separate JPA entity `CustomerBalance` (table
`loyalty.customer_balance`, one row per customer) holding `customer_id`
(PK), `balance` (`bigint`), and `version` (optimistic locking).

The application-service handler updates this row **in the same
`@Transactional` boundary** that appends the movement and persists the
outbox event. The order of operations within that transaction is:

1. `INSERT` into `processed_input_event` (ADR-003 dedup guard).
2. `INSERT` into `movement` (the immutable ledger row).
3. `UPSERT` into `customer_balance` (`balance += delta`, increment
   `version`).
4. `INSERT` into `outbox` with the `customer-balance-updated` payload
   carrying the **post-update** `balance` value (REQ-011).

Reads (REQ-009, REQ-010) hit only `customer_balance` and `movement`
respectively; they never touch outbox or `processed_input_event`.

## Alternatives considered

- **Derive balance on read** (`SELECT SUM(delta) FROM movement WHERE
  customer_id = ?`). Pros: zero projection storage; "single source of
  truth" purity. Cons: O(n) reads as customer history grows; hot-path
  SQL; the `movement` table needs an index on `customer_id` anyway for
  REQ-010, but a sum-over-all-history query is still wasteful.
  Rejected.
- **Materialize via Postgres trigger or materialized view.** Pros:
  decouples the projection from application code. Cons: hides logic in
  the database (against RULE-003 spirit — domain logic in the domain);
  triggers are hard to test without a real DB; materialized views need
  a refresh strategy. Rejected.
- **Maintain the projection in a separate eventual-consistency loop**
  (e.g. consume the outbound event in another process and update a
  read store). Pros: textbook CQRS. Cons: adds a window of staleness
  (Q5: read clients are internal services that may chain reads after
  a write — they expect read-after-write consistency); adds operational
  surface (a new consumer to monitor); over-engineered for v1.
  Rejected.

## Consequences

- **Positive:** O(1) balance reads. Read-after-write consistency for
  callers that read after a successful write commit. The projection is
  *guaranteed* in sync with the ledger because the same transaction
  writes both.
- **Negative:** the write path now updates two tables instead of one.
  Tiny added latency; not material at expected volumes. The
  `customer_balance` table can be re-derivable from `movement` at any
  time (a recovery script can `TRUNCATE customer_balance` and rebuild
  by SUM), so the projection is **not** a second source of truth — the
  ledger remains authoritative.
- **Neutral:** the upsert uses `ON CONFLICT (customer_id) DO UPDATE`
  in raw SQL, or two-step "find else insert" via Spring Data; either
  approach is fine. Choice deferred to TASK-NNN in `tasks.md`.

## Constitutional impact

- **RULE-003** (hexagonal-DDD; domain depends on nothing Spring) —
  **confirmed.** The `CustomerBalance` aggregate (and the `Movement`
  ledger entry) live in `lg5-loyalty-ledger-domain-core` as pure POJOs
  with invariants. The JPA entity is a **separate** class in
  `lg5-loyalty-ledger-data-access` with a mapper to/from the domain
  type. The Postgres-specific upsert lives in the data-access adapter.
- **RULE-008** (outbox mandatory; `@Version` mandatory) — **confirmed.**
  Both `customer_balance` and the outbox carry `@Version`. The outbox
  row is appended in the same transaction as the projection update.
- **RULE-016** (DDD blocks come from `lg5-common-domain`) —
  **confirmed.** `CustomerBalance` extends `AggregateRoot`;
  `Movement` extends `BaseEntity`; identifiers are `BaseId<UUID>`
  subclasses; monetary values use the `Money` value object.

No constitutional violations.

## Implementation notes

- PRD: [`../prd.md`](../prd.md), REQs covered: REQ-007, REQ-009,
  REQ-010, REQ-011 (post-update balance in payload).
- Schema: `loyalty` (Postgres), tables `movement`, `customer_balance`,
  `processed_input_event`, `outbox`. Full DDL in
  [`../data-model.md`](../data-model.md).
- Touched modules: `lg5-loyalty-ledger-domain-core`,
  `lg5-loyalty-ledger-data-access`,
  `lg5-loyalty-ledger-application-service`.

## Related ADRs

- ADR-003 — same transaction also performs the dedup guard.
- ADR-001 — confirms the outbox is the cross-boundary publish path.

## Definition of Done (ADR)

- [x] Status is `Accepted`.
- [x] Decision stated in active voice.
- [x] At least one alternative documented (3 alternatives).
- [x] Consequences cover positive AND negative.
- [x] Constitutional impact section names every relevant `must` rule
      (RULE-003, 008, 016).
- [x] Any `must` override is time-boxed — N/A (no overrides).
