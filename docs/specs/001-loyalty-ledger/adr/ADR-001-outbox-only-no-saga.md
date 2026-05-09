---
kind: adr
feature: 001-loyalty-ledger
adr-id: ADR-001
status: accepted
date: 2026-05-10
description: Outbox-only emission of customer-balance-updated events; no saga participation in v1.
---

# ADR-001 — Outbox-only emission, no saga participation in v1

- **Status:** Accepted
- **Date:** 2026-05-10
- **Deciders:** lglabs (PO), lglabs (tech lead)
- **Consulted:** —
- **Informed:** —

## Context

The credit/debit operations triggered by upstream order events
(REQ-001, REQ-004) do not require any compensating write into another
service. The ledger and the customer balance are this service's source
of truth. The downstream subscribers of `customer balance updated`
(REQ-011) — promotions engine, notifications — are not transactional
peers: they consume the event eventually and react. There is **no
distributed-transaction boundary to coordinate**, so a saga
orchestrator would add machinery without buying anything.

At the same time, the publish of the outbound event MUST happen if and
only if the ledger movement was committed (REQ-011 + REQ-015 — duplicate
tolerance is end-to-end), which is exactly what the Transactional Outbox
pattern guarantees (RULE-008).

## Decision

We will use the **Transactional Outbox** pattern (RULE-008) to persist
the `customer-balance-updated` event alongside the ledger movement and
the customer-balance projection update — all in **one local
transaction**. We will **not** model the credit/debit operations as
`SagaStep<T>` instances: there is no orchestrator, no rollback path,
no second saga participant.

The standard `OutboxScheduler` (RULE-011) polls the outbox and
publishes Avro records to the `customer-balance-updated` Kafka topic.

## Alternatives considered

- **Saga with order-service compensation.** Pros: end-to-end consistency
  if loyalty fails. Cons: order-service has no business reason to know
  whether loyalty succeeded — introduces unnecessary upstream coupling
  and inverts ownership (loyalty is downstream of order, not a peer).
  Rejected because there is no business need for compensation in v1
  (PRD §6: ledger is append-only, manual corrections out of scope).
- **Direct broker publish from the `@Transactional` write path.** Pros:
  fewer moving parts (no scheduler, no outbox table). Cons: directly
  violates RULE-008 (atomicity loss between DB commit and broker
  publish — the well-known dual-write problem). Rejected: rule-blocked.
- **Event sourcing of the ledger (no projection, derive balance on
  read).** Pros: theoretically purer. Cons: balance lookups (REQ-009)
  become O(n) over the customer's history; conflicts with the p95 < 5s
  SLA on the read path; adds replay machinery this team does not need.
  Rejected (see also ADR-004 for the materialized-balance choice).

## Consequences

- **Positive:** simpler implementation; ~half as many files vs a full
  saga. Outbox is the only durability boundary the team needs to reason
  about. Idempotency is local (see ADR-003) instead of distributed.
- **Negative:** if cross-service compensation ever becomes a requirement
  (e.g. "if loyalty fails, mark the order as `LOYALTY_FAILED`"),
  migrating the credit path to a `SagaStep<T>` is non-trivial: it
  involves restructuring the application-service handler, introducing
  saga status columns on the outbox, and coordinating an orchestrator.
- **Neutral:** the outbox table still uses the standard schema with
  `OutboxStatus` and `version`, so a future migration to saga keeps the
  storage layer largely unchanged.

## Constitutional impact

- **RULE-008** (outbox mandatory) — **confirmed.** Every outbound
  `customer-balance-updated` event flows through the outbox; the JPA
  entity carries `@Version` and `OutboxStatus`.
- **RULE-009** (saga step idempotent) — **explicitly opts out.** No
  `SagaStep<T>` beans are introduced. Idempotency is enforced in the
  inbound listener via a dedicated processed-event guard (ADR-003), not
  via the saga-step `@Transactional process()` pattern. This is **not**
  an override of RULE-009 (which only constrains saga steps that *do*
  exist) — there are no saga steps in this service.
- **RULE-011** (outbox scheduler shape) — **confirmed.** A standard
  `OutboxScheduler` is implemented with `@Scheduled` + the
  `scheduling.enabled` `@ConditionalOnProperty` gate.
- **RULE-007** (Kafka payloads = Avro; key = saga id) — **clarified.**
  Avro is honored. The "key = saga id" guidance in RULE-007 was written
  for saga participants. Since this service has no saga, we use the
  **customer id** as the Kafka message key (it is the natural
  partitioning + ordering key for `customer-balance-updated`). This is a
  clarification, not an override; the rule's intent (stable, meaningful
  partitioning key) is preserved.

No constitutional violations.

## Implementation notes

- PRD: [`../prd.md`](../prd.md), REQs covered: REQ-001, REQ-004,
  REQ-011, REQ-015.
- Plan: [`../plan.md`](../plan.md).
- Commands invoked during Build: `/scaffold-service`, `/add-outbox`,
  `/add-kafka-listener`. **No** `/add-saga`.

## Related ADRs

- ADR-003 — Idempotency by originating event id (replaces the saga-guard
  pattern for our no-saga case).
- ADR-004 — Materialized customer-balance projection.
- ADR-005 — `BalanceUpdateCause` Avro enum for the outbound event.

## Definition of Done (ADR)

- [x] Status is `Accepted`.
- [x] Decision stated in active voice.
- [x] At least one alternative documented (3 alternatives).
- [x] Consequences cover positive AND negative.
- [x] Constitutional impact section names every relevant `must` rule
      (RULE-007, 008, 009, 011).
- [x] Any `must` override is time-boxed with tech-debt link — N/A
      (no overrides; only opt-outs and clarifications).
