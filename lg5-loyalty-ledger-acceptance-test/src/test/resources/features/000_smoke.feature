Feature: ATDD infra smoke test (TASK-018)

  As an engineer wiring the Cucumber + Testcontainers + Spring Boot
  ATDD harness in lg5-loyalty-ledger-acceptance-test for the first
  time, I need a single trivial scenario so that:
    - the JUnit Platform Suite + Cucumber engine can discover at
      least one test (without it, the suite engine raises
      `NoTestsDiscoveredException` at runtime — see the JUnit
      Platform Suite engine `@Suite` default `failIfNoTests=true`),
    - Failsafe reports the runner as "ran 1 / failed 0",
    - the Spring application context, the Postgres container, and
      the Kafka + Schema-Registry containers all start cleanly
      under the {test, local} profile (RULE-012) BEFORE TASK-019
      adds the real REQ-coverage scenarios.
  This file MAY be deleted in TASK-019 once at least one real
  scenario lands; until then it pins the infra wire by itself.

  Scenario: harness boots and a trivial step passes
    Given the ATDD harness is wired
    Then the trivial step passes
