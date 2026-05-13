# C4+1 Views

This page complements the [Architecture overview](./index.md) with a small set
of practical views that make the service easier to reason about. The goal is
not to mirror every class or deployment detail, but to show the boundaries,
collaborators, and main flows that matter when you work on `loyalty-ledger`.

## System context

At system level, `loyalty-ledger` is the bounded context that translates order
lifecycle events into an auditable loyalty ledger and publishes balance updates
for downstream consumers.

```mermaid
flowchart LR
    order[Order Service]
    ledger[Loyalty Ledger]
    support[Support / Internal Callers]
    subs[Downstream Subscribers]

    order -->|order-paid\norder-cancelled\norder-refunded| ledger
    support -->|balance and movement queries| ledger
    ledger -->|customer-balance-updated| subs
```

The key architectural point in this view is ownership: `order-service` owns the
upstream order facts, while `loyalty-ledger` owns the loyalty movement history,
the current balance projection, and the downstream balance-change signal.

## Container and module view

Inside the repository, the service follows the usual lg5 split between domain,
application, transport, persistence, and composition-root concerns.

```mermaid
flowchart LR
    subgraph messaging[Message]
        listeners[Kafka listeners and mappers]
        producers[Outbox publisher and Avro mappers]
    end

    subgraph domain[Domain]
        core[domain-core\nMovement\nCustomerBalance\nProcessedInputEvent]
        app[application-service\ncommand handling\noutbox assembly]
    end

    subgraph persistence[Data Access]
        data[JPA repositories\nLiquibase\noutbox storage]
    end

    subgraph api[API]
        rest[Read-only REST controllers]
    end

    subgraph runtime[Container]
        boot[Spring Boot composition root]
    end

    listeners --> app
    app --> core
    app --> data
    app --> producers
    rest --> data
    boot --> listeners
    boot --> rest
    boot --> app
    boot --> data
```

The domain modules express business meaning, while the outer modules adapt that
meaning to HTTP, Kafka, persistence, and runtime wiring. That separation is the
main reason the service remains explainable despite handling event ingestion,
deduplication, projections, and outbound publication at the same time.

## Dynamic view: write path

The write path starts with an inbound order event and ends with a committed
movement, an updated balance projection, and a staged outbound message.

```mermaid
sequenceDiagram
    participant Kafka as Kafka topic
    participant Listener as Message listener
    participant App as Application service
    participant Dedup as ProcessedInputEvent
    participant Ledger as Movement + CustomerBalance
    participant Outbox as Outbox store
    participant Publisher as Outbox publisher

    Kafka->>Listener: order-paid / cancelled / refunded
    Listener->>App: mapped command
    App->>Dedup: insert dedup record by event id
    App->>Ledger: append movement and update balance
    App->>Outbox: store customer-balance-updated payload
    Publisher->>Kafka: publish outbound balance update
```

This is the most important flow in the system. It combines four architectural
choices that show up repeatedly across the code and specs: event-id-based
deduplication, immutable ledger append, materialized balance projection, and
outbox-backed publication.

## Dynamic view: read path

The read path is deliberately simpler: callers do not rebuild the balance by
replaying events; they read a projection for the current balance and query the
append-only ledger for movement history.

```mermaid
sequenceDiagram
    participant Caller as Internal caller
    participant API as REST controller
    participant Query as Read logic
    participant Balance as CustomerBalance projection
    participant Movement as Movement ledger

    alt Current balance
        Caller->>API: GET /customers/{id}/balance
        API->>Query: load current balance
        Query->>Balance: fetch projection row
        Balance-->>Query: balance snapshot
        Query-->>API: response DTO
        API-->>Caller: current balance
    else Movement history
        Caller->>API: GET /customers/{id}/movements?page=&size=
        API->>Query: load paged movement history
        Query->>Movement: fetch reverse-chronological page
        Movement-->>Query: paged ledger rows
        Query-->>API: response DTO
        API-->>Caller: movement page
    end
```

This split is why the service can serve fast balance reads without giving up the
append-only ledger as the auditable history behind the projection.

## How to use these views

- Start with the [Architecture overview](./index.md) if you need the narrative
  first.
- Continue to [DDD](./ddd.md) if you want to understand module boundaries and
  business concepts.
- Use [REST](./rest.md) and [Events](./events.md) when you need contract-level
  interpretation of the service interfaces.
