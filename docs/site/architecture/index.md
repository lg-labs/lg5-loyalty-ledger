# Architecture

`lg5-loyalty-ledger` is the service that owns the platform's loyalty-point
ledger for each customer. It listens to order lifecycle events, turns them into
 immutable ledger movements, maintains a materialized balance projection, and
publishes a downstream business event every time the balance changes.

This page is the entry point for the technical architecture of the service. Use
it to understand what the service is responsible for, where its boundaries are,
and how the main write and read flows are organized.

## Service purpose

The service exists to answer a simple business question with auditable data:
how many loyalty points does a customer have, and why did the balance change?

It does that by combining three responsibilities:

- ingest order lifecycle events (`order-paid`, `order-cancelled`,
  `order-refunded`)
- append immutable loyalty movements linked to the originating business event
- expose read models for current balance and movement history

The service also emits a `customer-balance-updated` event so other services can
react without reading the ledger database directly.

## Responsibilities and non-goals

The service is responsible for:

- converting paid orders into loyalty credits
- converting cancellations and refunds into loyalty debits when a prior credit
  exists
- tolerating duplicate inbound events through event-id-based deduplication
- allowing negative balances when debits exceed the current balance
- exposing read-only APIs for current balance and movement history
- publishing a downstream balance-update event through an outbox-backed flow

The service is not responsible for:

- owning customer identity or order identity
- deciding how upstream order totals are calculated
- manual balance adjustments in v1
- loyalty tiering, expiry, or promotion rules
- direct integration contracts for other services beyond its documented REST
  and event surfaces

## System role

`loyalty-ledger` sits between the upstream order domain and downstream
subscribers that care about balance changes.

- Upstream: `order-service` publishes the business events that drive credits and
  debits.
- Core: `loyalty-ledger` validates, deduplicates, persists, projects, and
  publishes.
- Downstream: other services consume `customer-balance-updated` to react to the
  new customer balance.

Within the broader lg5 ecosystem, the service follows the usual split between
domain, application, API, data-access, messaging, container, acceptance-test,
and support concerns, but its behavior is defined by this repository's specs,
contracts, ADRs, and code.

## Module boundaries

The repository is organized as a Maven multi-module service:

| Module | Purpose |
| --- | --- |
| `lg5-loyalty-ledger-domain/` | Business core: domain objects plus application-service orchestration |
| `lg5-loyalty-ledger-api/` | Read-only HTTP API for balances and movement history |
| `lg5-loyalty-ledger-data-access/` | JPA repositories, persistence mappings, Liquibase changelog |
| `lg5-loyalty-ledger-message/` | Kafka listeners, producers, and Avro-facing mapping |
| `lg5-loyalty-ledger-container/` | Spring Boot composition root and runtime wiring |
| `lg5-loyalty-ledger-acceptance-test/` | Acceptance-test suite |
| `lg5-loyalty-ledger-support/` | Local infrastructure support |

The most important architectural rule is that the domain core stays focused on
ledger concepts and invariants, while the application and infrastructure layers
handle orchestration, persistence, messaging, and transport-specific concerns.

## High-level write flow

The write path is event-driven.

1. A Kafka listener receives an order event.
2. The application layer maps it into a service command.
3. A dedup record is inserted using the originating event id as the identity
   key.
4. If the event should change balance, the service appends a `Movement` and
   updates the `CustomerBalance` projection in the same transaction.
5. The service stores an outbox message representing
   `customer-balance-updated`.
6. A separate outbox publisher emits the outbound event.

This design keeps the ledger append, balance projection, and outbound publish
intent aligned while still preserving an immutable movement history.

## High-level read flow

The read path is intentionally simpler than the write path.

1. An internal caller requests either current balance or movement history.
2. The API layer delegates to read-oriented application/data-access logic.
3. Current balance comes from the materialized `CustomerBalance` projection.
4. Movement history comes from the immutable `Movement` ledger, ordered from
   newest to oldest and paged.

This split keeps reads fast without sacrificing the append-only ledger as the
auditable source behind the projection.

## Why this shape matters

Several of the service's business requirements pull in different directions:

- the ledger must be immutable
- the balance query must be fast
- duplicate events must not create duplicate side effects
- downstream subscribers must receive traceable balance-change events

The current architecture resolves those tensions by combining:

- an append-only movement ledger
- a materialized customer-balance projection
- event-id-based deduplication
- an outbox-backed outbound publication flow

Those are the core ideas to keep in mind as you move through the deeper
architecture pages.

## Read next

- [C4+1 Views](./c4-model.md)
- [DDD](./ddd.md)
- [REST](./rest.md)
- [Events](./events.md)
- [API (sync)](/api/)
- [Events (async)](/events/)
- [ADRs](/adr/)
