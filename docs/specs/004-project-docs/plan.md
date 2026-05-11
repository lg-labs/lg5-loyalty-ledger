---
kind: spec
name: plan
feature: 004-project-docs
version: 0.1.0
description: Technical plan for the loyalty-ledger documentation surface (VitePress + dual deploy + label-gated previews + separate Allure site).
---

# Plan — `004-project-docs`

> Generated from [`prd.md`](prd.md) and the ADRs under [`adr/`](adr/).
> The plan describes the **how** at the architectural level; concrete
> code lives in [`tasks.md`](tasks.md) and the actual repo. Field-level
> contracts (Make-target signatures, workflow input shapes, VitePress
> sidebar config schema) belong to `design.md` (next phase).

## Deviation from RULE-004

RULE-004 prescribes the canonical 8-module Maven shape for lg5-spring
**services** (`-domain-core`, `-application-service`, `-api`,
`-data-access`, `-message-core`, `-message-model`, `-external`,
`-container`, plus `-acceptance-test` and `-support`). **Feature 004 is
a documentation-and-CI feature, not a Maven module.** It introduces no
Java code, no Spring beans, no domain logic, no Avro schemas, no JPA
entities. RULE-004's modules continue to exist unchanged for the
service itself (features 001-003); this feature adds a sibling top-level
directory `docs/` whose contents are Markdown + Node tooling.

Per the constitution, deviating from a `must` rule requires a dedicated
ADR. We did **not** add a 7th ADR for this deviation because RULE-004
does not actually prohibit additional top-level directories alongside
the canonical Maven modules — it prescribes the shape of the **service
itself**. A documentation surface that lives next to the service does
not "violate" the rule any more than the existing `Makefile`, `.github/`
or `docs/specs/` violate it. ADR-001 §"Constitutional impact" notes
this explicitly under "clarifies (deviation)". If a future review
disagrees, a 7th ADR can be added without rewriting the plan.

## Architecture overview

This feature does not produce Maven modules. The deliverables are:

```
<repo-root>/
├── docs/                                       # NEW — VitePress source tree
│   ├── .vitepress/
│   │   ├── config.ts                           # base path read from env var
│   │   └── theme/                              # default theme + minor overrides
│   ├── index.md                                # home page (REQ-001, REQ-002…REQ-007 links)
│   ├── architecture/                           # embeds dependency-graph.png + gource.mp4 (REQ-002)
│   ├── api/                                    # embeds Swagger UI HTML wrapper (REQ-003)
│   ├── events/                                 # embeds AsyncAPI Studio HTML wrapper (REQ-004)
│   ├── adr/                                    # ADR index page (REQ-006)
│   ├── runbook/                                # onboarding runbook stub (REQ-007)
│   └── public/                                 # static assets
├── firebase.json                               # NEW — hosting targets for two sites
├── .firebaserc                                 # NEW — project alias `lglabs-loyalty`
├── package.json                                # NEW — VitePress + firebase-tools devDeps
├── pnpm-lock.yaml (or package-lock.json)       # NEW — lock file
├── Makefile                                    # MODIFIED — add docs targets (RULE-017)
└── .github/workflows/c-integration.yml         # MODIFIED — add docs jobs
```

The existing Maven modules (`loyalty-ledger-domain-*`,
`loyalty-ledger-api`, `loyalty-ledger-data-access`,
`loyalty-ledger-message-*`, `loyalty-ledger-container`,
`loyalty-ledger-acceptance-test`, `loyalty-ledger-support`) are
**not touched** by this feature.

## Deliverable ↔ requirement matrix

Every PRD requirement is covered by ≥1 deliverable.

| Deliverable | Covers REQ |
|---|---|
| `docs/index.md` + `.vitepress/config.ts` (sidebar/nav) | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-016 |
| `docs/architecture/` (embeds `dependency-graph.png` + `gource.mp4`) | REQ-002 |
| `docs/api/` (embeds Swagger UI HTML wrapper from feature 002) | REQ-003 |
| `docs/events/` (embeds AsyncAPI Studio HTML wrapper from feature 002; cross-links the Schema Registry topic — see RULE-007 note) | REQ-004 |
| `docs/runbook/` (stakeholder-authored stub: first-day setup checklist, repo tour, links to the other five entries) | REQ-007 |
| `docs/adr/` (auto-listed index of `docs/specs/**/adr/*.md`) | REQ-006 |
| `.vitepress/config.ts` — built-in local search enabled | REQ-017 |
| `.vitepress/theme/` — visible footer with commit SHA + build timestamp (+ PR id for previews) | REQ-020 |
| `c-integration.yml :: docs-build-pages` job (Build A, `base: '/lg5-loyalty-ledger/'`) | REQ-001, REQ-008, REQ-009, REQ-010, REQ-011, REQ-016 |
| `c-integration.yml :: docs-build-firebase` job (Build B, `base: '/'`) | REQ-001, REQ-008, REQ-009, REQ-010, REQ-011 |
| `c-integration.yml :: pages-deploy` job (live, main only) | REQ-001, REQ-008, REQ-009, REQ-010, REQ-011 |
| `c-integration.yml :: firebase-deploy-docs` job (live, main only, site `lglabs-loyalty-docs`) | REQ-001, REQ-008, REQ-009, REQ-010, REQ-011 |
| `c-integration.yml :: firebase-deploy-allure` job (live, main only, site `lglabs-loyalty-allure`) | REQ-005, REQ-008 |
| `c-integration.yml :: firebase-preview` job (PR, label-gated `docs/preview`, 7-day TTL, channel `pr-<N>` on `lglabs-loyalty-docs`) | REQ-012, REQ-013, REQ-014, REQ-015, REQ-020 |
| `firebase.json` + `.firebaserc` at repo root | REQ-001, REQ-005, REQ-008, REQ-013 |
| `Makefile` additions (`docs-install`, `docs-build-pages`, `docs-build-firebase`, `docs-preview`, `docs-deploy-pages`, `docs-deploy-firebase`) | RULE-017 enforcement; underlies all docs REQs |
| VitePress build behavior on missing source artifact (stale or "no content yet" placeholder) + broken-link warning | REQ-018, REQ-019 |

Every REQ-001…REQ-020 appears in the table.

## ADR index

- [ADR-001](adr/ADR-001-vitepress-site-engine.md) — Use VitePress as the documentation site engine.
- [ADR-002](adr/ADR-002-dual-deploy-pages-and-firebase.md) — Dual-deploy to GitHub Pages and Firebase Hosting.
- [ADR-003](adr/ADR-003-allure-separate-firebase-site.md) — Allure on a separate Firebase site, cross-linked (not embedded).
- [ADR-004](adr/ADR-004-dual-base-path-build.md) — Build VitePress twice with different `base` paths.
- [ADR-005](adr/ADR-005-preview-channels-label-gated.md) — Per-PR Firebase preview channels, label-gated, 7-day TTL.
- [ADR-006](adr/ADR-006-upstream-vitepress-skill.md) — Upstream the pattern as bundle skill `lg5-vitepress-docs` in v4.0.0.

## Sequenced steps

Full atomic decomposition belongs to `tasks.md` (next phase). High-level
dependency sketch:

```
docs-scaffold ──► docs-content-stub ──► docs-build-local ──┐
                                                            │
firebase-config ───────────────────────────────────────────┤
                                                            ▼
                                              ci-jobs-docs-build ──┬─► ci-jobs-deploy-live
                                                                   │     (pages + firebase-docs
                                                                   │      + firebase-allure)
                                                                   │
                                                                   └─► ci-jobs-deploy-preview
                                                                         (firebase, label-gated)
```

`ci-jobs-*` consume the existing `dependency-graph`, `gource`, and
`allure` artifacts from the feature-003 workflow (no upstream
rework — confirmed by feature 003 ADR-003 §"Follow-up commitment").

## Cross-cutting concerns

- **Topics / channels:** N/A — no Kafka traffic in this feature.
  Asynchronous service contract docs (REQ-004) reference the Schema
  Registry topic information already shipped by feature 002.
- **Schema registry:** N/A directly — RULE-007 is not modified.
  `docs/events/` cross-links the Schema Registry browser URL (read-only
  reference). **Owner**: docs author (stakeholder).
- **Observability:** the source-state indicator (REQ-020) on every
  surface is the in-band observability for "did the regeneration
  succeed?". Broken-link warnings (REQ-018) are emitted to the CI job
  log. No external dashboards are added. **Owner**: stakeholder.
- **Security:** Firebase service account stored as a GitHub Actions
  secret (`FIREBASE_SERVICE_ACCOUNT_LGLABS_LOYALTY`). Pages uses the
  default `GITHUB_TOKEN` with `pages: write, id-token: write`
  permissions on the deploy job only. No new secrets beyond Firebase.
  **Owner**: stakeholder (configures secret); CI maintainer
  (workflow YAML).
- **Data lifecycle:** the surface is overwrite-on-advance (REQ-011);
  no archival. Preview channels auto-expire at 7 days (REQ-014).
  **Owner**: Firebase Hosting (built-in mechanism).
- **External dependencies (verified pre-conditions, 2026-05-10):**
  Firebase project `lglabs-loyalty` exists; sites
  `lglabs-loyalty-docs` and `lglabs-loyalty-allure` exist; Pages
  enabled with source = "GitHub Actions"; label `docs/preview`
  exists on the repo. See Risks for what happens if any of these
  drift.
- **Build-tool baseline:** Node + a Node package manager (pnpm or
  npm — to be chosen in `design.md`). JVM stack (RULE-001) is
  unchanged.

## Risks

| ID | Risk | Mitigation | Owner |
|----|------|------------|-------|
| R1 | Firebase service-account secret missing or rotated on the GitHub side; live deploy and preview both fail. | Capture the exact secret name in `design.md`; add a CI-time precheck job that fails fast with a clear message; stakeholder verifies before first run. | stakeholder |
| R2 | Pre-condition drift (Firebase site renamed, Pages source flipped to "Deploy from a branch", label `docs/preview` renamed). | The CI jobs name these explicitly; any drift produces a deterministic CI failure. Document the four pre-conditions at the top of `c-integration.yml`'s docs section. | stakeholder |
| R3 | PR previews from forked repositories cannot access the Firebase secret (GitHub Actions default). | Document the limitation in the onboarding runbook (REQ-007); the preview job is a no-op on fork PRs and logs a clear notice. Acceptable because this is an internal training project with no expected fork contributions. | stakeholder |
| R4 | Pages `base` path breaks if the repo is renamed away from `lg5-loyalty-ledger`. | The `base` value lives in one environment variable consumed by ADR-004's two builds; a repo rename requires updating one workflow input. Documented in ADR-004. | CI maintainer |
| R5 | Dual-build doubles VitePress build time per CI run. | VitePress build is fast (<1 min on this content size); acceptable. Re-evaluate if `docs/` grows to >500 pages. | CI maintainer |
| R6 | The "stale indicator" / "no content yet" placeholder behavior (REQ-019) requires the build to detect missing upstream artifacts. The exact detection mechanism is a `design.md` concern. | Defer mechanism choice to design phase; flag as an open design decision. | sdd-designer |
| R7 | Broken-link warning (REQ-018) requires VitePress to not fail the build on dead links; the default VitePress behavior is to fail. | Configure `vitepress` `ignoreDeadLinks` plus a separate post-build link-check step that emits warnings without failing. Concrete config in `design.md`. | sdd-designer |
| R8 | The upstream commitment (ADR-006) creates a backport window. | Keep the window short (target: same week as feature 004 merge). Tracked outside this feature. | stakeholder |

(R6 and R7 are the only items the PRD §8 left as "to be resolved by
design" rather than by clarification — all eight clarifications in PRD
§8 were resolved before plan time.)

## Estimated artifact count

- New files: ~25 (VitePress scaffold + stub content + Firebase config
  + package manifest + lock file).
- Modified files: 2 (`Makefile`, `.github/workflows/c-integration.yml`).
- New tests: 0 unit; 0 ATDD. CI smoke validation is implicit in the
  successful publish of the surface.

## Definition of Done (Plan)

- [x] Every PRD requirement is covered by ≥1 deliverable in the
      matrix above.
- [x] Every architectural decision is captured as an ADR under `adr/`
      (six ADRs; the deviation from RULE-004 is addressed inline in
      this plan with explicit justification, not promoted to a 7th ADR
      because the rule scopes the service-module shape, not the
      coexistence of sibling top-level directories).
- [x] Constitutional rule interactions explicitly listed: each ADR's
      "Constitutional impact" section names every relevant `must`
      rule. No rule is overridden.
- [ ] Module map matches RULE-004 — **deferred / not applicable.** See
      "Deviation from RULE-004" above. This is the only unchecked
      DoD box; flagging it explicitly.
- [x] Open questions explicitly listed — see PRD §8 (all resolved) and
      Risks R6, R7 (deferred to design phase).
- [x] All cross-cutting concerns assigned to a team/owner.
