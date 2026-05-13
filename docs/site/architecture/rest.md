# REST

This page explains the synchronous HTTP surface of `loyalty-ledger`. The goal
is not to duplicate the OpenAPI document, but to explain why the REST API is
shaped the way it is and how to interpret it correctly.

## Why the HTTP surface is read-only

`loyalty-ledger` is not a command API for loyalty operations. The service is
driven by inbound business events from the order domain, and its public HTTP
surface exists to expose read models derived from that event stream.

That means the REST side of the service is intentionally limited to two queries:

- current balance for one customer
- paged movement history for one customer

Credits and debits are not created through HTTP requests. They are created by
processing `order-paid`, `order-cancelled`, and `order-refunded` events.

## Resources

The service exposes two resources under the `loyalty` path space:

| Resource | Purpose |
| --- | --- |
| `GET /loyalty/customers/{customerId}/balance` | Read the current projected balance |
| `GET /loyalty/customers/{customerId}/movements` | Read a reverse-chronological page of immutable ledger movements |

The first resource is projection-oriented. The second resource is ledger-
oriented.

## Endpoint behavior

### `GET /loyalty/customers/{customerId}/balance`

This endpoint returns the current `CustomerBalance` projection for one customer.

Important semantics:

- `200` means a balance row exists and is returned
- `404` means no balance projection exists for that customer yet
- `400` means the path variable is not a valid UUID
- `500` means an unexpected internal failure occurred

The balance returned by this endpoint may be negative. That is intentional and
matches the business rule that debits are allowed even when they push the
customer below zero.

### `GET /loyalty/customers/{customerId}/movements`

This endpoint returns a page from the immutable movement ledger.

Important semantics:

- rows are ordered `appendedAt DESC, id DESC`
- an unknown customer is not treated as a `404`; it returns `200` with an empty
  page
- an out-of-range page is also `200`, not an error
- `page` and `size` are normalized server-side instead of rejected when outside
  expected bounds

This behavior treats the endpoint as a collection query rather than as a lookup
for a single required entity.

## Media type and versioning

All success and error responses use the vendor media type:

`application/vnd.api.v1+json`

That is a deliberate contract choice inherited from the lg5 conventions used by
the service. The important thing for consumers is that the API is versioned at
the media-type level, not only by URL naming.

## Response shapes

At a high level, the REST surface exposes three kinds of DTOs:

- a current-balance response
- a movement item response
- a paged movement response

The movement response preserves business traceability fields such as:

- `originatingOrderId`
- `originatingEventId`
- `originatingEventType`
- `originatingEventReceivedAt`

That is why the API is more than a balance number: it also exposes the audit
trail needed to explain why the balance changed.

## Error model

The service uses a consistent error DTO shape for REST failures.

The important error cases are:

- `INVALID_REQUEST` for malformed path input such as a non-UUID customer id
- `CUSTOMER_NOT_FOUND` when the balance projection does not exist
- `METHOD_NOT_ALLOWED` when callers use a non-GET verb on a read-only resource
- `INTERNAL` when an unexpected server-side failure occurs

The `INTERNAL` path additionally carries a server-generated `traceId`, which is
useful for support and operational correlation.

## Why balance and movements behave differently on missing data

The two endpoints do not treat missing data in exactly the same way.

- `balance` returns `404` when the customer projection row does not exist
- `movements` returns `200` with an empty collection when there are no ledger
  entries

This distinction is intentional.

The balance endpoint behaves like a projection lookup: either the current state
exists or it does not. The movements endpoint behaves like a collection query:
an empty result is still a valid answer.

## Contract-first enforcement

The OpenAPI file is not documentation-only. In this repository, it is treated as
an executable contract.

REST integration tests use an OpenAPI validation filter so that drift between
controller behavior and `docs/api/openapi.yaml` fails the build. That gives the
service a strong contract-first feedback loop:

- the spec describes the surface
- the tests enforce the surface
- documentation can safely point readers to the spec as the contract source of
  truth

## How to read the OpenAPI contract

Use the OpenAPI document when you need exact request/response shapes, response
codes, examples, and schema details.

Use this page when you need to understand:

- why the API is read-only
- what business meaning each endpoint carries
- why missing data is handled differently across resources
- how the REST layer relates to the projection and the immutable ledger

## Read next

- [API (sync)](/api/)
- [Architecture overview](./index.md)
- [DDD](./ddd.md)
- [Events](./events.md)
