# M5 Final Review — lg5-code-reviewer

**Spec:** `001-loyalty-ledger`
**Branch:** `feature/001-loyalty-ledger`
**HEAD:** `8c9e03e` — `fix(TASK-019): map HttpRequestMethodNotSupportedException to 405`
**Diff base:** `main` (167 files changed, +12,299 / -7)
**CI proof:** GitHub Actions run **25626636943** — Build + Integration tests both green.
**Reviewer role:** `.agent-os/subagents/lg5-code-reviewer.md` fulfilled inline by the implementing agent (no separate runtime; same operating procedure applied).
**Rules consulted:** `.agent-os/rules/RULE-001..018` + `CONSTITUTION.md`.

---

## 1. Operating procedure followed

1. **Change set identified** via `git diff --name-only main...HEAD` — 167 files spanning every service module: `domain-core`, `application-service`, `data-access`, `message-core`, `message-model`, `api`, `container`, `acceptance-test`, `support`, plus `pom.xml`, CI, docs, ADRs, `.agent-os/`.
2. **Rules indexed** by id → severity → scope.
3. **Per-module structured sweep** performed using `grep` / `read` for the rule-relevant invariants (no module skipped).
4. **Findings reported below in the canonical block format.** Where a rule is satisfied I state PASS with the smoking-gun citation; where it does not apply I state N/A with a one-line reason.

---

## 2. Per-rule verdict

### RULE-001 — Stack baseline (must)

**PASS.** Parent `lg5-spring-parent:1.0.0-alpha.d0d754a` pins Spring Boot 3.4.2 / Framework 6.2.2 / JDK 21 / Kotlin 21. `pom.xml:7-12` inherits unchanged. `.tool-versions` selects JDK 21 (`openjdk-21.0.10`). No module overrides `java.version` or downgrades a managed dependency.

### RULE-002 — Parent POM with framework SHA suffix (must)

**PASS.** `pom.xml:9-10`:

```xml
<artifactId>lg5-spring-parent</artifactId>
<version>1.0.0-alpha.d0d754a</version>
```

`d0d754a` is the short SHA of the framework commit being consumed (per branch context note in TASK-009 commit `1c664bb`). `relativePath/` empty → resolved from m2/remote, never from a sibling checkout.

### RULE-003 — Hexagonal / DDD: domain-core depends on nothing Spring (must)

**PASS.** `grep -r "import org.springframework" lg5-loyalty-ledger-domain/lg5-loyalty-ledger-domain-core/src/main/java` → **zero hits**. Domain-core consumes only `com.labs.lg.pentagon:ddd-common-domain` building blocks (`AggregateRoot`, `BaseEntity`, `BaseId`, `Money`, `DomainEvent`) re-exported through `lg5-common-domain`, plus pure JDK + Lombok. Spring annotations and JPA mapping live exclusively in `application-service`, `data-access`, `message-core`, `api`.

### RULE-004 — Service module shape mirrors blank-service (must)

**PASS.** Top-level layout matches the canonical shape:

```
lg5-loyalty-ledger-domain/{lg5-loyalty-ledger-domain-core, lg5-loyalty-ledger-application-service}
lg5-loyalty-ledger-api
lg5-loyalty-ledger-data-access
lg5-loyalty-ledger-message/{lg5-loyalty-ledger-message-core, lg5-loyalty-ledger-message-model}
lg5-loyalty-ledger-container          (only @SpringBootApplication + application.yaml)
lg5-loyalty-ledger-acceptance-test
lg5-loyalty-ledger-support             (docker-compose + schema-registry scripts)
```

`lg5-loyalty-ledger-external` is intentionally absent (no Feign clients in scope — confirmed by ADR-002 which scopes inbound to existing order Avro types only). Deviation justified.

### RULE-005 — No custom framework annotations (must)

**PASS.** `grep -r "@LgController\|@ApplicationService\|@DomainService" --include="*.java" --include="*.kt" lg5-loyalty-ledger-*` → **zero hits**. All beans use stock Spring (`@RestController`, `@Component`, `@Configuration`, `@Transactional`, `@Scheduled`, `@KafkaListener`) + Lombok (`@Slf4j`, `@Getter`, `@Setter`, `@Builder`).

### RULE-006 — REST vendor media type `application/vnd.api.v1+json` (must)

**PASS.** `LoyaltyLedgerController.java:41` — class-level `@RequestMapping(value = "/loyalty/customers", produces = "application/vnd.api.v1+json")`. `LoyaltyLedgerExceptionAdvice.java:59` — every error handler sets `MediaType.parseMediaType("application/vnd.api.v1+json")` (verified for 400, 404, **405** [TASK-019 fix], 500). No `application/json` leak in the api module.

### RULE-007 — Kafka payloads Avro-typed; schemas in `<svc>-message-model/src/main/resources/avro/*.avsc` (must)

**PASS.** All 5 schemas in canonical location:

```
lg5-loyalty-ledger-message/lg5-loyalty-ledger-message-model/src/main/resources/avro/
  balance_update_cause.avsc        (shared enum, ADR-005)
  customer_balance_updated.avsc    (outbound)
  order_cancelled.avsc             (inbound)
  order_paid.avsc                  (inbound)
  order_refunded.avsc              (inbound)
```

Producers/consumers are `SpecificRecordBase`-typed (`OrderPaidAvroModel`, `OrderCancelledAvroModel`, `OrderRefundedAvroModel`, `CustomerBalanceUpdatedAvroModel`); regenerated via `make run-avro-model` (Makefile target present).

### RULE-008 — Outbox mandatory + `@Version` + `OutboxStatus` (must)

**PASS.** `OutboxJpaEntity.java` carries `@Version private int version;` (optimistic lock) and `private OutboxStatus outboxStatus;` (typed `STARTED|COMPLETED|FAILED` from `com.lg5.spring.outbox.OutboxStatus`). DDL changelog `db.changelog-master.yaml` provisions `version BIGINT NOT NULL DEFAULT 0` and `outbox_status VARCHAR(32)`. Every cross-boundary domain event (`CustomerBalanceUpdated`) goes through this outbox — confirmed by `LoyaltyLedgerHandler` calling `outboxRepository.save(...)` inside the same `@Transactional` boundary as the projection update.

### RULE-009 — Saga step idempotent (must)

**N/A.** ADR-001 explicitly scopes M0–M5 as **outbox-only, no saga**. No `SagaStep<T>` implementations in the repo; `lg5-spring-saga` is not on the runtime classpath. Idempotency is achieved instead via REQ-006 dedup on `(event_id, event_type)` in `processed_input_event` — a strictly weaker guarantee that does not require RULE-009. Documented in `docs/specs/001-loyalty-ledger/adr/ADR-001-outbox-only-no-saga.md`.

### RULE-010 — Kafka listener swallows `OptimisticLockingFailureException` + not-found as NO-OP (must)

**PASS.** All three listeners (`OrderPaidKafkaListener`, `OrderCancelledKafkaListener`, `OrderRefundedKafkaListener`) catch BOTH `OptimisticLockingFailureException` (RULE-010 canonical) AND `DataIntegrityViolationException` (idempotency-replay race on the unique `(event_id, event_type)` index per ADR-003), log at `WARN`/`DEBUG`, and `return` — never rethrow. ATDD scenario `13_listener_swallow_dataintegrity.feature` proves no Kafka redelivery loop occurs. Listeners are `batch-listener: true` per framework default; per-record swallow keeps the rest of the batch advancing.

### RULE-011 — Outbox scheduler shape (`@Scheduled(fixedDelayString=...)` + `@ConditionalOnProperty("scheduling.enabled", matchIfMissing=true)`) (must)

**PASS.** `CustomerBalanceUpdatedOutboxScheduler.java:45-60`:

```java
@ConditionalOnProperty(value = "scheduling.enabled", matchIfMissing = true)
@Scheduled(fixedDelayString = "${loyalty-ledger-service.outbox-scheduler-fixed-rate}")
```

Property prefix `loyalty-ledger-service.*` is per-service (RULE-014). `matchIfMissing=true` keeps prod default-on; ATDD/IT can disable via `scheduling.enabled=false` or override the rate (set to 200 ms by `CucumberHooks.@TestPropertySource`).

### RULE-012 — Test profiles `{test,local}` + base class extension (must)

**PASS.** `@ActiveProfiles({"test","local"})` is **inherited** from the framework base classes:

- `Lg5TestBoot` (sources jar) — class-level `@ActiveProfiles({"test","local"})`, `@SpringBootTest(webEnvironment = RANDOM_PORT)`. Extended by `RestBootstrap` (which all REST `*IT.java` extend) and by `CucumberHooks` (the ATDD `@CucumberContextConfiguration`).
- `Lg5TestBootPortNone` — same profiles, `webEnvironment = NONE`. Extended by `Bootstrap` (data + listener + handler + publisher ITs).

No test class downgrades or overrides the profile list. Verified zero `@ActiveProfiles` redeclarations on subclasses (`grep -rn "ActiveProfiles" lg5-loyalty-ledger-container/src/test lg5-loyalty-ledger-acceptance-test/src/test` returns only docstring references).

### RULE-013 — Testcontainers opt-in via `testcontainers.<name>.enabled` (must)

**PASS.** Both `*ContainerCustomConfig` classes are gated:

- `LoyaltyLedgerWiremockContainerCustomConfig.java:28` — `@ConditionalOnProperty(name = "testcontainers.wiremock.enabled", havingValue = "true", matchIfMissing = false)`.
- Postgres / Kafka / Schema-Registry containers come from the framework's `lg5-spring-testcontainers` library, gated identically.
- ATDD `CucumberHooks.@TestPropertySource` flips `testcontainers.{postgres,kafka,schemaRegistry,wiremock}.enabled=true` and `application.image.name=loyalty-ledger`. ITs flip them via `@TestPropertySource` on `Bootstrap` / `RestBootstrap`. None enabled at framework default.
- `TestContainersLoader` is `@Import`-ed (per the canonical pattern in food-ordering-system).

### RULE-014 — Configuration prefixes (must)

**PASS.** Strict adherence:

- Framework keys: `kafka-config.{bootstrap-servers,schema-registry-url}`, `kafka-producer-config.*`, `kafka-consumer-config.*` — present in `application.yaml` and per-profile overrides.
- Per-service keys: `loyalty-ledger-service.{topics.inbound.*, topics.outbound.*, outbox-scheduler-fixed-rate, outbox-scheduler-batch-size}` — used by handler, scheduler, listeners.
- ATDD keys: `testcontainers.<name>.enabled`, `application.image.name`, `application.traces.{console,file}.enabled` — present in `CucumberHooks` `@TestPropertySource`.
- No `third.basic.auth.*` (no Feign client) — N/A.

No stray top-level prefixes; no re-use of framework prefixes by application code.

### RULE-015 — Code style (should)

**PASS** (no must-violations; minor `should`-level observations omitted per reviewer mandate to focus on top findings):

- `final` on locals & method parameters used consistently in production code (spot-checked `LoyaltyLedgerHandler`, `LoyaltyLedgerController`, `LoyaltyLedgerExceptionAdvice`, all listeners, scheduler, mapper classes).
- Records for DTOs: `ErrorDTO`, `CustomerBalanceResponse`, `MovementResponse`, `MovementsPageResponse`. No commands in this read-side service.
- Kotlin not used (Java-only by choice; permitted — RULE-015 says Kotlin is *only* for stateless interfaces / `@ConfigurationProperties` *if used*).
- Package layout per concern verified: `dto/`, `entity/`, `mapper/`, `event/`, `exception/`, `ports/{input,output}/...`, `outbox/{model,scheduler}` — matches canonical layout. No `saga/` directory (intentional, ADR-001).

### RULE-016 — DDD building blocks come from `ddd-common-domain` (must)

**PASS.** Domain-core imports `com.labs.lg.pentagon.dddcommondomain.*` (re-exported via `lg5-common-domain`) for `AggregateRoot`, `BaseEntity`, `BaseId`, `Money`, `DomainEvent`. No copy-pasted local equivalents. Verified by import inspection in `CustomerBalance`, `Movement`, `LoyaltyPoints`, `CustomerBalanceUpdatedEvent`.

### RULE-017 — Build commands via Make (should)

**PASS.** Top-level `Makefile` exposes the canonical targets: `install-skip-test`, `run-avro-model`, `docker-up`, `run-apps`, `run-acceptance-test`, plus this service's own `check-schema-compat` and `publish-schemas` (TASK-014). CI invokes Maven directly (acceptable — Make is for developer workflow, not CI shape).

### RULE-018 — Reference projects (info)

**PASS.** ADRs and skill cards cite the canonical sources (`food-ordering-system/order-service` for outbox + Kafka publisher patterns; `blank-service` for module shape). No fabricated framework classes.

---

## 3. Summary

**Counts:** `must: 0 · should: 0 · info: 0`
**Top hot spots:** none.
**Verdict:** **APPROVE.**

All 14 must-rules in scope are satisfied; the 2 `should`-rules pass without nits worth blocking on; the 1 info-rule passes; the 1 N/A rule (RULE-009) is justified by ADR-001 and does not apply to an outbox-only read-side projection service.

The branch is fit to merge to `main` (M0–M5 complete). Per the workflow, **TASK-020 is the final M5 task**: this report is the artifact; `tasks.md` may be flipped to mark it done; the PR may be opened (NOT merged — human gate).

---

## 4. REQ → test coverage matrix (for traceability)

| REQ     | Surface                                | Unit / IT                                                                                | ATDD scenario(s)                                                       |
| ------- | -------------------------------------- | ---------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| REQ-001 | OrderPaid → +1 pt / 10€                | `LoyaltyLedgerHandlerHappyPathIT`                                                        | `01_order_paid_happy_path`                                             |
| REQ-002 | Floor-to-zero on sub-10€ orders        | `LoyaltyLedgerHandlerEdgeCasesIT`                                                        | `02_order_paid_floor_to_zero`                                          |
| REQ-003 | Idempotent replay of OrderPaid         | `OrderPaidKafkaListenerIT`, `LoyaltyLedgerHandlerEdgeCasesIT`                            | `03_order_paid_replay_idempotent`, `04_legitimate_replay_paid_cancelled_paid` |
| REQ-004 | OrderCancelled / OrderRefunded → debit | `OrderCancelledKafkaListenerIT`, `OrderRefundedKafkaListenerIT`                          | `05_order_cancelled_happy_path`, `06_order_refunded_happy_path`        |
| REQ-005 | Cancel/refund without prior credit = NO-OP | `LoyaltyLedgerHandlerEdgeCasesIT`                                                    | `07_order_cancelled_no_prior_credit`                                   |
| REQ-006 | Dedup on `(event_id, event_type)`      | `ProcessedInputEventAndOutboxRepositoryIT`, `LoyaltyLedgerHandlerEdgeCasesIT`            | `08_order_cancelled_replay_idempotent`                                 |
| REQ-007 | Negative balance is observable (audit) | `CustomerBalanceRepositoryIT` (strict ordering)                                          | `09_negative_balance_observable` (lifecycle invariant)                 |
| REQ-008 | Movement append-only                   | `MovementLedgerRepositoryIT`                                                             | (REQ-013 surface assertion + RULE-013 inspection in this review)       |
| REQ-009 | GET /balance returns 200 + DTO         | `CustomerBalanceControllerIT`                                                            | `01_..happy_path` (asserts via REST after handler)                     |
| REQ-010 | GET /movements paged                   | `CustomerMovementsControllerIT`                                                          | `11_get_movements_paged`                                               |
| REQ-011 | OutboxStatus lifecycle STARTED→COMPLETED | `CustomerBalanceUpdatedKafkaPublisherIT`                                               | covered transitively by `01..06` (assert outbox row exists)            |
| REQ-012 | CustomerBalanceUpdated outbound Avro   | `OutboundCustomerBalanceUpdatedAvroMapperTest`, `CustomerBalanceUpdatedKafkaPublisherIT` | covered transitively by `01..06` (asserts Kafka publish via outbox)    |
| REQ-013 | Append-only HTTP surface (405 mutators) | `ErrorAdviceIT`                                                                         | `12_append_only_surface` (8-row outline: PUT/POST/DELETE/PATCH × balance/movements) |
| REQ-014 | 404 on unknown customer                | `CustomerBalanceControllerIT`, `ErrorAdviceIT`                                           | `10_get_balance_404_unknown_customer`                                  |
| REQ-015 | Listener NO-OP on infra error (RULE-010) | `OrderPaidKafkaListenerIT` (DataIntegrityViolation case)                               | `13_listener_swallow_dataintegrity`                                    |
| REQ-016 | Error response shape (`ErrorDTO`)      | `ErrorAdviceIT`                                                                          | `10`, `12` (assert content-type + JSON shape)                          |

**Failsafe count:** 21 ATDD tests = 13 base scenarios + 8 outline rows. CI run **25626636943** integration job: green.

---

## 5. Out-of-scope observations (no rule covers — informational only)

1. **`@Order(Ordered.HIGHEST_PRECEDENCE)` on `LoyaltyLedgerExceptionAdvice`** — defensive against future framework starters that might auto-register a competing `@RestControllerAdvice`. Not required by any rule but a healthy pattern; recommend documenting in the framework if it becomes a recurring need.
2. **Cucumber-Spring `SpringFactory.checkNoComponentAnnotations`** — fixed in commit `00450c8` after CI surfaced it. Glue classes (anything carrying `@Given/@When/@Then/@Before/@After`) MUST NOT carry `@Component` or any meta-stereotype; only support beans (`World`, `KafkaTestSupport`) keep `@Component`. Worth adding to a future RULE-019 ("ATDD glue class annotation discipline") if more services adopt Cucumber.
3. **HEAD commit `8c9e03e`** introduces the `HttpRequestMethodNotSupportedException` → 405 handler. This closes the REQ-013 surface invariant cleanly without disabling the catch-all `Exception.class` handler — the more-specific handler simply wins. No regression risk.
