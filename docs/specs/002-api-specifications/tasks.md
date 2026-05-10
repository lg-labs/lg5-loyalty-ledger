---
kind: tasks
feature: 002-api-specifications
version: 0.1.0
description: |
  Spec-first OpenAPI 3.1 + AsyncAPI 2.6 for the loyalty-ledger
  service, with Spectral lint in CI and contract-test wiring inside
  the existing REST ITs (drift protection, no production code change).
---

# Tasks — `002-api-specifications`

> Mini-feature without full SDD ceremonia (no PRD, no plan, no ADRs).
> 7 atomic tasks, ~1 commit each. Ordering is sequential — TASK-005
> depends on the OpenAPI authored in TASK-001; TASK-004 depends on the
> Spectral config in TASK-003.

## TASK-001 — Author `docs/api/openapi.yaml` (OpenAPI 3.1.0)

- **Status:** done
- **Scope:** REST surface for the loyalty-ledger read API.
- **Endpoints:**
  - `GET /loyalty/customers/{customerId}/balance` → 200 `CustomerBalanceResponse`, 400 `ErrorDTO`, 404 `ErrorDTO`, 405 `ErrorDTO`, 500 `ErrorDTO`.
  - `GET /loyalty/customers/{customerId}/movements?page=&size=` → 200 `MovementsPageResponse`, 400, 405, 500.
- **Schemas (mirror Java records exactly — read DTOs first):**
  - `CustomerBalanceResponse{customerId: uuid, balance: int64, lastUpdatedAt: date-time}`.
  - `MovementsPageResponse{movements: array<MovementResponse>, page: int32, size: int32, totalElements: int64}` — **no `totalPages`** (record only has 4 fields; spec must match).
  - `MovementResponse{id, customerId, delta: int32, cause: enum, originatingOrderId, originatingEventId, originatingEventType, originatingEventReceivedAt, appendedAt}` — **no `orderId`**, the field is named `originatingOrderId`; an extra `originatingEventReceivedAt` is also present.
  - `ErrorDTO{code: enum[INVALID_REQUEST,CUSTOMER_NOT_FOUND,METHOD_NOT_ALLOWED,INTERNAL], message: string, traceId: string|null}` — `traceId` is nullable (4xx are null, 5xx mint a UUID). `METHOD_NOT_ALLOWED` is a real surface (TASK-017 advice handles `HttpRequestMethodNotSupportedException`).
- **Content-Type:** `application/vnd.api.v1+json` on every response (RULE-006).
- **Path/query params:** `customerId: uuid`; `page: int (>=0, default 0)`; `size: int (>=1, default 20, max 100)`. Note: the controller **silently clamps** out-of-range `page`/`size` (does not 400) — document the bounds as the *contract* the server will enforce, not as a rejection.
- **Examples:** at least one example body per 200 / 4xx / 5xx response.
- **Validation:** `npx @stoplight/spectral-cli lint docs/api/openapi.yaml` clean.

## TASK-002 — Author `docs/api/asyncapi.yaml` (AsyncAPI 2.6.0)

- **Status:** done
- **Scope:** Kafka surface — 3 inbound consume + 1 outbound publish.
- **Channels:**
  - `order-paid` (subscribe / consume) — `OrderPaidAvroModel`.
  - `order-cancelled` (subscribe / consume) — `OrderCancelledAvroModel`.
  - `order-refunded` (subscribe / consume) — `OrderRefundedAvroModel`.
  - `customer-balance-updated` (publish) — `CustomerBalanceUpdatedAvroModel`; documents Kafka `key = customerId` via `bindings.kafka.key`.
- **Schema strategy:** embed the Avro field shape inline as JSON-Schema in `components.schemas` (AsyncAPI 2.6 + Spectral does not validate `$ref` to local `.avsc` files cleanly). Mirror each `.avsc` exactly; cite the source `.avsc` path in each schema `description`.
- **Server:** one `kafka` server, `host: '{bootstrap-servers}'` placeholder, protocol `kafka`.
- **Validation:** `npx @stoplight/spectral-cli lint docs/api/asyncapi.yaml` clean.

## TASK-003 — Spectral config (`.spectral.yaml`)

- **Status:** done
- Extend `spectral:oas` and `spectral:asyncapi` builtin rulesets.
- Repo-specific overrides:
  - Every operation must have `description` (`operation-description: error`).
  - Every channel (AsyncAPI) must have `description`.
  - Every 200-class JSON response on the OpenAPI side must declare the `application/vnd.api.v1+json` content type (`required-vendor-content-type` custom rule).
- **Validation:** running Spectral against both yamls passes.

## TASK-004 — CI job: `API specs lint`

- **Status:** done
- Add a new job `api-specs-lint` to `.github/workflows/ci.yml`.
- Same triggers as `build` (push on `main`/`feature/**` + PR to `main`).
- Use `stoplightio/spectral-action@latest` (pinned to a tag) OR `npm i -g @stoplight/spectral-cli` and run the CLI directly. We pick the npm-CLI route: explicit, pinned, no third-party action drift.
- Step: `spectral lint docs/api/openapi.yaml docs/api/asyncapi.yaml --ruleset .spectral.yaml --fail-severity=warn`.

## TASK-005 — Contract tests via `swagger-request-validator-restassured`

- **Status:** done
- Add `com.atlassian.oai:swagger-request-validator-restassured` (v2.x, scope `test`) to `lg5-loyalty-ledger-container/pom.xml`.
- New test helper `OpenApiContractFilter` in `src/test/java/.../container/api/contract/` exposing a static `RestAssured` `Filter` that validates every response against `../../docs/api/openapi.yaml` (path resolved relative to the container module working dir, with a fallback to walking up to the repo root so the helper works whether `mvn` is run from the module or the root).
- Wire `.filter(OpenApiContractFilter.openApiValidator())` into the existing `RestAssured.given(requestSpecification)` chains in `CustomerBalanceControllerIT`, `CustomerMovementsControllerIT`, and `ErrorAdviceIT`.
- **Validation:** `mvn -B -ntp -pl lg5-loyalty-ledger-container compile test-compile` resolves the dep and compiles. Full IT run is not exercised locally (no Docker on this machine); the existing `it` CI job will run it.

## TASK-006 — README `## API Specs` section

- **Status:** done
- Document the two yamls, the lint command, the contract-test mechanism, and the location of the Spectral config.

## TASK-007 — Open PR

- **Status:** done
- PR title: `feat(002-api-specifications): OpenAPI + AsyncAPI + Spectral lint + contract tests`.
- Body lists every TASK with the commit SHA, summary of what is specified, and any drift findings between the spec and the current code (the spec matches the code; mismatches found while reading the controller are listed in TASK-001's description above).
- Do NOT merge.
