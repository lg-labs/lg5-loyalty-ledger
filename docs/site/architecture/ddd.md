# DDD

This page explains how `loyalty-ledger` applies domain-driven design inside the
service. The goal is not to restate generic lg5 principles, but to show how the
service's business boundaries are expressed in this repository.

## Bounded context

`loyalty-ledger` owns the `loyalty` bounded context. In that context, the
service is responsible for all state required to compute and serve a customer's
loyalty balance and the immutable history behind it.

The context owns:

- movement identity and movement history
- the current balance projection per customer
- the deduplication record of inbound order events
- the business event emitted after a balance change

The context does not own:

- customer identity semantics
- order identity semantics
- how order totals are calculated upstream
- business capabilities such as loyalty expiry, manual adjustments, or tiering

That boundary is important: the service reacts to upstream order facts, but it
does not redefine them.

## Domain core

The domain core lives under `lg5-loyalty-ledger-domain/lg5-loyalty-ledger-domain-core`.
It contains the business concepts that define the ledger.

The most important aggregates are:

| Aggregate | Role |
| --- | --- |
| `Movement` | One immutable credit or debit appended to the ledger |
| `CustomerBalance` | Materialized current-balance projection for one customer |
| `ProcessedInputEvent` | Dedup and audit record for each observed inbound event |

The domain also defines the event that represents a successful balance change:

- `CustomerBalanceUpdatedEvent`

The business rules that matter most are expressed here:

- a movement delta cannot be zero
- `ORDER_PAID` produces positive deltas
- `ORDER_CANCELLED` and `ORDER_REFUNDED` produce negative deltas
- balances may become negative
- deduplication is keyed on the originating event id, not only the order id

The key architectural intent is that these rules live in the domain language of
the service, not in Kafka listeners, HTTP controllers, or JPA mappings.

## Application service

The application layer lives under
`lg5-loyalty-ledger-domain/lg5-loyalty-ledger-application-service`.

This is where the service orchestrates domain behavior across repositories,
deduplication, balance projection, and outbox storage.

The central entry point is:

- `LoyaltyLedgerInputPort`

Its main implementation is:

- `LoyaltyLedgerHandler`

This handler is where the write path becomes concrete:

1. receive a command derived from an inbound order event
2. create the dedup record
3. decide whether the event is a movement append or a no-op
4. append `Movement`
5. update `CustomerBalance`
6. stage the outbox payload for downstream publication

This layer is intentionally orchestration-heavy and transport-light. It knows
the use case, but not the Kafka listener details or HTTP controller details.

## Domain events and outbox split

`loyalty-ledger` separates domain meaning from wire-level publication.

- Inside the domain, a balance change is represented by
  `CustomerBalanceUpdatedEvent`.
- For persistence and publication, that event is transformed into
  `CustomerBalanceUpdatedEventPayload` and stored in the outbox.
- A later publisher turns that payload into the outbound Avro contract.

That split matters because the service does not treat "domain event" and
"published message" as the same thing. The outbox is part of the application
and integration model, not the domain model itself.

## Ports and adapters

The service uses ports to keep the domain/application logic independent from
transport and persistence details.

At a high level, the shape is:

- input ports for application use cases
- output ports for persistence and publication
- adapters in `data-access`, `message`, and `api`

In practical terms:

- Kafka listeners are adapters that translate inbound Avro records into
  application commands
- JPA repositories and mappers are adapters that persist domain concepts
- REST controllers are adapters that expose read models to internal callers
- Kafka publishers are adapters that emit outbound Avro messages

This keeps the central business flow readable: order events become commands,
commands become domain-side decisions, and adapters take care of transport and
storage.

## Why `CustomerBalance` is part of the model

The balance projection is not treated as a purely technical cache. It is part
of the service model because "current balance" is itself a business capability
that the service owns and must serve efficiently.

That is why the model contains both:

- the append-only `Movement` ledger for auditability
- the `CustomerBalance` projection for efficient reads

The projection does not replace the ledger; it complements it.

## Why deduplication is explicit in the model

Duplicate inbound events are not handled as an incidental messaging concern.
They are modeled explicitly through `ProcessedInputEvent` because the service
must remember which business event it has already accepted and what outcome that
event produced.

This is why the service can distinguish between:

- a repeated delivery of the same event
- a legitimate new event for the same order
- a no-op that was still intentionally processed

That distinction is central to the service's correctness.

## Module mapping

From a DDD point of view, the repository maps roughly like this:

| Area | Repository location |
| --- | --- |
| Domain model | `...-domain-core` |
| Application orchestration | `...-application-service` |
| Persistence adapters | `...-data-access` |
| Messaging adapters | `...-message-core` and `...-message-model` |
| HTTP adapters | `...-api` |
| Composition root | `...-container` |

The key thing to notice is that the business language of the service starts in
the domain and application modules, while the surrounding modules adapt it to
integration protocols and runtime wiring.

## Read next

- [Architecture overview](./index.md)
- [C4+1 Views](./c4-model.md)
- [REST](./rest.md)
- [Events](./events.md)
