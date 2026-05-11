# M1 Completion Report — Feature 003 (CI/CD canonical)

- **Feature**: `003-ci-cd-canonical`
- **Milestone**: M1 (single-milestone feature)
- **Status**: COMPLETED
- **Merged to `main`**: `5a84182` on 2026-05-10 via PR
  [#4](https://github.com/lg-labs/lg5-loyalty-ledger/pull/4)
- **Bundle version consumed**: `lg5-spring-agent-os` v0.3.5
  (`.agent-os` submodule pinned to `368e55c`)
- **Framework SHA pin (transitive)**: `lg5-spring` `d0d754a`

## Goal recap

Replace the hand-rolled 4-job `.github/workflows/ci.yml` with the
canonical 11-job `lg5-spring` topology shipped by the bundle, while
preserving the two project-specific gates added in features 001
(Schema-Registry compatibility) and 002 (Spectral OpenAPI/AsyncAPI
lint).

## Outcome

Canonical workflow `.github/workflows/c-integration.yml` is live on
`main`, with **12 of 11 canonical jobs** (10 canonical jobs + 2
preserved project-specific) and the legacy 4-job workflow deleted in
the same commit.

The canonical `docs` job (MkDocs) was deliberately removed per ADR-003
because this service will publish documentation via VitePress in
feature 004; the upstream `dependency-graph` and `gource` artifacts
that fed `docs` are kept in place because feature 004 will consume
them.

## Final job topology (`main` HEAD `5a84182`)

```
setup
  ├─ checkstyle
  ├─ coverage
  ├─ visualization
  ├─ schema-compat                          (preserved, ADR-001)
  └─ api-specs-lint (Spectral)              (preserved, ADR-001)
       ↓
  ├─ quality
  └─ build
       ↓
     test (Acceptance Test)
       ├─ openapi
       ├─ asyncapi
       └─ allure
```

`docs` (MkDocs) removed (ADR-003); reintroduction belongs to feature 004.

## Final CI run evidence

Workflow run [`25660948040`](https://github.com/lg-labs/lg5-loyalty-ledger/actions/runs/25660948040)
on commit `f1cf924`, 12/12 green:

| Job | Time |
|---|---|
| Setup | 28s |
| API specs lint (Spectral) | 13s |
| Checkstyle | 52s |
| Schema-Registry compatibility gate (TASK-014) | 45s |
| Visualization | 1m58s |
| Coverage | 11m35s |
| Quality | 5s |
| Build | 5m37s |
| Acceptance Test | 9m4s |
| OpenAPI | 8s |
| AsyncAPI | 6s |
| Allure Report | 8s |

## Diffs vs. canonical template (deviations)

All deviations are documented by ADRs in
`docs/specs/003-ci-cd-canonical/adr/`. Three exist:

| # | Deviation | ADR | Rationale (summary) |
|---|-----------|-----|---------------------|
| 1 | `schema-compat` job preserved (added post-`setup`, parallel to canonical jobs) | ADR-001 | Project-specific gate from feat 001 TASK-014; required to enforce Avro Schema-Registry backward compatibility on every PR. No canonical equivalent. |
| 2 | `api-specs-lint` (Spectral) job preserved (independent, no `needs:`) | ADR-001 | Project-specific gate from feat 002 TASK-004; lints OpenAPI + AsyncAPI specs. No canonical equivalent. |
| 3 | `docs` (MkDocs) job removed | ADR-003 | This service will publish docs via VitePress in feat 004 (dual-deploy: GitHub Pages + Firebase Hosting on project `lglabs-loyalty`); MkDocs is obsolete here. |

## Vestigial artifacts kept (intentional)

- **`build` → upload `dependency-graph.png` artifact**: produced
  unchanged from canonical template. Was consumed only by removed
  `docs` job. Kept because feat 004 will embed it inside VitePress
  under `architecture/`.
- **`visualization` → upload `gource.mp4` artifact**: same rationale.
  Feat 004 will embed it under `history/`.
- **`build` → upload `firebase-json` artifact** (canonical step
  vestige; ADR-002): the template references `./firebase.json` which
  doesn't exist in this repo; mitigated with `if-no-files-found: warn`
  + `continue-on-error: true`. Will become real in feat 004.

## Pre-conditions verified during execution

| # | Item | Verified |
|---|------|----------|
| 1 | Repo secret `PKG_GITHUB_TOKEN` (read:packages on `lg-labs-pentagon`) | ✓ — Build job pulled framework parent successfully |
| 2 | `.agent-os` submodule at v0.3.5 | ✓ — `461f927` on main pre-feat |
| 3 | `application.yaml` has no `log:` block | ✓ — skill step 7 confirmed N/A |
| 4 | `Makefile` exposes 6 required targets | ✓ — all invoked by canonical jobs |

## Build/test fixes applied during execution

Two follow-up commits beyond the initial scaffold proved necessary;
both stayed in scope:

1. **`0a606b9` — install sibling modules in `test` job before ATDD**.
   Canonical `test` job assumed `mvn install -DskipTests` against the
   acceptance-test module would resolve sibling modules from local
   `.m2`, but the runner doesn't share state with `build`. Added
   explicit `install-skip-test` step against the parent reactor before
   the `run-atdd-module` invocation.
2. **`b85461d` — point openapi/asyncapi jobs at `docs/api/`**. The
   canonical `openapi` and `asyncapi` jobs default-templated to
   `<svc>-api` and `<svc>-message-model` resource roots; in this repo
   the contract sources live under `docs/api/openapi/` and
   `docs/api/asyncapi/` per feat 002. Re-pointed both jobs.

Both fixes are local to this service and do **not** indicate bugs in
the bundle template; they reflect the natural friction of mapping a
generic template onto a specific repo layout.

## TASKs status

All 10 TASKs done (`tasks.md` updated with status column and commit
references).

## Verification gates outcome

| Gate | Outcome |
|---|---|
| Pre-execution (specs + 2 ADRs initial) | ✓ approved by user |
| Mid-execution (actionlint clean) | ✓ exit 0 pre-`f1cf924` push |
| Post-execution (12/12 green) | ✓ run `25660948040` |
| Quality (this report enumerates deviations linked to ADRs) | ✓ |

## Follow-ups → feature 004

Feature 004 (`004-project-docs`) will:

- Reintroduce a documentation job set (3+ jobs) at the canonical DAG
  position vacated by removed `docs` job:
  - `docs-build`: VitePress dual build (`base: '/lg5-loyalty-ledger/'`
    for Pages + `base: '/'` for Firebase). Embeds Swagger UI/AsyncAPI
    Studio HTML wrappers under `api/`. Consumes `dependency-graph` +
    `gource` artifacts.
  - `pages-deploy`: gated `if: github.ref == 'refs/heads/main'`,
    publishes Pages build via `actions/deploy-pages`.
  - `firebase-deploy` (live, site `lglabs-loyalty-docs`): gated
    `if: github.ref == 'refs/heads/main'`.
  - `firebase-deploy-allure` (live, site `lglabs-loyalty-allure`,
    overwrite each main): gated same.
  - `firebase-preview` (PR label `docs/preview`, channel `pr-<N>`,
    7d TTL on `lglabs-loyalty-docs` only).
- Provide a real `firebase.json` + `.firebaserc` (project default
  `lglabs-loyalty`) so the existing `firebase-json` upload-artifact
  step in `build` produces a meaningful artifact.
- Be the first feature in this repo to use the new SDD 7-phase
  workflow (`/sdd-intent → /sdd-specify → /sdd-plan → /sdd-design →
  /sdd-tasks → /sdd-implement → /sdd-verify`) introduced by
  `lg5-spring-agent-os` v3.0.0 — which will be bumped in a separate
  PR (`chore: bump agent-os v0.3.5 → v3.0.0`) before feature 004
  starts.

## Lessons learned (for future M-reports)

- **Template mismatches surface fast and cheap**. Both fixes
  (`0a606b9`, `b85461d`) caught on first PR run; no need to debug
  locally because Build+ATDD log is enough.
- **Defer-by-ADR is cleaner than defer-by-stub**. Removing the
  `docs` job + ADR-003 produced a smaller, more honest diff than
  inserting a placeholder MkDocs config would have.
- **Branch protection `BLOCKED` ≠ git conflict**. Watch for confusion:
  `mergeable: true, mergeable_state: blocked` means policy gate
  (review/checks), not a merge conflict.