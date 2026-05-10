# Feature 003 — CI/CD canonical (lg5-spring 11-job topology)

> Mini-SDD feature. No PRD or data-model — pure infrastructure scaffold.
> Driver: `lg5-ci-cd-engineer` subagent + `/scaffold-ci-cd` command
> shipped in `lg5-spring-agent-os` v0.3.5.

## Goal

Replace our hand-rolled 4-job CI workflow (`.github/workflows/ci.yml`)
with the canonical 11-job lg5-spring topology shipped by the bundle,
**preserving** the two project-specific gates we built in features 001
and 002 (Schema-Registry compatibility gate, Spectral OpenAPI/AsyncAPI
lint).

## Scope (in)

- Composite action `.github/actions/setup-maven-credentials/action.yml`
  (DRY Maven 401 fix; consumes `PKG_GITHUB_TOKEN`).
- New workflow `.github/workflows/c-integration.yml` with the canonical
  11 jobs:
  `setup → {checkstyle, coverage, visualization} → {quality, build} → test → {openapi, asyncapi, allure, docs}`.
- Two **preserved** project-specific jobs added in parallel to the
  canonical topology (post-`setup`, no other dependencies):
  `schema-compat` (TASK-014 of feat 001) and `api-specs-lint`
  (TASK-004 of feat 002). See ADR-001.
- API doc templates under `lg5-loyalty-ledger-support/{openapi,asyncapi}-template/index.html`.
- Allure wiring in `lg5-loyalty-ledger-acceptance-test/`:
  - `src/test/resources/allure.properties`.
  - `pom.xml` adds `allure-cucumber7-jvm` + `allure-junit-platform`
    (both `2.29.1`, test scope).
  - `AcceptanceTestRunnerIT` plugin string appended with
    `, io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm`.
- Delete the old `.github/workflows/ci.yml` (replaced; no rollback path
  needed — both old and new are idempotent on `main`).

## Scope (out, deferred to feature 004)

- Firebase Hosting deployment of the OpenAPI / AsyncAPI / Allure /
  MkDocs sites. Documented in ADR-002.
- Container delivery (push image to GHCR / ECR). Bundle does not yet
  ship a `lg5-container-delivery` skill. Deferred sine die.
- Codacy / Sonar **actual** integration. The `quality` job is a
  placeholder that downloads the Codacy reporter binary but does
  not call its endpoint (matches blank-service exactly).

## Pre-conditions

| # | Item | Status |
|---|------|--------|
| 1 | Repo secret `PKG_GITHUB_TOKEN` (read:packages on `lg-labs-pentagon`) | DONE — verified with `gh secret list` (created 2026-05-10) |
| 2 | Bundle v0.3.5 installed (`.agent-os` submodule) | DONE — merged to `main` as `461f927` |
| 3 | `application.yaml` does not hard-code `log.path` | OK — uses Spring's default Logback config (no `log:` block); skill step 7 N/A |
| 4 | `Makefile` exposes `run-checkstyle`, `run-verify`, `install-skip-test`, `run-atdd-module`, `publish-schemas`, `check-schema-compat` | OK — all six present |

## Tasks (atomic; single milestone)

| ID | Description | Acceptance |
|----|-------------|------------|
| TASK-001 | Run `/scaffold-ci-cd loyalty-ledger` (steps 1–6, skip 7). | Files present in expected paths; placeholders rewritten from `blank-` to `lg5-loyalty-ledger-`. |
| TASK-002 | Append `schema-compat` job (verbatim from current `ci.yml`) into the new `c-integration.yml`. Re-target `needs:` from `build` → `setup` so it runs in parallel earlier. | Workflow valid; job runs after `setup`. |
| TASK-003 | Append `api-specs-lint` job (verbatim from current `ci.yml`) into `c-integration.yml`. No `needs:` (independent of Maven). | Workflow valid; job runs at the same level as `setup`. |
| TASK-004 | Delete `.github/workflows/ci.yml` (the legacy 4-job workflow). | Only `c-integration.yml` remains under `.github/workflows/`. |
| TASK-005 | Run `actionlint` on the new workflow locally. Fix any findings. | `actionlint` exit 0. |
| TASK-006 | Verify ATDD module still compiles with new Allure deps + `AcceptanceTestRunnerIT` plugin string. Run `JAVA_HOME=… mvn -pl lg5-loyalty-ledger-acceptance-test -am test-compile` locally. | Compile succeeds. |
| TASK-007 | Push branch, open PR, watch all 13 jobs (11 canonical + 2 preserved). Investigate any red. | All checks green. |
| TASK-008 | Squash-merge to `main` after approval. | `main` HEAD on the new commit; feature branch deleted. |
| TASK-009 | Author milestone report `reports/M1-completion.md` summarizing diffs vs. canonical, deviations, and follow-ups (mainly feature 004 — Firebase). | Report committed. |

## Verification gates

- **Pre-execution gate (this doc + 2 ADRs)**: user approval.
- **Mid-execution gate (after TASK-005)**: actionlint clean before push.
- **Post-execution gate (after TASK-007)**: 13/13 green.
- **Quality gate (TASK-009)**: report enumerates every deviation from
  the canonical template and links it to its ADR.

## Roll-forward / rollback

- Roll-forward only. The canonical workflow is a strict superset of
  capability vs. the old 4-job one. If a canonical job is broken
  post-merge, fix in a follow-up PR rather than reverting.
- `quality`, `visualization`, `docs` jobs that fail intermittently due
  to network (CDN, Codacy releases endpoint) should be made
  `continue-on-error: true` only after a real failure is observed —
  not pre-emptively.
