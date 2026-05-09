---
kind: adr
feature: 001-loyalty-ledger
adr-id: ADR-002
status: accepted
date: 2026-05-10
description: Reuse order-service-message-model for inbound Avro contracts (OrderPaid, OrderCancelled, OrderRefunded) instead of redeclaring schemas locally.
---

# ADR-002 — Reuse `order-service-message-model` for inbound Avro contracts

- **Status:** Accepted
- **Date:** 2026-05-10
- **Deciders:** lglabs (PO), lglabs (tech lead)
- **Consulted:** order-service team (assumed; ownership of the schemas)
- **Informed:** —

## Context

This service consumes three upstream business events from
`order-service`: order-paid, order-cancelled, order-refunded
(REQ-001, REQ-004, PRD §3 system role). All three must be deserialized
as Avro `SpecificRecordBase` records (RULE-007).

The producing service, `order-service`, already publishes its events
with Avro schemas defined in its own `order-service-message-model`
Maven module (`food-ordering-system` reference, RULE-018). That module
is published as a regular Maven artifact and is consumable as a
dependency.

Two paths exist:

1. **Reuse** the upstream `order-service-message-model` as a Maven
   dependency in `lg5-loyalty-ledger-message-model`. Generated Avro
   classes are imported as-is.
2. **Redeclare** the three schemas locally inside
   `lg5-loyalty-ledger-message-model/src/main/resources/avro/` with
   identical content (duplicate `.avsc` files).

## Decision

We will **reuse** `order-service-message-model` as a Maven dependency in
`lg5-loyalty-ledger-message-model`. The three inbound topics
(`order-paid`, `order-cancelled`, `order-refunded`) deserialize to the
upstream's generated `SpecificRecordBase` classes directly. We do **not**
redeclare any of those schemas locally.

For the **outbound** event (`customer-balance-updated`) we own the
schema and it lives in our own `lg5-loyalty-ledger-message-model`
(see ADR-005 for the cause-enum aspect).

## Alternatives considered

- **Redeclare schemas locally.** Pros: zero coupling on `order-service`'s
  release cadence; "self-contained module". Cons: schema divergence is
  inevitable over time (someone adds a field upstream and forgets here);
  duplicated maintenance; a deserialization mismatch will surface as a
  consumer-side runtime error in production rather than a build-time
  classpath conflict. Rejected: the cost of staying in sync manually
  outweighs the coupling cost.
- **Use a shared "platform-events" module hosted in a third repo.**
  Pros: one source of truth for ALL platform events. Cons: introduces a
  third repo and release cycle that does not exist today; over-engineered
  for a 3-event feature; deferable. Rejected for v1; revisit if a third
  consumer of these events appears.

## Consequences

- **Positive:** zero schema duplication; we get upstream's evolution for
  free (subject to BACKWARD-compatibility, RULE-007); a build-time
  bump of the upstream dependency is the only "sync" action needed.
- **Negative:** versioning coupling — bumping
  `order-service-message-model` may force a recompile here even if the
  bumped fields are irrelevant to us. Mitigated by pinning a known good
  version in our `pom.xml` and only bumping intentionally.
- **Neutral:** the topic names and partitioning keys are still our own
  configuration (`<svc>-service.kafka.consumer.order-paid-topic-name`
  etc.); reuse is at the schema level, not at the broker-config level.

## Constitutional impact

- **RULE-007** (Kafka payloads = Avro; schemas in `*-message-model`) —
  **confirmed.** All inbound payloads are `SpecificRecordBase`.
  "Schemas live in `*-message-model`" is honored *transitively*: they
  do live in a `*-message-model` module — just `order-service`'s, which
  is consumed as a Maven dependency by ours. This is a clarification of
  the rule, not an override.
- **RULE-018** (reference projects) — **confirmed.** This is exactly
  the reuse pattern shown in `food-ordering-system`'s
  `payment-service` ↔ `order-service-message-model` consumption.

No constitutional violations.

## Implementation notes

- PRD: [`../prd.md`](../prd.md), REQs covered: REQ-001, REQ-004
  (input contracts).
- Maven dependency to add in `lg5-loyalty-ledger-message-model/pom.xml`:
  `com.lg.platform:order-service-message-model:<known-good-version>`
  (exact GA version pinned in Plan).
- Topics (consumed): `order-paid`, `order-cancelled`, `order-refunded`.
  Topic names are properties; only the schema classes are reused.

## Related ADRs

- ADR-005 — Outbound `BalanceUpdateCause` Avro enum (the part of the
  contract we *do* own).

## Definition of Done (ADR)

- [x] Status is `Accepted`.
- [x] Decision stated in active voice.
- [x] At least one alternative documented (2 alternatives).
- [x] Consequences cover positive AND negative.
- [x] Constitutional impact section names every relevant `must` rule
      (RULE-007, 018).
- [x] Any `must` override is time-boxed — N/A (no overrides).
