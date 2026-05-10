# M5 — ATDD + Final Review

**Spec:** `001-loyalty-ledger`
**Branch:** `feature/001-loyalty-ledger`
**Closing HEAD:** `8c9e03e`
**CI proof:** GitHub Actions run **25626636943** — Build (compile + unit) and Integration tests (Testcontainers) both green.

## Tasks closed in this milestone

| TASK    | Status | Commits                                                                                                                                                                                                          |
| ------- | ------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| TASK-018 | done   | `0bf9b88` (ATDD infra), `01060b5` (smoke), `bb692f5` / `90060ba` (datasource blanks), `27dec1d` (`@TestPropertySource` hoist)                                                                                    |
| TASK-019 | done   | `a099195` (13 features / 21 scenarios — `lg5-test-generator` role inline), `00450c8` (drop `@Component` from glue per Cucumber-Spring `SpringFactory.checkNoComponentAnnotations`), `8c9e03e` (405 advice fix)   |
| TASK-020 | done   | this report + `docs/specs/001-loyalty-ledger/reports/m5-final-review.md` (`lg5-code-reviewer` role inline) — verdict **APPROVE**, 0 `must`-violations                                                             |

## ATDD count

- **13 feature files** in `lg5-loyalty-ledger-acceptance-test/src/test/resources/features/`.
- **16 scenarios** authored (12 base + 1 outline).
- **21 Failsafe tests** = 13 base scenarios + 8 outline rows of `12_append_only_surface`.
- All 21 green in CI run **25626636943**.

## Architecture confirmation (`lg5-code-reviewer` highlights)

- **One Spring context** for the whole Cucumber JVM (`@CucumberContextConfiguration` on `CucumberHooks` extending `Lg5TestBoot` — RANDOM_PORT + RestAssured spec).
- **Glue classes** (`LedgerSteps`, `RestSteps`, `KafkaSteps`, `RestSetupHooks`) carry **no** `@Component` (Cucumber-Spring 7.x rejects any meta-stereotype on glue). Only support beans (`World`, `KafkaTestSupport`) keep `@Component`.
- **Scenario isolation** by random UUID per scenario (RULE-013 append-only repos cannot truncate).
- **Outbox-payload assertions** look across `STARTED + COMPLETED + FAILED` to race-proof against the 200 ms scheduler tick.
- **Most scenarios** drive `LoyaltyLedgerInputPort.process(...)` directly (deterministic, no Kafka roundtrip). Only `13_listener_swallow_dataintegrity` uses the real Avro Kafka producer (`KafkaTestSupport`) to prove RULE-010 swallow.
- **REQ-013** (append-only HTTP surface) is now machine-checkable: `LoyaltyLedgerExceptionAdvice.handleMethodNotAllowed` maps `HttpRequestMethodNotSupportedException` → `405 METHOD_NOT_ALLOWED` with `application/vnd.api.v1+json`.

## REQ → ATDD scenario matrix

See **§4** of `docs/specs/001-loyalty-ledger/reports/m5-final-review.md` for the full REQ → unit/IT/ATDD coverage matrix. Every REQ-NNN (001..016) is covered by ≥1 passing ATDD scenario plus its IT counterpart.

## Reviewer verdict

`docs/specs/001-loyalty-ledger/reports/m5-final-review.md`:

> **APPROVE.** All 14 must-rules in scope are satisfied; 2 should-rules pass without nits worth blocking on; the 1 info-rule passes; the 1 N/A rule (RULE-009) is justified by ADR-001 and does not apply to an outbox-only read-side projection service.

## Next gate

Open PR `feature/001-loyalty-ledger` → `main` (DO NOT merge — human gate per `tasks.md` TASK-020 acceptance: "marked done only after the human reviewer also signs off on the PR").
