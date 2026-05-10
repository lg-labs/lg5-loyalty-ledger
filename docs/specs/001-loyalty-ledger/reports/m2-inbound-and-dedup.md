# M2 Milestone Report — Inbound + Dedup

**Branch:** `feature/001-loyalty-ledger`
**Closed at commit:** `a450f0e` (TASK-011 docs backfill)
**Final CI run:** [`25620422467`](https://github.com/lg-labs/lg5-loyalty-ledger/actions/runs/25620422467) — **34/34 ITs green**, **0 failures**, **0 errors**, **0 skipped**.
**Reporting period:** PR #1 to lg5-spring → end of TASK-011.

---

## Scope (per `plan.md` §M2)

> "M2 Inbound + dedup — listener wiring (×3); helper + dedup guard;
> domain-event raise; mapper Avro→domain."

Five `tasks.md` rows:

| TASK | Title | Status |
|---|---|---|
| TASK-008 | Mapper inbound Avro → `LoyaltyLedgerCommand` | done (closed in M1, included for graph completeness) |
| TASK-009 | `OrderPaid` Kafka listener + testcontainer wiring | done |
| TASK-010 | `OrderCancelled` Kafka listener + IT | done |
| TASK-010b | `OrderRefunded` Kafka listener + IT | done |
| TASK-011 | Application-service handler (dedup + movement + balance + outbox) | done |

All M2 acceptance criteria from the PRD (REQ-001 through REQ-008,
REQ-011..REQ-015 inbound paths) verified at the IT layer; no manual
deviation from the spec apart from TASK-011 Case H (see §Decisions
below).

---

## Commit chain (M2 only — 12 commits)

```
a450f0e docs(TASK-011): backfill commit 371b163 + CI run 25620422467 (34 ITs green); flip status done; drop Case H
371b163 feat(TASK-011): application-service handler — dedup gate + movement append + balance update + outbox payload persist (7 cases)
d9f9f72 feat(TASK-010b): add OrderRefundedKafkaListener + IT (REQ-004, REQ-005)
7c7f8ea fix(TASK-010): drop per-IT consumer-group overrides — they fragment the Spring TestContext cache and break SR container reuse
5beaf94 feat(TASK-010): add OrderCancelledKafkaListener + IT (REQ-004, REQ-005)
1c664bb docs(TASK-009): credit framework SHA d0d754a + final commit chain in completion note
304e433 test(TASK-009): drop SR readiness workaround now that framework fix landed
0b1bb5a fix(TASK-009): add com.lg5.spring.kafka + .outbox to scanBasePackages
fc11ab3 chore(framework-bump): pin lg5-spring af81c7c → d0d754a (Kafka testcontainer in-network listener fix)
6668ae9 fix(TASK-009): bump SR readiness timeout to 180s + dump SR container logs on failure
5329103 fix(TASK-009): unblock unrelated ITs + wait for schema-registry readiness
0ed7ca2 feat(TASK-009): @KafkaListener for order-paid topic + Kafka testcontainer wiring
```

Twelve commits across 5 TASKs (one `feat`, plus targeted `fix`/`docs`
follow-ups). No `--no-verify`, no `--amend` of pushed commits, no force
push.

---

## Cross-repo dependency: lg5-spring PR #1

TASK-009 surfaced two framework-level defects in the Kafka
testcontainer + schema-registry wiring:

1. The Kafka container's `KAFKA_ADVERTISED_LISTENERS` was missing the
   `PLAINTEXT://kafka:19092` in-network alias, so containers sharing
   the same Docker network (Schema Registry, app under test) could not
   resolve the broker by hostname.
2. `ConfluentKafkaContainerCustomConfig.waitingFor(...)` was called
   twice; the second invocation silently shadowed the first, dropping
   the SR `/subjects` HTTP probe and leaving the container declared
   "ready" before SR was actually accepting requests.

Fixed in [lg5-spring PR #1](https://github.com/lg-labs-pentagon/lg5-spring/pull/1)
(squash-merged → main `d0d754a`). Service consumed via
`fc11ab3 chore(framework-bump): pin lg5-spring af81c7c → d0d754a`.
Side-effect: `agent-os` released `v0.3.2` (main `6937f31`) to update
the `lg5-spring` skill snapshot.

---

## Architecture delivered (M2 surface)

### Inbound flow (per inbound event)

```
order-{paid,cancelled,refunded} (Kafka, Avro)
        │
        ▼
{Order…}KafkaListener  ─── @KafkaListener(batch-listener: true)
        │                    catches: OptimisticLockingFailureException,
        │                             DataIntegrityViolationException,
        │                             not-found exceptions  →  NO-OP (RULE-010)
        ▼
inbound mapper        ─── Avro record → LoyaltyLedgerCommand (sealed)
        │                    no Spring annotations (RULE-005)
        ▼
LoyaltyLedgerHandler  ─── @Service @Transactional
        │                    pattern-match on sealed command
        │
        ├─► ProcessedInputEventRepository.save(...)        ◄── dedup gate
        │       (uq_processed_event_type_id raises DIVE on replay)
        │
        ├─► MovementLedgerRepository.save(Movement.of{Credit,Debit})
        │
        ├─► CustomerBalanceRepository.save(balance.applyDelta(±N))
        │
        └─► OutboxRepository.save(OutboxMessage.started(...))
                payload = JSON(CustomerBalanceUpdatedEventPayload)
```

### Per-command behaviour

| Command | Floor / precondition | Outcome |
|---|---|---|
| `OrderPaid` | `floor(paidAmount EUR) == 0` (e.g. 0.50) | `NOOP_ZERO_CREDIT`, no movement, no outbox |
| `OrderPaid` | `floor(paidAmount EUR) > 0` | credit + balance + outbox |
| `OrderCancelled` / `OrderRefunded` | no prior credit on order | `NOOP_DEBIT_WITHOUT_CREDIT` + WARN, no movement, no outbox |
| `OrderCancelled` / `OrderRefunded` | prior credit exists | debit of `Σ positive deltas for orderId` + balance + outbox |
| any | replay (same `eventId`) | `DataIntegrityViolationException` propagates → txn rollback → listener swallows |

### New surface added in M2

**Listener layer** (`lg5-loyalty-ledger-message-core`):
- `OrderPaidKafkaListener`, `OrderCancelledKafkaListener`,
  `OrderRefundedKafkaListener` — three `@Component`s, batch listeners,
  dispatch to `LoyaltyLedgerInputPort`.
- `MessagingBeansConfig` — registers the RULE-005-clean inbound
  mappers as `@Bean`s.

**Application-service layer** (`lg5-loyalty-ledger-application-service`):
- `LoyaltyLedgerHandler @Service @Transactional` — sealed-command
  pattern-match implementation of `LoyaltyLedgerInputPort`.
- `outbox/payload/CustomerBalanceUpdatedEventPayload` — Jackson record
  (`@JsonInclude(NON_NULL)`); `cause` emitted as **string symbol** per
  the RULE-008 wire-vs-domain split.
- `outbox/model/OutboxMessage.started(...)` factory.
- `ports/output/repository/MovementLedgerRepository.sumPositiveDeltaForOrder(...)`
  — new port method (sized debit equals sum of credit deltas; v1 has
  no partial cancel/refund per PRD §Out-of-scope).

**Data-access layer** (`lg5-loyalty-ledger-data-access`):
- `MovementJpaRepository` gained two SELECT-only methods:
  `@Query sumPositiveDeltaForOrder` (`COALESCE(...,0)`) and
  `findByOriginatingOrderIdOrderByAppendedAtAsc` (test-only; REQ-013
  append-only surface preserved — no `update*`/`delete*` exposed).
- `MovementLedgerRepositoryImpl` impl of the new port method.

**Test infrastructure** (`lg5-loyalty-ledger-container/src/test`):
- `TestContainersLoader` extended with
  `ConfluentKafkaContainerCustomConfig` (gated by
  `testcontainers.kafka.enabled`, default `false` so existing
  data-access ITs are unaffected per RULE-013).
- `Bootstrap.DefaultMocks @ConditionalOnMissingBean LoyaltyLedgerInputPort`
  Mockito fallback so non-application ITs are not broken by the
  eagerly-wired listener `@Component`s. The real `LoyaltyLedgerHandler`
  shipped in TASK-011 wins the gate in production and in the
  application-layer ITs.
- `Lg5LoyaltyLedgerApplication.scanBasePackages` extended with
  `com.lg5.spring.kafka` + `com.lg5.spring.outbox` so the framework's
  `KafkaConsumerConfig @Configuration` registers the
  `kafkaListenerContainerFactory` bean. Without this addition,
  `@KafkaListener` silently failed to register (root cause of two CI
  failures during TASK-009).

### IT count delta (M1 → M2)

| At end of | ITs green | Delta |
|---|---|---|
| M1 (TASK-008 `8827590`) | 24 | — |
| TASK-009 `1c664bb` | 25 | +1 (`OrderPaidKafkaListenerIT`) |
| TASK-010 `7c7f8ea` | 26 | +1 (`OrderCancelledKafkaListenerIT`) |
| TASK-010b `d9f9f72` | 27 | +1 (`OrderRefundedKafkaListenerIT`) |
| TASK-011 `a450f0e` | **34** | **+7** (`LoyaltyLedgerHandlerHappyPathIT` ×4 + `LoyaltyLedgerHandlerEdgeCasesIT` ×3) |

Runtime: M2 final CI Integration-tests job 2m35s, of which the three
listener ITs share a single Spring context and a single Kafka +
Schema-Registry container set (the `kafka:19092` network alias is
preserved across the IT class series).

---

## Decisions

### D1 — Per-IT `@TestPropertySource` overrides break Spring TestContext caching

**Symptom (TASK-010, run `25620010798`):** `OrderCancelledKafkaListenerIT`
red on first push. Per-IT `loyalty-ledger-service.consumer-groups.order-{paid,cancelled}=…-listener-it`
overrides made each IT's Spring context cache key unique → second
context refresh raced a disappearing `kafka:19092` network alias → SR
`/subjects` probe `Connection reset`.

**Decision:** unify `@TestPropertySource` keys across all listener ITs
so they share one TestContext (and therefore one container set).
Offset-bleed concerns are moot — fresh container = fresh broker.

**Commit:** `7c7f8ea fix(TASK-010): drop per-IT consumer-group overrides`.
**Result:** TASK-010 + TASK-010b + TASK-011 all green on first push
after the unification.

### D2 — Dedup-row outcome chosen up-front (no write-then-update)

The handler picks one of three `ProcessedInputEvent.forX(...)`
factories (`forMovementAppended`, `forNoopZeroCredit`,
`forNoopDebitWithoutCredit`) at the branch point, **before** any
movement/balance/outbox write. This avoids a same-txn
write-then-update on the dedup row, which would have required either
a second JPA flush or a custom UPSERT, and would have made
`@Version`-based optimistic-lock semantics on the dedup row
ill-defined.

### D3 — Debit magnitude = `sumPositiveDeltaForOrder(orderId)`

V1 has no partial cancel or refund (PRD §Out-of-scope). The compensating
debit therefore equals the sum of all positive deltas previously
recorded for the order. Realised through a new dedicated port method
(`MovementLedgerRepository.sumPositiveDeltaForOrder`) backed by a
JPQL `COALESCE(SUM(...),0)`. The alternative (scanning the customer's
movements) was rejected as wider in scope than necessary and harder to
make append-only-safe.

### D4 — Replay propagates `DataIntegrityViolationException` from the handler

The `data-model.md §Idempotency strategy` says the handler returns
without rethrowing on replay. The actual implementation **propagates**
the exception so the `@Transactional` boundary rolls back atomically;
the listener layer catches and swallows it as NO-OP per RULE-010 (this
swallow is verified at the listener layer in TASK-009/010/010b ITs,
not in the handler ITs). The spec text was updated in `a450f0e` to
reflect the propagation contract.

### D5 — TASK-011 Case H dropped

The acceptance text describes "balance +5 → debit -12 → -7" — but the
v1 public input-port API cannot produce this state. Any +12 debit
requires a prior +12 credit on the same order (REQ-005), which would
push the balance to +17 before the cancel.

**Resolution (Option (c) of three options surfaced):** drop Case H
from the IT, mark it dropped with a one-line note in the acceptance
list, and re-route REQ-007 / REQ-008 coverage in the matrix:

| REQ | Was | Now |
|---|---|---|
| REQ-007 | TASK-003, **TASK-006**, TASK-011 (Case H), TASK-019 | TASK-003, **TASK-006 (`+100, -150, +50` → `-50` mid-sequence)**, TASK-019 |
| REQ-008 | TASK-006, TASK-011 (Case H), TASK-015, TASK-019 | TASK-006, TASK-011 (Case D outbox payload `newBalance=0`), TASK-015, TASK-019 |

REQ-007 (negative balance allowed) is fully covered at the
closer-to-invariant repo layer by `CustomerBalanceRepositoryIT`. No
production code change required.

### D6 — Framework bump to `1.0.0-alpha.d0d754a` (lg5-spring PR #1)

See §"Cross-repo dependency" above. Net effect on the service: SR
readiness workaround (180s polling + container log dump) removed in
`304e433`; no further test instability observed across TASK-010,
TASK-010b, TASK-011.

---

## REQ → coverage matrix at end of M2

(Subset relevant to M2; full matrix in `tasks.md` §Coverage matrix.)

| REQ | Where covered (M2) |
|---|---|
| REQ-001 | TASK-009 (`OrderPaidKafkaListenerIT`), TASK-011 Case A |
| REQ-002 / Q1 | TASK-011 Case A (floor 12.95→12), Case B (floor 0.50→0 NO-OP) |
| REQ-003 | TASK-007 (unique constraint), TASK-011 Case C (replay rolls back), Case G (distinct event ids → multiple movements) |
| REQ-004 | TASK-010 + TASK-010b (listeners), TASK-011 Cases D, F |
| REQ-005 / Q2 | TASK-011 Case E (debit-without-credit NO-OP + WARN) |
| REQ-006 | TASK-007 + TASK-011 (analogous Case C contract for cancels — covered at the dedup-table level) |
| REQ-007 | TASK-006 (`+100, -150, +50` → `-50` mid-sequence) at the repo layer |
| REQ-008 | TASK-006 + TASK-011 Case D (outbox payload `newBalance=0` after debit) |
| REQ-011/012 | TASK-011 (outbox payload shape — `cause`, `originatingEventType`, `originatingEventId`); end-to-end Kafka delivery ships in TASK-013 |
| REQ-013 | `MovementJpaRepository` extends bare `Repository<…>` only; new derived-query finder is SELECT-only |
| REQ-014 | TASK-011 outbox payload carries `originatingEventId` + `originatingEventType` |
| REQ-015 | TASK-009/010/010b listener swallow contract (RULE-010) |

---

## Risks / follow-ups carried into M3

1. **TASK-013 (outbox scheduler + producer)** — has not yet
   exercised end-to-end Kafka delivery of the
   `CustomerBalanceUpdatedAvroModel` events whose JSON payloads M2
   produces. Until then, the only assertion that the outbox payload
   shape is correct is JSON-shape inspection in
   `LoyaltyLedgerHandlerHappyPathIT`. TASK-012 (JSON → Avro mapper)
   gates TASK-013.
2. **`MovementJpaRepository.findByOriginatingOrderIdOrderByAppendedAtAsc`** —
   added for IT convenience; it is SELECT-only so REQ-013 surface is
   preserved, but the read API (TASK-015 / TASK-016) should re-use
   this finder rather than introducing a parallel one.
3. **No failure-injection IT for `LoyaltyLedgerHandler`** — e.g., a
   forced JSON serialisation failure in `toJson(...)` (which the
   handler maps to an `IllegalStateException` to roll back the txn).
   Low-priority; the failure path is structurally simple and the
   payload is a record of primitives + UUID + ZonedDateTime.
4. **PRD example arithmetic for TASK-011 Case H is misspecified.**
   Documented in `tasks.md` and resolved by dropping the case (D5).
   No further action required — REQ-007 coverage is intact.

---

## Sign-off

- All five M2 TASK rows are flipped to `done` in `tasks.md` with
  commit chain + final CI run id.
- Final CI run [`25620422467`](https://github.com/lg-labs/lg5-loyalty-ledger/actions/runs/25620422467)
  green on `feature/001-loyalty-ledger@a450f0e` — 34/34 ITs.
- Branch `feature/001-loyalty-ledger` is **ready to start M3** at
  `a450f0e`. M3 begins with TASK-012 (outbox payload JSON →
  outbound Avro `CustomerBalanceUpdatedAvroModel` mapper) which has
  no further dependencies inside this branch.
