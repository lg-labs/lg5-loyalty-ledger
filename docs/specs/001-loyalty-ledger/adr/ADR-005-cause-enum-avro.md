---
kind: adr
feature: 001-loyalty-ledger
adr-id: ADR-005
status: accepted
date: 2026-05-10
description: Outbound customer-balance-updated event distinguishes ORDER_PAID, ORDER_CANCELLED, ORDER_REFUNDED via an Avro enum field "cause".
---

# ADR-005 — `BalanceUpdateCause` Avro enum on the outbound event

- **Status:** Accepted
- **Date:** 2026-05-10
- **Deciders:** lglabs (PO), lglabs (tech lead)
- **Consulted:** —
- **Informed:** downstream subscriber owners (notifications, marketing)

## Context

Resolved decision Q8 in PRD §9 establishes that `cancellation` and
`refund` are **distinct business causes** from this service's point of
view, even though the *debit logic* is identical. REQ-011 explicitly
requires the published `customer balance updated` event to carry a
`cause` field, and REQ-012 requires traceability to the originating
order event type.

The Avro contract for the outbound event must therefore enumerate the
three legal causes; a free-form `string` would lose enforcement and
allow drift over time.

## Decision

We will declare an Avro `enum` named `BalanceUpdateCause` in
`lg5-loyalty-ledger-message-model/src/main/resources/avro/`, with
exactly three symbols: `ORDER_PAID`, `ORDER_CANCELLED`,
`ORDER_REFUNDED`. The `customer-balance-updated` Avro record will
include a non-nullable field `cause: BalanceUpdateCause`.

Schema-registry compatibility mode for both schemas is **BACKWARD**
(consumers can be upgraded after producers). Adding new enum symbols in
the future requires either a `default` symbol (Avro 1.9+) or a major
version bump of the consumer; we accept this trade-off because adding a
new business cause is a deliberate evolution, not a routine change.

## Alternatives considered

- **Use a `string` field with documented allowed values.** Pros:
  Avro-trivial; lazy evolution. Cons: no schema-level enforcement; a
  typo in a producer build (or in a future field added by an unrelated
  team) silently emits invalid data; consumers must defensively validate.
  Rejected.
- **Use three separate Avro records / topics**, one per cause
  (`order-paid-credited`, `order-cancelled-debited`,
  `order-refunded-debited`). Pros: each event self-describes its
  semantic. Cons: triples the number of consumer subscriptions for
  downstream services that care about *all* balance changes (the
  expected case for notifications and marketing); fragments the
  contract for what is fundamentally one business fact ("balance
  changed"). Rejected.
- **Make `cause` an `int` enum** (numeric encoding). Pros: smallest
  wire size. Cons: opaque in logs; brittle to reordering; no
  human-readable diagnostics. Rejected.

## Consequences

- **Positive:** strong schema-level enforcement of valid causes;
  human-readable in logs and ad-hoc inspection; downstream consumers
  exhaustively pattern-match on a known set.
- **Negative:** adding a fourth cause (e.g. `ORDER_PARTIALLY_REFUNDED`
  in v2) requires a coordinated rollout with subscribers if no
  `default` symbol was set up front. Mitigation: declare a `default`
  symbol on the enum from day one (`UNKNOWN`) so older consumers can
  parse new values gracefully.
- **Neutral:** the cause is also persisted on the `movement` row and
  on the `outbox.payload` blob; the same value flows from JPA → Avro
  → broker without conversion.

## Constitutional impact

- **RULE-007** (Kafka payloads = Avro; schemas in `*-message-model`) —
  **confirmed.** The enum and the wrapping record live in
  `lg5-loyalty-ledger-message-model/src/main/resources/avro/`.
  Compatibility mode (`BACKWARD`) is documented in the schema
  registration step of the Plan.
- **RULE-008** (outbox mandatory; payload distinct from domain event) —
  **confirmed.** The Avro record `CustomerBalanceUpdatedAvroModel` is
  the **wire shape**; the domain event `CustomerBalanceUpdatedEvent`
  in `lg5-loyalty-ledger-domain-core` carries the same data as a pure
  POJO. The outbox payload is the JSON representation of the domain
  event; the producer mapper converts JSON → Avro at publish time.

No constitutional violations.

## Implementation notes

- PRD: [`../prd.md`](../prd.md), REQs covered: REQ-004, REQ-008,
  REQ-011, REQ-012.
- Schemas to add: `BalanceUpdateCause.avsc` (enum) and
  `CustomerBalanceUpdatedAvroModel.avsc` (record referencing the
  enum). Concrete contents in [`../data-model.md`](../data-model.md).
- Module: `lg5-loyalty-ledger-message-model`.
- Build target: `make run-avro-model` regenerates the Java sources.

## Related ADRs

- ADR-002 — inbound contracts are owned by `order-service`; only the
  outbound contract (this ADR) is owned here.
- ADR-001 — outbox is the publish path.

## Definition of Done (ADR)

- [x] Status is `Accepted`.
- [x] Decision stated in active voice.
- [x] At least one alternative documented (3 alternatives).
- [x] Consequences cover positive AND negative.
- [x] Constitutional impact section names every relevant `must` rule
      (RULE-007, 008).
- [x] Any `must` override is time-boxed — N/A (no overrides).
