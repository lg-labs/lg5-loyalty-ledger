# M3 Milestone Report — Outbound (Outbox → Kafka)

**Branch:** `feature/001-loyalty-ledger`
**Closed at commit:** `6c50fe6` (TASK-014 CI fix-up)
**Final CI run:** [`25621646414`](https://github.com/lg-labs/lg5-loyalty-ledger/actions/runs/25621646414) — **35/35 ITs green** + schema-compat gate green; **0 failures**, **0 errors**, **0 skipped**.
**Reporting period:** end of M2 (`a450f0e`) → end of TASK-014.

---

## Scope (per `plan.md` §M3)

> "M3 Outbound — outbox payload JSON → Avro mapper; outbox scheduler +
> Kafka publisher (sync mapping + async send + status callback);
> schema-registry compatibility gate."

Three `tasks.md` rows:

| TASK | Title | Status |
|---|---|---|
| TASK-012 | Outbox payload JSON → `CustomerBalanceUpdatedAvroModel` mapper | done |
| TASK-013 | `OutboxScheduler` + `CustomerBalanceUpdatedKafkaPublisher` (publish path) | done |
| TASK-014 | Schema-registry compatibility check + registration script | done |

All M3 acceptance criteria from the PRD (REQ-011, REQ-012, the
outbound side of REQ-014) verified — TASK-012 at unit level, TASK-013
at IT level (end-to-end through a real `cp-schema-registry` + `cp-kafka`
testcontainer pair, published Avro consumed and decoded with field-by-field
assertions), TASK-014 at CI-gate level (running `cp-schema-registry`
service container in the workflow). No deviation from spec text apart
from the TASK-014 negative-test substitution noted in §Decisions D3.

---

## Commit chain (M3 only — 5 commits)

```
6c50fe6 fix(TASK-014): use add-required-field as the breaking-change probe
e514ecb feat(TASK-014): schema-registry compatibility check + register subjects
16910d0 fix(TASK-013): add no-op @MockitoBean to publisher IT to share TestContext
f635659 feat(TASK-013): outbox scheduler + Kafka publisher for CustomerBalanceUpdated
ad6507d feat(TASK-012): outbound Avro mapper (JSON → CustomerBalanceUpdatedAvroModel)
```

Five commits across 3 TASKs (one `feat` per TASK plus two targeted
`fix` follow-ups, one for each `feat` commit on TASK-013/014). No
`--no-verify`, no `--amend` of pushed commits, no force pushes. Every
commit committed as `lglabs <105936384+lglabs@users.noreply.github.com>`.

---

## CI runs (M3)

| Run | Head | Trigger | Result |
|---|---|---|---|
| [`25621098191`](https://github.com/lg-labs/lg5-loyalty-ledger/actions/runs/25621098191) | `ad6507d` | TASK-012 push | ✅ build + 34 ITs |
| [`25621293415`](https://github.com/lg-labs/lg5-loyalty-ledger/actions/runs/25621293415) | `f635659` | TASK-013 push | ❌ ITs (SR group-coordinator collision; see D2) |
| [`25621406319`](https://github.com/lg-labs/lg5-loyalty-ledger/actions/runs/25621406319) | `16910d0` | TASK-013 fix-up | ✅ build + 35 ITs |
| [`25621566921`](https://github.com/lg-labs/lg5-loyalty-ledger/actions/runs/25621566921) | `e514ecb` | TASK-014 push | ❌ schema-compat gate (negative test wrong; see D3) |
| [`25621646414`](https://github.com/lg-labs/lg5-loyalty-ledger/actions/runs/25621646414) | `6c50fe6` | TASK-014 fix-up | ✅ all 3 jobs green (final M3 run) |

All fix-up commits are NEW commits (per the milestone-mode rules:
amend is not allowed once a commit has been pushed).

---

## IT delta vs. M2

M2 closed at 34 container ITs. M3 added one Kafka publisher IT
(`CustomerBalanceUpdatedKafkaPublisherIT`, 1 test) and modified three
existing listener ITs (no new tests, only `@TestPropertySource`
additions to share the Spring TestContext cache). End of M3:

```
Tests run: 35, Failures: 0, Errors: 0, Skipped: 0
  -- LoyaltyLedgerHandlerEdgeCasesIT             3
  -- LoyaltyLedgerHandlerHappyPathIT             4
  -- OrderPaidKafkaListenerIT                    1
  -- OrderCancelledKafkaListenerIT               1
  -- OrderRefundedKafkaListenerIT                1
  -- CustomerBalanceUpdatedKafkaPublisherIT      1   ← new in M3
  -- CustomerBalanceRepositoryIT                 3
  -- MovementLedgerRepositoryIT                  5
  -- LiquibaseMigrationIT                       11
  -- ProcessedInputEventAndOutboxRepositoryIT    5
```

(Plus the new `OutboundCustomerBalanceUpdatedAvroMapperTest` — 6
unit tests — running in the surefire phase.)

A new CI job — `Schema-Registry compatibility gate (TASK-014)` —
runs in parallel with the IT job. It spins up a Confluent SR
service container and exercises four assertions:

1. `make publish-schemas` registers both subjects
   (`customer-balance-updated-value`, `BalanceUpdateCause`) and sets
   compatibility mode `BACKWARD`.
2. Re-running `make publish-schemas` is a no-op — the version count
   on `customer-balance-updated-value` is unchanged.
3. `make check-schema-compat` passes on the current sources.
4. After mutating a copy of `customer_balance_updated.avsc` to add
   a required (non-defaulted) field, `make check-schema-compat`
   exits non-zero AND the registered version count is unchanged
   (the gate is read-only, no side effect).

All four were green in run `25621646414`.

---

## Cross-repo dependency

None. M3 consumed the `lg5-spring` framework at the same SHA as M2
(`d0d754a`, parent version `1.0.0-alpha.d0d754a`). The framework
classes touched by M3 — `com.lg5.spring.outbox.{OutboxScheduler,
OutboxStatus}`, `com.lg5.spring.kafka.producer.{KafkaProducer,
KafkaMessageHelper}` — are all present and stable in that SHA; no
upstream change required.

---

## Decisions (M3-specific, ordered by introduction)

### D1 — Mapper signature is `toAvro(UUID messageId, payload)`, called with `OutboxMessage.id()`

The Avro record carries a `messageId` field (`fixed[16]` UUID) that
acts as a producer-side dedup key for downstream consumers. Two
options were considered:

(a) Generate a fresh `UUID.randomUUID()` inside the mapper.
(b) Take it as an argument and have the scheduler pass
    `OutboxMessage.id()`.

(b) was selected. Rationale: the outbox row's `id` is stable across
retries — if a scheduler iteration crashes between
`KafkaTemplate.send` and the `STARTED → COMPLETED` status update,
the next iteration will re-publish the same outbox row with the
**same** `messageId`, giving downstream consumers a clean dedup
key. Generating a fresh UUID per `toAvro` call would defeat that.
This is the same shape food-ordering-system uses for its
`PaymentResponseAvroModel.id`.

### D2 — Per-IT `@MockitoBean` set is part of the Spring TestContext cache key — must align across kafka ITs

Run `25621293415` (TASK-013) failed with the kafka container reporting
`schema registry group contained multiple members advertising the
same URL`. Root cause: Spring TestContext caches contexts by a key
that includes both `@TestPropertySource` content **and** the set of
`@MockitoBean`-declared types. The three pre-existing listener ITs
all declared `@MockitoBean LoyaltyLedgerInputPort`; the new
`CustomerBalanceUpdatedKafkaPublisherIT` declared none, producing
a different cache key, which spawned a second Spring context — and
therefore a second Confluent SR testcontainer — while the first was
still alive, racing for the SR group leadership.

**Resolution (commit `16910d0`):** add a no-op
`@MockitoBean LoyaltyLedgerInputPort` to the publisher IT (the
publisher path never calls the input port, so the mock is genuinely
unused at runtime; it exists purely to align the cache key). Result:
all four kafka ITs now share a single context and a single SR/Kafka
testcontainer pair.

A more invasive alternative would have been to factor out a shared
abstract base class with the mock declaration. Rejected as
over-engineering for four files; the unused mock is locally
documented with a comment.

### D3 — `make check-schema-compat` negative test is "add required field", not "remove field"

The TASK-014 spec example for the negative test was *"removing the
`delta` field."* That is **BACKWARD-compatible** under Avro
semantics: a new reader of old data can simply ignore the missing
field, so `is_compatible=true` is the correct registry answer.
Run `25621566921` exposed the spec text as technically wrong —
the gate itself was correct.

**Resolution (commit `6c50fe6`):** change the negative-test
mutation to **add a required (non-defaulted) field**, which is
unambiguously BACKWARD-incompatible (a new reader cannot supply a
value for a field that was absent in old data). The corrected
negative test passes in run `25621646414`. The PRD/spec example was
not retro-edited; this report is the canonical record.

### D4 — `OutboxSchedulerHelper` lives in `application-service`, not in `message-core`

`OutboxSchedulerHelper.findStarted()` and
`OutboxSchedulerHelper.updateOutboxMessage(...)` both touch the
`outbox.customer_balance_updated_outbox` JPA repository. Two
candidate placements:

(a) `message-core` (next to the Kafka publisher).
(b) `application-service` (the only Spring module that already
    imports JPA).

(b) was selected. Rationale: the Kafka adapter must remain a pure
adapter — port in (the `MessagePublisher` interface), framework
producer + helper out, **no DB access**. Touching the outbox
table from `message-core` would invert the dependency direction
(adapter→infra DB) and contaminate the message ring. The same
shape lives in `food-ordering-system/payment-service` —
`PaymentOutboxHelper` is in `payment-service-domain/payment-service-application`,
not in the message ring. RULE-010 alignment.

### D5 — Standalone `BalanceUpdateCause` subject registered in addition to the auto-registered inline form

The runtime `KafkaAvroSerializer` registers the **whole record**
schema under one subject (`customer-balance-updated-value`) and the
inline enum is part of that record schema — there is no separate
runtime subject for `BalanceUpdateCause`. The TASK-014 acceptance
text nonetheless calls for `BalanceUpdateCause` to exist as its own
subject. Two interpretations:

(a) Use Avro **schema references**: register the enum first, then
    register the record using `references` to point at the enum
    subject. Runtime serializer would have to be configured for
    references.
(b) Register the standalone enum schema under its own subject as a
    governance / discovery entry; runtime serializer is unaffected
    and continues to register the inline form on first send.

(b) was selected — simpler, keeps the runtime path identical to
food-ordering-system's, and the standalone subject still serves the
spec's intent (downstream tooling and ADR-005 documentation can
discover the enum's evolution under a stable subject name).

### D6 — All four kafka ITs run with `scheduling.enabled=true` even though only one needs the scheduler

`CustomerBalanceUpdatedOutboxScheduler` is gated by
`@ConditionalOnProperty("scheduling.enabled", matchIfMissing=true)`,
and `application-test.yaml` sets `scheduling.enabled: false` to
keep the rest of the suite quiet. The publisher IT must override
back to `true` (otherwise the scheduler bean is never instantiated
and nothing publishes). Per D2, the listener ITs must use the
**same** `@TestPropertySource` block to share the cache key — so
they too end up running with `scheduling.enabled=true`.

This is harmless: the listener ITs `@MockitoBean` the input port,
no outbox rows ever get written, and the scheduler simply iterates
an empty `findStarted()` result every 200 ms. No measurable test
slowdown.

### D7 — `OutboxSchedulerHelper.updateOutboxMessage` swallows `OptimisticLockingFailureException`

The scheduler runs every 200 ms in tests (10 s in prod). Two
schedulers can in principle pick up the same `STARTED` row and race
on the version-bump update. Catching the optimistic-lock failure
and swallowing it (logging a single WARN line) is the established
pattern from food-ordering-system; it is correct because the
**winner** has already published the Avro message and bumped the
status to `COMPLETED`, so the loser doing nothing is exactly right.
This mirrors RULE-010's listener swallow contract for the same
exception class on the inbound side.

---

## REQ → coverage matrix at end of M3

(Subset relevant to M3; full matrix in `tasks.md` §Coverage matrix.)

| REQ | Where covered (M3) |
|---|---|
| REQ-011 | TASK-012 (mapper preserves `cause`, `originatingEventType`, `originatingEventId`); TASK-013 (`CustomerBalanceUpdatedKafkaPublisherIT` decodes the published Avro and asserts these fields field-by-field); TASK-014 (subject `customer-balance-updated-value` registered with BACKWARD compatibility). |
| REQ-012 | TASK-012 (`messageId` field carried end-to-end as Avro `fixed[16]`); TASK-013 (publisher uses `OutboxMessage.id()` as the `messageId`, stable across retries — D1); TASK-014 (subject registered, future schema evolution gated by `make check-schema-compat`). |
| REQ-014 | TASK-013 (per-customer ordering preserved: Kafka producer key = `payload.customerId().toString()` — same partition, same FIFO order). |
| RULE-007 | TASK-014 (BACKWARD compatibility mode set on both subjects; CI gate enforces it for any future schema edit). |
| RULE-008 | TASK-013 (`OutboxScheduler.updateOutboxMessage` swallows `OptimisticLockingFailureException` on the `@Version`-stamped outbox row — D7). |
| RULE-010 | TASK-013 (publisher `outboxCallback.accept(msg, FAILED)` on send failure does not rethrow into the scheduler thread; the next scheduler tick will retry the row). |
| RULE-011 | TASK-013 (`CustomerBalanceUpdatedOutboxScheduler implements OutboxScheduler`, `@Component`, `@ConditionalOnProperty(scheduling.enabled, matchIfMissing=true)`, `@Scheduled(fixedDelayString=…)`). |
| ADR-005 | TASK-014 (standalone `BalanceUpdateCause` subject registered as governance entry — D5). |

---

## Risks / follow-ups carried into M4

1. **Outbox watermark / poison-message handling.** The scheduler
   currently retries any `STARTED` row indefinitely; a permanently
   un-mappable payload (e.g. a future cause string the mapper can no
   longer accommodate) would loop forever logging at WARN. V1 PRD is
   silent on this, and the mapper's `UNKNOWN`-default behaviour
   makes it unreachable today, but a `failure_count` column +
   `FAILED` terminal state would harden the path. Out of scope for
   v1.
2. **No producer-side schema-validation IT.** The publisher IT
   asserts the message round-trips through a real SR; it does not
   prove that an attempted publish of a registry-incompatible
   payload would be rejected at send time. The TASK-014 CI gate
   covers the equivalent check at build time, which is the correct
   place. Documented for future explicit producer-validation tests
   if/when ADR-005 evolves.
3. **TASK-014 negative-test mutation is per-CI-job, not in the
   gate's own self-test.** The gate script (`check-schema-compat.sh`)
   does not have a `--self-test` mode; the proof that it actually
   rejects an incompatible change lives only in the CI workflow. If
   the workflow is ever simplified, this guarantee must move into
   the script (or into a dedicated bats/shellspec test). Low
   priority while the workflow is the only consumer.
4. **The standalone `BalanceUpdateCause` subject is not actually
   referenced by anything yet** (D5). It is registered for future
   tooling. If no downstream tool consumes it within the next two
   milestones, consider removing the registration to keep the
   subject list tight.

---

## Sign-off

- All three M3 TASK rows are flipped to `done` in `tasks.md` with
  commit chain + final CI run id.
- Final CI run [`25621646414`](https://github.com/lg-labs/lg5-loyalty-ledger/actions/runs/25621646414)
  green on `feature/001-loyalty-ledger@6c50fe6` — 35/35 ITs +
  schema-compat gate (4/4 assertions).
- Branch `feature/001-loyalty-ledger` is **ready to start M4** at
  `6c50fe6`. M4 begins with TASK-015 (REST controller
  `GET /loyalty/customers/{id}/balance`) which has no further
  dependencies inside this branch (TASK-006 from M1 satisfies it).
