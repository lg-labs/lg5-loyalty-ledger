---
kind: data-model
feature: 001-loyalty-ledger
version: 0.1.0
description: Concrete data shapes (aggregates, domain events, outbox payloads, REST DTOs, Avro schemas, JPA tables) for loyalty-ledger v1.
---

# Data model — `loyalty-ledger`

> Companion to [`plan.md`](plan.md). Captures the **concrete shapes**
> the implementation will use. References:
> [`prd.md`](prd.md) §5 (REQs), [`adr/ADR-001..005`](adr/).

## Bounded context

`loyalty` — owns *all* state required to compute and serve a customer's
loyalty point balance and the immutable history of how that balance
evolved. The context owns:

- the **identity of movements** (their UUID, their order, their cause);
- the **derived projection** of each customer's current balance;
- the **dedup record** of every inbound order event the service has
  observed (whether it produced a movement or was a no-op).

The context does **not** own:

- the identity of `customer_id` (it is an opaque UUID assigned by the
  upstream identity service);
- the identity of `order_id` (assigned by `order-service`);
- the meaning of `paid_amount` beyond "monetary units the customer
  paid for that order, as published in the inbound `order paid` Avro
  contract" (cf. ADR-002, PRD Q7).

## Aggregates & entities (`lg5-loyalty-ledger-domain-core`)

All aggregates extend `AggregateRoot` (RULE-016, from
`com.labs.lg.pentagon:ddd-common-domain` re-exported by
`lg5-common-domain`). All entities extend `BaseEntity`. Identifiers
extend `BaseId<UUID>`. Monetary values use the `Money` value object.

### `Movement` (aggregate root)

Represents one immutable ledger entry — a credit or a debit applied to
one customer at one point in time, originated by exactly one inbound
business event.

| Field                      | Type                  | Notes |
|----------------------------|-----------------------|-------|
| `id`                       | `MovementId`          | PK; `BaseId<UUID>`. |
| `customerId`               | `CustomerId`          | `BaseId<UUID>`. |
| `delta`                    | `int`                 | signed; positive = credit, negative = debit. **Invariant:** `delta != 0` (REQ-002 / Q1). |
| `cause`                    | `BalanceUpdateCause`  | enum: `ORDER_PAID`, `ORDER_CANCELLED`, `ORDER_REFUNDED`. |
| `originatingOrderId`       | `OrderId`             | `BaseId<UUID>`; from the inbound event. |
| `originatingEventId`       | `UUID`                | event id from the inbound event header (REQ-014). |
| `originatingEventType`     | `String`              | mirror of `cause` for diagnostic ease. |
| `originatingEventReceivedAt` | `ZonedDateTime`     | server time at which the listener accepted the inbound event. |
| `appendedAt`               | `ZonedDateTime`       | server time at which the movement was committed. |
| `version`                  | `int`                 | optimistic locking (RULE-008). Movements are immutable so this never bumps; declared for uniformity with the rest of the persistent state. |

**Invariants:**

- `delta != 0` (zero-credit case is skipped before instantiation; REQ-002 / Q1).
- `cause == ORDER_PAID` ⇔ `delta > 0` (no debits on a paid event).
- `cause ∈ {ORDER_CANCELLED, ORDER_REFUNDED}` ⇔ `delta < 0`.

**Behavior:**

- Constructor only; no mutating methods. The aggregate is born complete.
- Static factory `Movement.ofCredit(customer, order, eventId, eventType, eventReceivedAt, delta)` and `Movement.ofDebit(...)` enforce the cause/sign invariant.

### `CustomerBalance` (aggregate root)

Materialized projection of a customer's current point balance
(ADR-004). One row per customer; created on first credit/debit, never
deleted.

| Field         | Type           | Notes |
|---------------|----------------|-------|
| `customerId`  | `CustomerId`   | PK. |
| `balance`     | `long`         | signed; may be negative (REQ-007, REQ-008). |
| `lastUpdatedAt` | `ZonedDateTime` | server time of last update. |
| `version`     | `int`          | optimistic locking (RULE-008). |

**Invariants:**

- `balance` is the algebraic sum of all `Movement.delta` for `customerId` (recoverable: a maintenance job could `TRUNCATE customer_balance` and re-derive from `movement`).

**Behavior:**

- `applyDelta(int delta)` — adds `delta` to `balance`, bumps `version`. The only mutator. Never throws on negative balance (REQ-007).
- `static empty(CustomerId)` — initial state for a customer first seen.

### `ProcessedInputEvent` (aggregate root)

The dedup guard + audit record of every inbound business event the
service has observed (ADR-003). One row per `(eventType, eventId)`,
inserted before any side-effect; uniqueness is the dedup mechanism.

| Field                       | Type           | Notes |
|-----------------------------|----------------|-------|
| `id`                        | `ProcessedInputEventId` | `BaseId<UUID>`. |
| `originatingEventId`        | `UUID`         | unique together with `originatingEventType`. |
| `originatingEventType`      | `String`       | one of `OrderPaid`, `OrderCancelled`, `OrderRefunded`. |
| `originatingOrderId`        | `OrderId`      | for diagnostics + REQ-014 trace. |
| `originatingCustomerId`     | `CustomerId`   | denormalized; supports per-customer audit queries. |
| `receivedAt`                | `ZonedDateTime` | server time of insert. |
| `outcome`                   | `ProcessedInputEventOutcome` | enum: `MOVEMENT_APPENDED`, `NOOP_ZERO_CREDIT`, `NOOP_DEBIT_WITHOUT_CREDIT`. |
| `movementId`                | `MovementId?`  | nullable; non-null iff `outcome == MOVEMENT_APPENDED`. |
| `version`                   | `int`          | optimistic locking (RULE-008). |

**Invariants:**

- `(originatingEventType, originatingEventId)` is unique → DB-level unique constraint.
- `outcome == MOVEMENT_APPENDED` ⇔ `movementId != null`.

**Behavior:** factories per outcome; no mutating methods.

## Domain events (`lg5-loyalty-ledger-domain-core`)

Pure POJOs (no Spring, no Avro, no JSON), implementing `DomainEvent`
from `lg5-common-domain` (RULE-016).

| Event                          | Payload                                                                                                | When raised |
|--------------------------------|--------------------------------------------------------------------------------------------------------|-------------|
| `CustomerBalanceUpdatedEvent`  | `customerId, newBalance, delta, cause, originatingOrderId, originatingEventId, originatingEventType, occurredAt` | After a `Movement` is appended and the `CustomerBalance` is updated, in the application-service handler. |

(REQ-005 no-op cases and REQ-002/Q1 zero-credit cases do **not** raise
this event; they only insert the dedup row.)

## Outbox payloads (`lg5-loyalty-ledger-application-service`)

RULE-008: outbox payload is **distinct** from the domain event. The
payload is the JSON wire shape stored in `outbox.payload` (`jsonb` at
the DDL level); the producer mapper converts it to Avro
(`CustomerBalanceUpdatedAvroModel`) at publish time.

| Payload                              | Fields                                                                                                       | Source event |
|--------------------------------------|--------------------------------------------------------------------------------------------------------------|--------------|
| `CustomerBalanceUpdatedEventPayload` | `customerId, newBalance, delta, cause, originatingOrderId, originatingEventId, originatingEventType, occurredAt` | `CustomerBalanceUpdatedEvent` |

Notes:
- `cause` is serialized as a string (the enum symbol name) for
  forward-compatibility with the Avro enum's evolution.
- The mapping `domain event → payload` is a 1:1 field copy in v1; the
  separation exists to honor RULE-008 and to absorb future divergence.

## REST DTOs (`lg5-loyalty-ledger-api`)

Java records. Controllers produce `application/vnd.api.v1+json`
(RULE-006). Read-only operations (REQ-009, REQ-010); no request bodies
needed in v1 because there are no write endpoints (PRD §6: no manual
corrections).

| DTO                          | Purpose                                | Fields |
|------------------------------|----------------------------------------|--------|
| `CustomerBalanceResponse`    | response of `GET /customers/{id}/balance` | `customerId: UUID, balance: long, lastUpdatedAt: ZonedDateTime` |
| `MovementResponse`           | element of the movements page          | `id: UUID, delta: int, cause: String, originatingOrderId: UUID, originatingEventType: String, appendedAt: ZonedDateTime` |
| `MovementsPageResponse`      | response of `GET /customers/{id}/movements?page=&size=` | `movements: List<MovementResponse>, page: int, size: int, totalElements: long, totalPages: int` |
| `ErrorDTO`                   | standard error body (re-use lg5-spring's) | `code: String, message: String, traceId: String` |

Endpoints (locked here, refined in `tasks.md`):

| Method | Path                                       | Returns                  | Covers REQ |
|--------|--------------------------------------------|--------------------------|------------|
| `GET`  | `/loyalty/customers/{customerId}/balance`  | `CustomerBalanceResponse` | REQ-009    |
| `GET`  | `/loyalty/customers/{customerId}/movements?page={page}&size={size}` | `MovementsPageResponse` (reverse-chronological) | REQ-010    |

## Avro schemas (`lg5-loyalty-ledger-message-model`)

### Inbound (consumed; **owned by `order-service`**, see ADR-002)

| Topic              | Schema (reused from `order-service-message-model`) | Compatibility |
|--------------------|----------------------------------------------------|---------------|
| `order-paid`       | `OrderPaidAvroModel`                               | `BACKWARD` (registry-side) |
| `order-cancelled`  | `OrderCancelledAvroModel`                          | `BACKWARD` |
| `order-refunded`   | `OrderRefundedAvroModel`                           | `BACKWARD` |

We assume each upstream record carries at minimum:
`messageId: string (uuid)`, `orderId: string (uuid)`,
`customerId: string (uuid)`, `paidAmount: bytes (decimal)` (for
`OrderPaidAvroModel` only — cancel/refund need only the order id),
`createdAt: long (timestamp-millis)`. Exact field set is pinned in the
build by the upstream Maven dependency version (ADR-002).

### Outbound (produced; **owned by us**, see ADR-005)

#### `BalanceUpdateCause.avsc` (enum)
```json
{
  "type": "enum",
  "name": "BalanceUpdateCause",
  "namespace": "com.lg.platform.loyalty.kafka.avro.model",
  "symbols": ["ORDER_PAID", "ORDER_CANCELLED", "ORDER_REFUNDED", "UNKNOWN"],
  "default": "UNKNOWN"
}
```

The `UNKNOWN` symbol + `default` enables forward-compatibility for
future causes (consumers built before the new symbol parse it as
`UNKNOWN` instead of erroring; cf. ADR-005 negative consequence
mitigation).

#### `CustomerBalanceUpdatedAvroModel.avsc` (record)
```json
{
  "type": "record",
  "name": "CustomerBalanceUpdatedAvroModel",
  "namespace": "com.lg.platform.loyalty.kafka.avro.model",
  "fields": [
    { "name": "messageId",            "type": { "type": "string", "logicalType": "uuid" } },
    { "name": "customerId",           "type": { "type": "string", "logicalType": "uuid" } },
    { "name": "newBalance",           "type": "long" },
    { "name": "delta",                "type": "int" },
    { "name": "cause",                "type": "BalanceUpdateCause" },
    { "name": "originatingOrderId",   "type": { "type": "string", "logicalType": "uuid" } },
    { "name": "originatingEventId",   "type": { "type": "string", "logicalType": "uuid" } },
    { "name": "originatingEventType", "type": "string" },
    { "name": "occurredAt",           "type": { "type": "long", "logicalType": "timestamp-millis" } }
  ]
}
```

| Topic                       | Schema                              | Compatibility | Key                  |
|-----------------------------|-------------------------------------|---------------|----------------------|
| `customer-balance-updated`  | `CustomerBalanceUpdatedAvroModel`   | `BACKWARD`    | `customerId` (string-uuid) |

The Kafka **key** is `customerId`, not `sagaId` (no saga in this
service; see ADR-001 RULE-007 clarification). This guarantees that all
events for one customer land on the same partition and are consumed
in order.

## JPA tables (`lg5-loyalty-ledger-data-access`)

Schema name: `loyalty` (no quoting needed; not a reserved word).

### `loyalty.movement`

| Column                          | Type                          | Constraints |
|---------------------------------|-------------------------------|-------------|
| `id`                            | `uuid`                        | PK |
| `customer_id`                   | `uuid`                        | not null |
| `delta`                         | `integer`                     | not null; check `delta <> 0` |
| `cause`                         | `loyalty_cause` (Postgres ENUM: `ORDER_PAID`, `ORDER_CANCELLED`, `ORDER_REFUNDED`) | not null; mapped via JPA `@Enumerated(EnumType.STRING)` (framework cast convention) |
| `originating_order_id`          | `uuid`                        | not null |
| `originating_event_id`          | `uuid`                        | not null |
| `originating_event_type`        | `varchar(64)`                 | not null |
| `originating_event_received_at` | `timestamptz`                 | not null |
| `appended_at`                   | `timestamptz`                 | not null; default `now()` |
| `version`                       | `integer`                     | not null; default `0` |

Indexes:

- PK: `(id)`.
- `idx_movement_customer_appended` on `(customer_id, appended_at DESC, id DESC)` — supports REQ-010 reverse-chronological paging with stable ordering on ties.
- `idx_movement_originating_order` on `(originating_order_id)` — supports REQ-004 lookup of the prior credit's amount when computing the debit.

### `loyalty.customer_balance`

| Column           | Type           | Constraints |
|------------------|----------------|-------------|
| `customer_id`    | `uuid`         | PK |
| `balance`        | `bigint`       | not null; default `0` |
| `last_updated_at`| `timestamptz`  | not null; default `now()` |
| `version`        | `integer`      | not null; default `0` |

Indexes: PK only. Negative `balance` values are not constrained
(REQ-007).

### `loyalty.processed_input_event`

| Column                    | Type           | Constraints |
|---------------------------|----------------|-------------|
| `id`                      | `uuid`         | PK |
| `originating_event_id`    | `uuid`         | not null |
| `originating_event_type`  | `varchar(64)`  | not null |
| `originating_order_id`    | `uuid`         | not null |
| `originating_customer_id` | `uuid`         | not null |
| `received_at`             | `timestamptz`  | not null; default `now()` |
| `outcome`                 | `processed_input_outcome` (Postgres ENUM: `MOVEMENT_APPENDED`, `NOOP_ZERO_CREDIT`, `NOOP_DEBIT_WITHOUT_CREDIT`) | not null |
| `movement_id`             | `uuid`         | nullable; FK → `movement(id)` (deferred — single-Tx insert order makes the FK satisfiable) |
| `version`                 | `integer`      | not null; default `0` |

Indexes:

- PK: `(id)`.
- **Unique** `uq_processed_event_type_id` on `(originating_event_type, originating_event_id)` — the **dedup mechanism** (ADR-003). A duplicate inbound event triggers a `DataIntegrityViolationException` here, which the listener swallows as NO-OP.

### `loyalty.outbox` (RULE-008 standard shape)

Standard outbox columns from the framework:

| Column          | Type                     | Constraints |
|-----------------|--------------------------|-------------|
| `id`            | `uuid`                   | PK |
| `saga_id`       | `uuid`                   | not null — **reused as a *correlation* id**; we set it to `originating_event_id` so the outbox row is traceable back to the source. (Cf. ADR-001 RULE-007 clarification.) |
| `type`          | `varchar(64)`            | not null; constant `"CustomerBalanceUpdated"` for v1. |
| `payload`       | `jsonb`                  | not null; JSON of `CustomerBalanceUpdatedEventPayload`. JPA-side type is `String`; cast handled by the framework's standard outbox config. |
| `outbox_status` | `outbox_status` (Postgres ENUM: `STARTED`, `COMPLETED`, `FAILED`) | not null; defaults to `STARTED`. |
| `created_at`    | `timestamptz`            | not null; default `now()` |
| `version`       | `integer`                | not null; default `0` |

Indexes:

- PK: `(id)`.
- `idx_outbox_status_created` on `(outbox_status, created_at)` — supports the scheduler's "fetch STARTED rows in order" query.

## Idempotency strategy (RULE-009 / RULE-010 honored as follows)

RULE-009 (saga step idempotent) — **not applicable**: no saga steps
exist in this service (ADR-001).

RULE-010 (Kafka listener: do not rethrow on optimistic-lock /
not-found; NO-OP) — **honored and extended** (ADR-003) with
`DataIntegrityViolationException` on the
`uq_processed_event_type_id` unique constraint. Algorithm in the
inbound handler (one method per event type, all sharing the same
helper):

```text
@Transactional
void handle(<inbound>EventAvroModel event):
    try:
        processedRepo.insert(ProcessedInputEvent.of(event, outcome=PENDING))
    catch DataIntegrityViolationException:
        log.debug("duplicate inbound event {}/{}; NO-OP", type, eventId)
        return                                # NO-OP swallow (RULE-010 + ADR-003)

    delta := computeDelta(event)              # may be 0 → outcome=NOOP_ZERO_CREDIT
    if delta == 0 and event is OrderPaid:
        processed.outcome = NOOP_ZERO_CREDIT
        return                                # commit; no movement, no outbox

    if event is OrderCancelled|Refunded
       and not movementRepo.existsCreditFor(orderId):
        processed.outcome = NOOP_DEBIT_WITHOUT_CREDIT
        log.warn("debit without prior credit for order {}", orderId)   # REQ-005
        return                                # commit; no movement, no outbox

    movement := Movement.of...(event, delta)
    movementRepo.save(movement)
    processed.outcome = MOVEMENT_APPENDED
    processed.movementId = movement.id

    balance := balanceRepo.findById(customerId).orElse(CustomerBalance.empty(customerId))
    balance.applyDelta(delta)
    balanceRepo.save(balance)

    outboxRepo.save(Outbox.of(
        sagaId        = event.eventId,        // correlation id
        type          = "CustomerBalanceUpdated",
        payload       = toJson(CustomerBalanceUpdatedEventPayload.from(...))
    ))
```

The outbox scheduler (`lg5-loyalty-ledger-application-service`) polls
`outbox` for `STARTED` rows, publishes the Avro record, and marks
`COMPLETED` (or `FAILED` with a retry policy outside the scope of this
PRD).

## Definition of Done (Data model)

- [x] Every aggregate has at least one invariant declared. _(Movement: `delta != 0` + sign; CustomerBalance: balance == sum of deltas; ProcessedInputEvent: unique key.)_
- [x] `version` field present on every aggregate that is updated (RULE-008). _(All four tables carry `version`.)_
- [x] Domain events are pure POJOs (no Spring, no Avro). _(`CustomerBalanceUpdatedEvent` in `domain-core`.)_
- [x] Outbox payload distinct from domain event. _(`CustomerBalanceUpdatedEventPayload` in `application-service` ≠ `CustomerBalanceUpdatedEvent` in `domain-core`.)_
- [x] Every Kafka topic has an Avro schema with explicit compatibility mode. _(All `BACKWARD`; inbound owned by `order-service`, outbound owned here.)_
- [x] Idempotency strategy stated for every consumer-side handler. _(One handler shape; ADR-003 dedup table.)_
