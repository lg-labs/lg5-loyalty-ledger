# FAQ

This page answers the questions that come up most often when someone starts
working on `lg5-loyalty-ledger`.

It stays intentionally short. When a topic needs full detail, the answer links
to the deeper page or source artifact instead of duplicating it.

## What kind of service is `lg5-loyalty-ledger`?

`lg5-loyalty-ledger` is the service that records loyalty point movements and
serves read-side views of customer balances and movement history.

It is event-driven on the write side and HTTP read-only on the query side.

Read next:

- [Architecture overview](/architecture/)
- [DDD](/architecture/ddd)
- [Events](/architecture/events)
- [REST](/architecture/rest)

## Why is the HTTP API read-only?

Because ledger writes are driven by inbound business events, not by direct REST
commands.

The REST surface exposes projections that let clients read:

- the current balance of a customer
- the ordered movement history of that customer

The canonical HTTP contract is `docs/api/openapi.yaml`.

Read next:

- [API (sync)](/api/)
- [REST](/architecture/rest)

## Which business events does the service consume?

The service consumes three inbound events:

- `OrderPaid`
- `OrderCancelled`
- `OrderRefunded`

Those events produce ledger movements and update the customer balance
projection. After a movement is accepted, the service emits one outbound
`CustomerBalanceUpdated` event.

The canonical async contract is `docs/api/asyncapi.yaml`.

Read next:

- [Events (async)](/events/)
- [Events](/architecture/events)

## Why does the service publish through an outbox instead of directly from the consumer?

Because the service wants the ledger write and the outbound publication trigger
to be part of one durable flow.

The design uses the Transactional Outbox pattern so the movement append,
projection update, and outbound publication hand-off stay coordinated without
requiring a distributed transaction.

Read next:

- [Events](/architecture/events)
- [ADR landing page](/adr/)
- `docs/specs/001-loyalty-ledger/adr/ADR-001-outbox-only-no-saga.md`

## Why is there no saga in v1?

Because this service is not orchestrating a multi-step business transaction in
v1.

Its responsibility is narrower:

- consume upstream order business events
- apply ledger effects idempotently
- publish the resulting balance-updated event

That decision is explicit in the ADR set.

Read next:

- [ADR landing page](/adr/)
- `docs/specs/001-loyalty-ledger/adr/ADR-001-outbox-only-no-saga.md`

## How does the service avoid duplicate ledger writes when Kafka redelivers messages?

It deduplicates inbound processing using the originating event identity before
applying any side effect.

In the docs and specs, this is described as idempotency keyed by the inbound
event id / message id, depending on the contract surface you are reading.

Read next:

- [Events](/architecture/events)
- [ADR landing page](/adr/)
- `docs/specs/001-loyalty-ledger/adr/ADR-003-idempotency-by-event-id.md`

## Why does `GET /loyalty/customers/{customerId}/balance` return `404`, while movements can return an empty `200` page?

Because the two endpoints model different read concerns.

For `balance`, the API treats the missing projection row as a missing resource,
so it returns `404`.

For `movements`, an empty history is still a valid collection result, so the API
returns `200` with `movements = []` and `totalElements = 0`.

This distinction is documented in the OpenAPI contract and reflected in the REST
documentation.

Read next:

- [REST](/architecture/rest)
- [API (sync)](/api/)

## Can a customer balance be negative?

Yes.

The OpenAPI contract explicitly documents that balances may be negative, and the
examples show a negative balance response.

Read next:

- [API (sync)](/api/)
- `docs/api/openapi.yaml`

## Where should I look first: `docs/site/`, `docs/specs/`, or the contracts under `docs/api/`?

Use them for different jobs:

- `docs/site/` is the reader-friendly technical entrypoint
- `docs/specs/` contains the SDD source material, planning context, and ADRs
- `docs/api/` contains the canonical OpenAPI and AsyncAPI contracts

If you need the shortest path to understanding, start with `docs/site/`.
If you need exact requirements or decisions, move into `docs/specs/`.
If you need exact wire contract details, use `docs/api/`.

Read next:

- [QuickStart](/quickstart/)
- [Architecture overview](/architecture/)
- [ADR landing page](/adr/)

## What is the fastest way to get the repo working locally?

Use the `QuickStart` page.

The short version is:

```bash
git clone --recurse-submodules git@github.com:lg-labs/lg5-loyalty-ledger.git
cd lg5-loyalty-ledger
./.agent-os/scripts/install.sh
make install-skip-test
make run-integration-test
make docs-install
make docs-preview-local
```

Read next:

- [QuickStart](/quickstart/)
- [Onboarding Runbook](/runbook/)

## Which docs page should I read if I only need one starting point?

If you are a contributor, start with [QuickStart](/quickstart/).

If you need technical orientation, start with [Architecture overview](/architecture/).

If you need exact protocol details, start with [API (sync)](/api/) or
[Events (async)](/events/).

## Where are the architectural decisions documented?

The canonical ADR files live under `docs/specs/<feature>/adr/`.

The docs site provides a curated landing page that groups and summarizes them.

Read next:

- [ADR landing page](/adr/)

## How should I interpret the published Swagger UI and AsyncAPI viewer?

Treat them as convenient rendered views of the contracts, not as the only source
of truth.

When precision matters:

- use `docs/api/openapi.yaml` for the HTTP contract
- use `docs/api/asyncapi.yaml` for the event contract
- use the ADRs and architecture pages for the surrounding design intent

Read next:

- [API (sync)](/api/)
- [Events (async)](/events/)
- [REST](/architecture/rest)
- [Events](/architecture/events)
