# M4 Milestone Report — REST Read API

**Branch:** `feature/001-loyalty-ledger`
**Closed at commit:** `edc7ab0` (TASK-017)
**Final CI run:** [`25624548448`](https://github.com/lg-labs/lg5-loyalty-ledger/actions/runs/25624548448) — **43/43 ITs green** + schema-compat gate green; **0 failures**, **0 errors**, **0 skipped**.
**Reporting period:** end of M3 (`6c50fe6`) → end of TASK-017.

---

## Scope (per `plan.md` §M4)

> "M4 REST Read API — `GET /loyalty/customers/{id}/balance`,
> `GET /loyalty/customers/{id}/movements?page=&size=`, uniform error
> mapping with stable codes."

Three `tasks.md` rows:

| TASK | Title | Status |
|---|---|---|
| TASK-015 | REST controller `GET /loyalty/customers/{id}/balance` | done |
| TASK-016 | REST controller `GET /loyalty/customers/{id}/movements?page=&size=` | done |
| TASK-017 | `@RestControllerAdvice` + `ErrorDTO` mapping (400 / 404 / 500) | done |

All M4 acceptance criteria from the PRD (REQ-009, REQ-010, REQ-013,
REQ-015, RULE-005, RULE-006) are pinned by IT — controller wiring,
content type, page ordering and stability, error envelope shape, and
the read-only invariant of the query side. No deviation from spec
text; one in-flight fix-up per TASK is documented in §Decisions
below (each was a precision / contract refinement, not a scope
change).

---

## Commits (chronological)

| SHA | Subject |
|---|---|
| `4abbfa3` | `feat(TASK-015): GET /loyalty/customers/{customerId}/balance + read-side query port` |
| `7cc71c5` | `fix(TASK-015): defer 404 ErrorDTO assertion to TASK-017` |
| `a1ee314` | `feat(TASK-016): GET /loyalty/customers/{id}/movements (paged reverse-chronological)` |
| `024ebd4` | `fix(TASK-016): compare appendedAt truncated to micros (Postgres timestamptz precision)` |
| `edc7ab0` | `feat(TASK-017): @RestControllerAdvice + ErrorDTO mapping (400 / 404 / 500)` |

Five commits across three TASKs. Each TASK has exactly one `feat`
commit; the two `fix` commits are pre-merge fix-ups against the
same TASK that introduced them (RULE: never amend after push).

---

## CI runs

| Run | Trigger commit | Outcome | ITs |
|---|---|---|---|
| `25624040369` | `4abbfa3` (TASK-015 initial) | ❌ 1 failure | 35 (M3 baseline) + 3 new |
| `25624137640` | `7cc71c5` (TASK-015 fix-up) | ✅ green | 38 |
| `25624317475` | `a1ee314` (TASK-016 initial) | ❌ 1 failure | 41 |
| `25624403747` | `024ebd4` (TASK-016 fix-up) | ✅ green | 41 |
| `25624548448` | `edc7ab0` (TASK-017) | ✅ green | **43** |

The IT count progression matches the test deltas exactly:
- TASK-015: +3 (`CustomerBalanceControllerIT`).
- TASK-016: +3 (`CustomerMovementsControllerIT`).
- TASK-017: +2 (`ErrorAdviceIT`); the `CustomerBalanceControllerIT`
  third test was tightened in place rather than added (the deferred
  404 assertion promised by `7cc71c5`), so the test count holds.

---

## What got built

### TASK-015 — `GET /loyalty/customers/{id}/balance`

- New input port `LoyaltyLedgerQueryService` under
  `lg5-loyalty-ledger-application-service/.../ports/input/` with
  `getBalance(CustomerId)` returning the domain `CustomerBalance`.
- Implementation `LoyaltyLedgerQueryServiceImpl` annotated
  `@Service @Transactional(readOnly=true)` at class level — REQ-013
  (query side never mutates) is enforced by the read-only TX
  semantics.
- New application exception
  `CustomerBalanceNotFoundException extends RuntimeException` for
  the "no projection row yet" case. Lives in `application-service/
  exception/`, NOT in `domain-core/` — a fresh customer is a legal
  domain state; "no row in the DB yet" is purely a read-side
  application concern.
- Stock Spring controller `LoyaltyLedgerController` under
  `lg5-loyalty-ledger-api/.../api/rest/` with class-level
  `@RequestMapping(produces="application/vnd.api.v1+json")`
  (RULE-006) — single `produces=` keeps every method honest.
  No custom annotations (RULE-005).
- DTO record `CustomerBalanceResponse(customerId, balance,
  lastUpdatedAt)` under `<api>/dto/`.

### TASK-016 — `GET /loyalty/customers/{id}/movements?page=&size=`

- Output port `MovementLedgerRepository` extended with two
  read-only methods: `findPageByCustomer(CustomerId, int, int)`
  returning `MovementsPage` and `countByCustomer(CustomerId)`.
  Append-only contract preserved (no update / delete on the
  surface).
- `MovementJpaRepository` (which still extends `Repository<…>`,
  NOT `JpaRepository<…>`, deliberately) gains a derived-name
  finder `findByCustomerIdOrderByAppendedAtDescIdDesc(UUID,
  Pageable):Page` and `countByCustomerId(UUID):long`. The
  finder name maps **exactly** onto the existing
  `idx_movement_customer_appended (customer_id, appended_at
  DESC, id DESC)` from TASK-005 — no schema or index change
  needed for M4.
- `LoyaltyLedgerQueryService` widened with `getMovementsPage(
  CustomerId, int, int)` returning a self-contained nested
  record `MovementsPage(List<Movement> movements, int page, int
  size, long totalElements)` — controllers never need a second
  port type.
- Controller method `@GetMapping("/{customerId}/movements")` with
  `@RequestParam(defaultValue="0") int page` and
  `@RequestParam(defaultValue="20") int size`. Defensive size
  clamp `[1, 100]` (silent — the acceptance criterion does not
  require a 400 here, and clamping is the more forgiving REST
  default). Out-of-range page returns `200 OK` with empty
  `movements` and the absolute `totalElements` (NOT 404).
- DTO records `MovementResponse(id, customerId, delta, cause,
  originatingOrderId, originatingEventId, originatingEventType,
  originatingEventReceivedAt, appendedAt)` and
  `MovementsPageResponse(movements, page, size, totalElements)`.
  The originating-event traceability fields (REQ-014) are
  carried through to the wire so consumers can correlate a
  ledger row back to the inbound business event without a
  second call.

### TASK-017 — uniform error mapping

- New record `ErrorDTO(code, message, traceId)` — three fields
  by design (see Decisions §D2).
- New `LoyaltyLedgerExceptionAdvice` (`@RestControllerAdvice`,
  `@Order(Ordered.HIGHEST_PRECEDENCE)`):
  - `MethodArgumentTypeMismatchException` → `400
    INVALID_REQUEST` (covers a malformed UUID in any path
    variable / request param, no traceId on 4xx).
  - `CustomerBalanceNotFoundException` → `404
    CUSTOMER_NOT_FOUND` (no traceId on 4xx).
  - `Exception` catch-all → `500 INTERNAL` with
    `traceId = UUID.randomUUID().toString()` and the full
    stacktrace logged at ERROR keyed by the same traceId — the
    operator greps the log without leaking the raw message to
    the client.

### Test infrastructure

- New `RestBootstrap extends Lg5TestBoot` (RANDOM_PORT +
  RestAssured `requestSpecification`) sibling to the existing
  `Bootstrap extends Lg5TestBootPortNone`. Both `@Import`
  `TestContainersLoader` and their own `*DefaultMocks`
  `@TestConfiguration` registering a fallback
  `LoyaltyLedgerInputPort` Mockito mock under
  `@ConditionalOnMissingBean` — the cache-key alignment
  pattern from TASK-013 D2.
- Three new IT classes, all `@TestPropertySource`-gating only
  `testcontainers.postgres.enabled=true` (the REST read path
  doesn't touch the message layer; Kafka stays gated off):
  - `CustomerBalanceControllerIT` — 3 tests: positive balance,
    negative balance (REQ-007/008 round-trip), unknown-customer
    404 with `ErrorDTO` body.
  - `CustomerMovementsControllerIT` — 3 tests: 60-row seed across
    3 pages × 20 with no overlap and verified DESC monotonicity
    (intra-page + cross-page boundary); page=999 returns empty
    content with totalElements=60; unknown customer returns
    empty content with totalElements=0.
  - `ErrorAdviceIT` — 2 tests for the surfaces
    `CustomerBalanceControllerIT` cannot exercise end-to-end:
    malformed UUID (no mock needed), unexpected
    `RuntimeException` (induced via a `@Primary` Mockito
    override of `LoyaltyLedgerQueryService`).

---

## Decisions

### D1 — Two `Bootstrap` siblings instead of one parameterised base

`Lg5TestBoot` (RANDOM_PORT + RestAssured) and
`Lg5TestBootPortNone` (NONE web env) are mutually exclusive
choices about whether to spin up Tomcat. M2/M3 ITs run on
`Lg5TestBootPortNone` (they hit Kafka / Postgres directly and
don't need an HTTP server); M4 ITs need real HTTP, so they
extend `Lg5TestBoot`.

We could have switched all ITs to `Lg5TestBoot` to share one
base, but then the Spring TestContext cache would be polluted
by Tomcat startup for ITs that don't need it (M2/M3 = 35 ITs).
Keeping two parallel bases preserves the cache key separation
and keeps Tomcat out of the M2/M3 critical path. Both bases
ship the same fallback `LoyaltyLedgerInputPort` Mockito mock
under `@ConditionalOnMissingBean` so an IT that needs the real
write-side handler imports its own `@TestConfiguration` to win
the `@ConditionalOnMissingBean` race (the established
TASK-013 D2 pattern).

### D2 — Custom 3-field `ErrorDTO` instead of the framework's 2-field one

The framework ships `com.lg5.spring.api.rest.ErrorDTO(code,
message)`. The M4 acceptance criterion for 5xx requires a
`traceId` so an operator can correlate a client report to a
log line. Sleuth/Brave is intentionally NOT on the classpath of
this service (RULE-014 baseline) so we cannot lean on
`Tracer.currentSpan()`; we generate the traceId on the spot
via `UUID.randomUUID().toString()` and log the full stacktrace
at ERROR keyed by the same value.

We considered three alternatives:
1. **Add Sleuth and reuse the framework's `ErrorDTO`**: drags
   in a tracing dependency that this service has no other use
   for, and requires touching `pom.xml` + `application.yaml` —
   too much surface area for a single field.
2. **Echo the traceId in the `message` field of the framework's
   record**: pollutes the human-readable text and forces every
   client to parse a free-form string to extract a stable id.
3. **Ship our own record with a third field**: chosen. New
   record adds the `traceId`, keeps `code` and `message`
   contract-compatible with the framework's, and consumers
   parsing only `(code, message)` remain forward-compatible.
   Zero new dependencies.

### D3 — TASK-015 deferred the 404 body assertion to TASK-017

When TASK-015 first landed, the IT for an unknown customer
asserted only "non-2xx" with a comment that the strict 404 +
`ErrorDTO{code=CUSTOMER_NOT_FOUND}` body assertion would ship
in TASK-017 (`7cc71c5`). The reason: the
`@RestControllerAdvice` is the topic of TASK-017; landing it
inside TASK-015 would have split a single concern across two
TASKs and made the per-TASK contract muddy. The fix-up commit
in TASK-017 (`edc7ab0`) tightened the same test in place
(`getBalance_for_unknown_customer_returns_404_customerNotFound_errorDto`)
so the contract is now pinned end-to-end. No test was deleted,
and the IT count for TASK-015 stayed at 3.

### D4 — TASK-016 IT seeds via `MovementJpaRepository`, not the output port

`Movement.ofCredit` / `ofDebit` hard-code `appendedAt =
ZonedDateTime.now()`. Seeding 60 rows in a tight loop through
the production factory would yield sub-millisecond-resolution
collisions, making the DESC ordering assertion flaky on fast
hardware. Bypassing the factory and inserting `MovementJpaEntity`
straight via `MovementJpaRepository` gives the test exclusive
control over the timestamp axis (1ms spacing) without
weakening the production write path — `Movement.ofCredit`
/`ofDebit` still own the invariant on the production side.
This breaks the hexagonal boundary in test code only; the
production code path is untouched.

### D5 — TASK-016 fix-up: compare `appendedAt` truncated to micros

Postgres `timestamptz` stores microsecond precision;
`ZonedDateTime.now()` carries nanoseconds. The newest-seed
assertion compared the in-memory seed value (nanos) against
the DB round-trip (micros) and failed when `now()` happened to
land on a non-zero nano sub-microsecond fraction (CI run
`25624317475`: expected `…:02.301561383Z`, but was
`…:02.301561Z`). Fix-up commit `024ebd4` truncates both sides
to `ChronoUnit.MICROS` for a deterministic, precision-aware
equality check. The DESC-ordering and no-overlap assertions
were not affected.

### D6 — `ErrorAdviceIT` mocks the read port via `@Primary`, not `@MockBean`

`@MockBean` (Spring Boot test annotation) and a class-level
`@Primary @Bean` Mockito mock both work; we picked `@Primary`
because it lives in a `@TestConfiguration` `@Import`-ed by the
test class — the override is visible from the same
configuration block as the fallback mocks in `RestBootstrap`,
which keeps the test wiring discoverable and consistent with
the established M3 pattern. `@MockBean` would have been a
second annotation site to track. The 404 surface is NOT
duplicated in `ErrorAdviceIT` — it's pinned in
`CustomerBalanceControllerIT` against the REAL service bean,
which is the most honest mapping test we can write.

---

## Trade-offs / known limitations

- **Defensive size clamp is silent** (TASK-016): a request with
  `size > 100` is silently clamped down rather than rejected
  with `400`. The acceptance criterion does not specify a
  rejection contract, and silent clamping is the more
  forgiving REST default. If a stricter SLA emerges, switch
  to a `MethodArgumentNotValidException` mapping in the advice
  with a new `INVALID_PAGE_SIZE` code.
- **No traceId for 4xx**: the advice only mints a traceId for
  5xx. 4xx responses have enough context from `code` alone,
  and we keep the wire small. If a future need arises to
  correlate 4xx volumes per client, add the traceId to the
  4xx handlers.
- **Movements endpoint does not 404 for unknown customers**:
  per REQ-010 and standard REST practice, an empty collection
  is not a 404. The balance endpoint, by contrast, IS 404 for
  an unknown customer (REQ-009) — it returns a single resource,
  not a collection.
- **`CustomerMovementsControllerIT` breaks the hexagonal
  boundary in test code** (D4): justified by the precision
  control argument; production code path unaffected.

---

## What's left for M5 (ATDD)

Three remaining TASKs (`tasks.md` §TASK-018 / §TASK-019 /
§TASK-020):
- ATDD infra: Cucumber + Testcontainers (Postgres, Kafka,
  Schema Registry) + Wiremock.
- Two end-to-end happy-path scenarios covering the inbound +
  outbound + REST surfaces wired together.
- One failure-path scenario covering the listener-side
  swallow + outbox compensation.

M5 is the only milestone left after M4. The branch will be
merge-ready (rebase + PR) at the end of M5.
