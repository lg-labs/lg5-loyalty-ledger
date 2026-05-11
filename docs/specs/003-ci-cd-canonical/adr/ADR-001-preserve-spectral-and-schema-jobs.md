# ADR-001 — Preserve `schema-compat` and `api-specs-lint` jobs alongside the canonical topology

- **Status**: Proposed
- **Date**: 2026-05-10
- **Feature**: `003-ci-cd-canonical`
- **Decided by**: pending user approval

## Context

Features 001 and 002 each shipped a project-specific CI gate that
does not exist in the canonical lg5-spring template:

1. `schema-compat` (feat 001 / TASK-014) — spins up a real
   Confluent Kafka + Schema Registry stack via `services:`, runs
   `make publish-schemas` (idempotency check), `make check-schema-compat`
   (positive case), and a synthetic breaking-change negative case
   to assert the gate refuses incompatibilities **without** registering
   them. ~110 lines of YAML; no equivalent in the canonical template.
2. `api-specs-lint` (feat 002 / TASK-004) — installs Spectral CLI
   pinned to `6.15.1` and runs `spectral lint docs/api/openapi.yaml
   docs/api/asyncapi.yaml --fail-severity=warn`. Independent of Maven
   and JDK, so very fast (~17s on average).

Both are constitutional to our service contract:

- `schema-compat` is the only thing protecting downstream Kafka
  consumers from breaking changes pushed by us (RULE-007 about Avro).
- `api-specs-lint` is the only thing keeping `docs/api/*.yaml` honest
  vs. our REST controllers (paired with the swagger-request-validator
  contract tests at runtime).

## Decision

**Add both jobs into the new `c-integration.yml` as parallel jobs**
that run alongside the canonical 11. They share the same workflow
trigger (`push` to main, `pull_request` to main) but are listed below
the canonical jobs in the YAML for readability.

- `schema-compat`: `needs: setup` (was `needs: build` in the old
  workflow — relaxed to `setup` because it does not need Maven; it
  brings up its own Kafka stack via `services:` and only needs the
  schema files in the working tree).
- `api-specs-lint`: no `needs:` (Spectral is Node-based; doesn't
  even need the JDK).

Both are **left verbatim** in their current implementation — no
rewrites, no migration to Make targets. They are deliberately
out-of-scope for the bundle skill.

## Alternatives considered

1. **Keep two workflows side by side** (`c-integration.yml` for the
   canonical 11 + `ci.yml` for our 2 specific gates).
   Rejected: doubles the duplication of `actions/checkout` /
   `setup-java` / Maven cache steps, and a developer reading the
   PR checks list has to mentally reconcile two workflows. With
   everything in one file, a single "Required check on `main`"
   policy can list all 13 jobs.

2. **Migrate `schema-compat` to a Make target invoked from a single
   "schema" Cucumber-style ATDD scenario inside the canonical `test`
   job.**
   Rejected: forces ATDD (Spring Boot context, testcontainers, time
   budget) onto a check that should run in <1 min independent of
   anything else. Also breaks the negative-test pattern (mutate
   .avsc on disk and restore it) which is hard to express as a
   Cucumber scenario.

3. **Push `schema-compat` and `api-specs-lint` upstream into the
   bundle so future services inherit them.**
   Long-term: yes, but out of scope for this PR. Recorded as a
   follow-up: open issues on `lg5-spring-agent-os` for skills
   `lg5-schema-registry-gate` and `lg5-api-specs-lint` once we have
   a 2nd consumer service and can validate the pattern is general.

## Consequences

- **Positive**: zero loss of existing protection; one workflow file
  to reason about; required-checks policy stays simple.
- **Positive**: jobs run in parallel (Schema-Registry: ~45s,
  Spectral: ~17s) so total wall-clock CI time is unchanged vs.
  canonical-only.
- **Negative**: divergence from a literal copy of the bundle
  template. Mitigated by a milestone report (TASK-009) that
  enumerates every deviation, and by RULE-018 compliance (we cite
  the canonical template in the same workflow file via comments).
- **Negative**: when the bundle ships skills for these jobs, we'll
  need a follow-up PR to remove our local versions and inherit the
  upstream one. Cheap; both are self-contained.

## Compliance

- RULE-001 — Stack baseline (JDK 21, zulu): ✓ both jobs already comply.
- RULE-014 — Config prefixes: N/A (workflow-level).
- RULE-017 — Prefer Make targets: ✓ `schema-compat` already uses
  `make publish-schemas` and `make check-schema-compat`.
- RULE-018 — Cite canonical sources: ✓ this ADR + comments in the
  workflow point to the bundle skill version (`lg5-github-actions`
  v0.1.1, bundle v0.3.5) and the food-ordering-system reference.
