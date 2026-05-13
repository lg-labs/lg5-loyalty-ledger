# Events

This page explains the asynchronous model of `loyalty-ledger`. It focuses on
how the service consumes business events, how it avoids duplicate side effects,
and how it publishes downstream balance updates in a traceable way.

## Event-driven role of the service

`loyalty-ledger` is event-driven on the write side.

It does not wait for synchronous write commands to create credits or debits.
Instead, it reacts to order lifecycle events and translates them into ledger
movements plus a downstream balance-update event.

That makes the service sit between two event concerns:

- inbound business facts from the order domain
- outbound balance-change facts for downstream consumers

## Inbound events

The service consumes three inbound topics:

| Topic | Business meaning |
| --- | --- |
| `order-paid` | a paid order may produce a credit |
| `order-cancelled` | a cancelled order may produce a debit |
| `order-refunded` | a refunded order may produce a debit |

These events are owned upstream. `loyalty-ledger` does not define their business
meaning; it consumes them as the trigger for loyalty state changes.

At integration level, Kafka listeners in `message-core` receive Avro models and
map them into application commands before the service decides whether the event
becomes a movement append or a no-op.

## Outbound event

The service publishes one outbound topic:

| Topic | Business meaning |
| --- | --- |
| `customer-balance-updated` | the customer balance changed after an appended movement |

This event is owned by `loyalty-ledger`. It is the service's public asynchronous
signal to downstream consumers that the balance changed and why.

The outbound payload carries:

- `customerId`
- `newBalance`
- `delta`
- `cause`
- `originatingOrderId`
- `originatingEventId`
- `originatingEventType`
- `occurredAt`

That is why the event is both state-oriented and traceable back to the inbound
business event that caused it.

## Idempotency and deduplication

Duplicate deliveries are expected in an event-driven system, so the service
models deduplication explicitly.

The important rule is:

- deduplication is keyed on the originating event id, not only on the order id

This means the service can distinguish between:

- the same event delivered more than once
- a legitimate new event for the same order
- a no-op event that was still intentionally processed

That distinction is persisted through `ProcessedInputEvent`, which acts as both:

- the dedup gate
- the audit record of how the service handled the inbound event

## No-op outcomes are part of the model

Not every inbound event produces a movement.

Examples:

- a paid event whose floored amount is zero
- a cancel/refund event for an order that was never credited

These are still meaningful outcomes. The service records them in the dedup/audit
model instead of pretending they never happened.

That is why the event model is more expressive than just "message received" or
"message published".

## Outbox-backed publication

The outbound event is not published directly from the same code path that
decides the business outcome.

Instead, the service uses an outbox-backed flow:

1. the application service appends the movement and updates the balance
2. it stores an outbox payload for `customer-balance-updated`
3. a scheduler reads `STARTED` outbox rows
4. a publisher maps the payload to the Avro contract and sends it to Kafka
5. the outbox row is marked `COMPLETED` or `FAILED`

This split is what lets the service keep its state change and publication intent
aligned without collapsing domain logic into messaging callbacks.

## Ordering model

The outbound Kafka key is `customerId`.

That design choice matters because it keeps all balance-update events for the
same customer on the same partition, which preserves per-customer ordering for
downstream consumers.

Inbound ordering is not globally guaranteed by the service; instead, the service
is designed to be correct under replay and duplicate delivery, with event-id-
based deduplication acting as the core safety mechanism.

## Traceability model

One of the strongest parts of the event design is that traceability is carried
through multiple layers.

The originating event is visible in:

- the movement record
- the dedup record
- the outbound balance-update payload
- the REST movement history response

That lets an engineer move from inbound event to persisted state to outbound
publication without losing the thread of causality.

## Schemas and contracts

For human readers and tooling, the asynchronous contract is documented in
`docs/api/asyncapi.yaml`.

For wire-level serialization, the canonical source is still the Avro schemas in
`lg5-loyalty-ledger-message-model`.

Use the AsyncAPI document when you need:

- topic list
- message shapes
- key semantics
- reader-facing contract overview

Use the Avro files when you need the canonical serialization contract.

## How to read the event surface

Read the event model in this order:

1. start with the inbound topics and what business situation they represent
2. understand that the service may append a movement or record a no-op
3. follow the outbox-backed path to the outbound event
4. use the traceability fields to connect the outbound event back to the
   originating order event

That sequence mirrors the real lifecycle of a business event inside the service.

## Read next

- [Events (async)](/events/)
- [Architecture overview](./index.md)
- [DDD](./ddd.md)
- [REST](./rest.md)
