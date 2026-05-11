---
kind: spec
name: design
feature: 004-project-docs
version: 0.1.0
description: Detailed technical design for the loyalty-ledger documentation surface (VitePress + dual deploy + label-gated previews + Allure cross-link). Companion to plan.md and adr/*.
---

# Design — `004-project-docs`

> Generated from [`prd.md`](prd.md) + [`plan.md`](plan.md) + [`adr/`](adr/).
> This design pins the **non-Java configuration surface** (VitePress
> config, Firebase config, `package.json`, Makefile targets, CI jobs)
> for a docs-and-CI feature. Most template sections about Spring/JPA/
> Kafka/Saga/Outbox are explicitly skipped in §10 — feature 004
> introduces no Java, no domain logic, no persistent state, no events,
> no DTOs and no schemas. See [`plan.md`](plan.md) §"Deviation from
> RULE-004" for the rationale on the absence of a Maven module.
>
> Constitutional rules cited by stable RULE-ID
> ([`rules/CONSTITUTION.md`](../../../.agent-os/rules/CONSTITUTION.md)).

## 1. Scope and boundaries

**In design** (everything below has a deliverable row in [`plan.md`](plan.md) §"Deliverable ↔ requirement matrix"):

- VitePress source tree under `docs/` — scaffold + stub content per
  required entry (covers REQ-001, REQ-002…REQ-007, REQ-016).
- `docs/.vitepress/config.ts` — site title, nav, sidebar, **local
  search**, `base` from env var, `ignoreDeadLinks: true` (covers
  REQ-001, REQ-002…REQ-007, REQ-016, REQ-017, REQ-018).
- `docs/.vitepress/theme/` — default theme + layout slot override for a
  **source-state footer** (covers REQ-020).
- `firebase.json` + `.firebaserc` at repo root — two `hosting` targets
  (`docs` → `lglabs-loyalty-docs`, `allure` → `lglabs-loyalty-allure`)
  (covers REQ-001, REQ-005, REQ-008, REQ-013).
- `package.json` + lock file — pinned VitePress + firebase-tools
  (covers the runtime for all docs jobs).
- **Pre-build artifact-presence script** `scripts/check-artifacts.mjs`
  — emits stale / "no content yet" placeholders (covers REQ-019).
- **Post-build link-checker step** — emits warnings without failing
  (covers REQ-018).
- `Makefile` additions: `docs-install`, `docs-build-pages`,
  `docs-build-firebase`, `docs-preview-local`, `docs-deploy-pages`,
  `docs-deploy-firebase` (RULE-017).
- `.github/workflows/c-integration.yml` additions — six jobs:
  `docs-build-pages`, `docs-build-firebase`, `pages-deploy`,
  `firebase-deploy-docs`, `firebase-deploy-allure`, `firebase-preview`
  (covers REQ-008…REQ-015, REQ-020).

**Out of design**:

- Production Java code, Spring beans, JPA, Kafka, Avro, Saga, Outbox
  — none of which this feature introduces (see §10).
- The Swagger UI HTML wrapper and AsyncAPI HTML wrapper — shipped by
  feature 002, this feature only embeds them.
- Allure XML/HTML generation itself — shipped by feature 003, this
  feature only deploys the existing artifact to Firebase.
- The upstream bundle skill `lg5-vitepress-docs` — deferred to ADR-006
  follow-up PR.

**REQ ↔ section coverage** (full matrix in the final report):

- REQ-001…REQ-007, REQ-016 — §1 (deliverables), §7 (config), §8 (graph), §9 (test).
- REQ-008…REQ-011 — §7 (CI workflow), §8 (graph), §9.
- REQ-012…REQ-015 — §7 (preview job), §8, §9.
- REQ-017 — §7 (`themeConfig.search`), §9.
- REQ-018 — §7 (link-check), §9.
- REQ-019 — §7 (artifact-check script), §9.
- REQ-020 — §7 (source-state footer), §9.

## 2. Domain model (sketch)

**Skipped — see §10.**

## 3. REST contracts (RULE-006)

**Skipped — see §10.**

## 4. Kafka contracts (RULE-007, RULE-010)

**Skipped — see §10.**

## 5. Persistence model (RULE-008)

**Skipped — see §10.**

## 6. Saga design (RULE-009)

**Skipped — see §10.**

## 7. Configuration

> RULE-014 prescribes canonical **Spring** property prefixes. There is
> no Spring configuration in this feature; **reinterpretation**: this
> section pins the canonical configuration surfaces for the
> non-Java tooling (VitePress, Firebase, `package.json`, CI inputs,
> Makefile). Where RULE-014 names exist (e.g. `<svc>-service.*`) they
> are unchanged by this feature.

### 7.1 Package manager

**Decision: pnpm.** Rationale: stricter lockfile semantics align with
the project's `mvn`/`gradle` philosophy of reproducible builds; smaller
`node_modules`; better CI cache story (single `~/.local/share/pnpm/store`
mount across jobs). One-line policy: pinned via `package.json#packageManager`
and the `pnpm/action-setup@v3` GitHub Action.

### 7.2 `package.json` (repo root)

```jsonc
{
  "name": "lg5-loyalty-ledger-docs",
  "private": true,
  "type": "module",
  "packageManager": "pnpm@9.x",
  "scripts": {
    "docs:dev":     "vitepress dev docs",
    "docs:build":   "node scripts/check-artifacts.mjs && vitepress build docs",
    "docs:preview": "vitepress preview docs"
  },
  "devDependencies": {
    "vitepress":      "^1.6.0",
    "firebase-tools": "^13.0.0",
    "linkinator":     "^6.0.0"
  }
}
```

Version-pin policy: caret-pinned (`^x.y.z`) at scaffold time; bumps
governed by Renovate / Dependabot — no manual sweeps. Confirm latest
stable at scaffold time; the values above are starting points.

### 7.3 `docs/.vitepress/config.ts`

```ts
import { defineConfig } from 'vitepress';

const base = process.env.DOCS_BASE ?? '/';      // '/' (Firebase) or '/lg5-loyalty-ledger/' (Pages)

export default defineConfig({
  title:       'loyalty-ledger',
  description: 'Reference documentation for the loyalty-ledger service.',
  base,
  srcDir:      '.',
  cleanUrls:   true,
  ignoreDeadLinks: true,                        // REQ-018 — warn-not-fail
  themeConfig: {
    nav: [
      { text: 'Architecture',        link: '/architecture/' },
      { text: 'API (sync)',          link: '/api/' },
      { text: 'Events (async)',      link: '/events/' },
      { text: 'ADRs',                link: '/adr/' },
      { text: 'Acceptance Report',   link: 'https://lglabs-loyalty-allure.web.app/' },
      { text: 'Runbook',             link: '/runbook/' }
    ],
    sidebar: {
      '/architecture/': [{ text: 'Overview', link: '/architecture/' }],
      '/api/':          [{ text: 'Synchronous contract',  link: '/api/' }],
      '/events/':       [{ text: 'Asynchronous contract', link: '/events/' }],
      '/adr/':          [{ text: 'Decision records',      link: '/adr/' }],
      '/runbook/':      [{ text: 'Onboarding runbook',    link: '/runbook/' }]
    },
    search: { provider: 'local' }               // REQ-017
  }
});
```

The `DOCS_BASE` env var is exported by the two CI build jobs and the
two Makefile build targets — see §7.6.

### 7.4 Source-state footer (REQ-020)

**Decision: layout-slot override** (not a full theme fork). File:
`docs/.vitepress/theme/index.ts` extends `DefaultTheme` and registers a
slot component `docs/.vitepress/theme/SourceStateFooter.vue`. The
component reads three values **baked in at build time** via Vite's
`define` mechanism (so they survive the static build):

- `__COMMIT_SHA__`  — short SHA, from CI env `GITHUB_SHA` (truncated to 7).
- `__BUILD_TIME__`  — ISO-8601 UTC timestamp captured at build start.
- `__PR_NUMBER__`   — optional, from CI env `PR_NUMBER`; empty on main
  builds.

Rendering shape (text-only, no styling beyond default theme):

> Built from `abc1234` · 2026-05-11T14:22:08Z · _(PR #123)_

The `define` map is set in `docs/.vitepress/config.ts` via `vite.define`.
The same component is rendered on every page through the default theme
layout's `layout-bottom` slot.

### 7.5 `firebase.json` and `.firebaserc`

```jsonc
// firebase.json
{
  "hosting": [
    {
      "target":        "docs",
      "public":        "docs/.vitepress/dist",
      "ignore":        ["firebase.json", "**/.*", "**/node_modules/**"],
      "cleanUrls":     true,
      "trailingSlash": false
    },
    {
      "target":        "allure",
      "public":        "allure-report",
      "ignore":        ["firebase.json", "**/.*", "**/node_modules/**"],
      "cleanUrls":     false,
      "trailingSlash": false
    }
  ]
}
```

```jsonc
// .firebaserc
{
  "projects": { "default": "lglabs-loyalty" },
  "targets": {
    "lglabs-loyalty": {
      "hosting": {
        "docs":   ["lglabs-loyalty-docs"],
        "allure": ["lglabs-loyalty-allure"]
      }
    }
  }
}
```

### 7.6 `Makefile` targets (RULE-017)

| Target                 | Underlying invocation                                                                              |
|------------------------|----------------------------------------------------------------------------------------------------|
| `docs-install`         | `pnpm install --frozen-lockfile`                                                                   |
| `docs-build-pages`     | `DOCS_BASE='/lg5-loyalty-ledger/' pnpm run docs:build`                                             |
| `docs-build-firebase`  | `DOCS_BASE='/' pnpm run docs:build`                                                                |
| `docs-preview-local`   | `pnpm run docs:dev` (dev server, `base: '/'`)                                                      |
| `docs-deploy-pages`    | _CI-only in practice._ Documented for parity; locally a developer cannot publish to Pages.         |
| `docs-deploy-firebase` | `pnpm exec firebase deploy --only hosting:docs --project lglabs-loyalty` (CI-equivalent)           |

`docs-deploy-pages` and `docs-deploy-firebase` are local-convenience
parity wrappers; production publication is **CI-only** (requires
secrets that should not live on developer machines).

### 7.7 Broken-link policy (REQ-018)

Two layers:

1. **VitePress build**: `ignoreDeadLinks: true` in `config.ts`. Build
   no longer fails on intra-site dead links (default would fail).
2. **CI post-build link checker** (separate step in each docs-build
   job, against the static `dist/`):

   ```bash
   pnpm exec linkinator docs/.vitepress/dist \
     --recurse \
     --skip "^https?://(?!(lglabs-loyalty-docs|lglabs-loyalty-allure)\.web\.app|.+\.github\.io/lg5-loyalty-ledger)" \
     --silent || true
   ```

   - Runs against the local `dist/` (filesystem) — no need to wait for
     the deploy URL to be live.
   - `--skip` allow-lists the two production hostnames + the Pages URL
     pattern; everything else (external links) is not crawled to keep
     the check deterministic.
   - Trailing `|| true` ensures the step **never** fails the job;
     the report is captured as a CI annotation via
     `::warning::` lines emitted by a small wrapper script
     (`scripts/linkinator-to-annotations.mjs`, ~20 LOC).

### 7.8 Stale / placeholder policy (REQ-019)

A pre-build Node ES-module script `scripts/check-artifacts.mjs` runs
**before** `vitepress build` (wired into the `docs:build` npm script
above). For each expected upstream artifact, if the file is missing on
disk, the script writes a stub `.md` fragment under
`docs/<section>/_placeholder.md` and emits a CI warning via
`::warning file=…::`. A `<section>/index.md` includes the placeholder
via VitePress include syntax (`<!--@include: ./_placeholder.md-->`).

| Expected artifact                                  | Section            | Placeholder copy                                                                                                                  |
|----------------------------------------------------|--------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `docs/public/dependency-graph.png`                 | `architecture/`    | "The dependency graph was not produced in the most recent CI run."                                                                |
| `docs/public/gource.mp4`                           | `architecture/`    | "The repository activity visualization was not produced in the most recent CI run."                                               |
| `docs/api/swagger-ui.html`                         | `api/`             | "The synchronous service contract (Swagger UI) was not produced in the most recent CI run."                                       |
| `docs/events/asyncapi.html`                        | `events/`          | "The asynchronous service contract (AsyncAPI) was not produced in the most recent CI run."                                        |
| Allure URL reachability (HTTP HEAD probe, optional)| (cross-link)       | (No on-disk artifact — the link from the home is rendered unconditionally; the Allure site itself owns its own "no data" page.)   |

Stale-vs-placeholder distinction: if a previous version of the
artifact exists on the deploy target, Pages/Firebase serve the
previous version (overwrite-on-advance — REQ-011 — applies to the
**whole** site upload, but missing files in the new upload leave the
previous file unchanged on Firebase by default; on Pages this is more
binary, hence the placeholder fallback). The placeholder is the
deterministic mechanism; the "stale-with-indicator" path is best-effort
on Firebase and is communicated by the source-state footer (the SHA
makes staleness observable).

### 7.9 Workflow inputs and secrets

Single new secret: `FIREBASE_SERVICE_ACCOUNT_LGLABS_LOYALTY` (already
captured as Risk R1 in `plan.md`). No new variables. Pages deploy uses
the default `GITHUB_TOKEN` with `pages: write, id-token: write` on the
deploy job only.

Workflow jobs (added to `.github/workflows/c-integration.yml`):

| Job                       | Trigger                                | Build base               | Deploys to                                          |
|---------------------------|----------------------------------------|--------------------------|-----------------------------------------------------|
| `docs-build-pages`        | `push:main` + `pull_request`           | `/lg5-loyalty-ledger/`   | uploads artifact `docs-dist-pages`                  |
| `docs-build-firebase`     | `push:main` + `pull_request`           | `/`                      | uploads artifact `docs-dist-firebase`               |
| `pages-deploy`            | `push:main` only, needs build-pages    | n/a                      | GitHub Pages (`pages: write, id-token: write`)      |
| `firebase-deploy-docs`    | `push:main` only, needs build-firebase | n/a                      | Firebase `lglabs-loyalty-docs` (live)               |
| `firebase-deploy-allure`  | `push:main` only                       | n/a                      | Firebase `lglabs-loyalty-allure` (live)             |
| `firebase-preview`        | `pull_request` + label `docs/preview`  | `/` (reuses Build B)     | Firebase channel `pr-<N>` on `lglabs-loyalty-docs`  |

`firebase-preview` job's `if:` guard:
`${{ github.event.pull_request != null && contains(github.event.pull_request.labels.*.name, 'docs/preview') }}`,
plus a fork-skip clause documented in plan.md R3.

## 8. Module dependency graph

Reinterpreted as **deliverable dependency graph** (the feature ships
no Maven module — see plan.md §"Deviation from RULE-004"). Inputs from
features 002/003 are shown to clarify boundaries. The graph is acyclic.

```
                ┌───────────────────────────────────┐
                │ feature-002 / feature-003 outputs │
                │  swagger-ui.html, asyncapi.html,  │
                │  dependency-graph.png, gource.mp4,│
                │  allure-report/                   │
                └──────────────┬────────────────────┘
                               │ (CI artifacts download)
                               ▼
  docs/ source ──► scripts/check-artifacts.mjs ──► vitepress build
                                                      │
                          DOCS_BASE='/lg5-loyalty-ledger/'   DOCS_BASE='/'
                                      │                          │
                                      ▼                          ▼
                       artifact: docs-dist-pages       artifact: docs-dist-firebase
                                      │                          │
                  ┌───────────────────┘                          │
                  │                                              │
                  ▼                                              ▼
            pages-deploy                                ┌────────┴───────────┐
                                                       │                    │
                                              firebase-deploy-docs  firebase-preview
                                              (main only, live)     (PR + label, channel)

  allure-report/  ──────────────────────► firebase-deploy-allure (main only, live)
```

Justification for new edges: every edge follows directly from an
accepted ADR — ADR-002 (dual deploy), ADR-003 (separate Allure site),
ADR-004 (two builds with different base), ADR-005 (label-gated preview).

## 9. Test design

> The PRD ships 4 feature-level acceptance criteria headers in §7 and 10
> bullet criteria total. There is no Java code path; therefore there
> are **no unit tests, no Integration Tests (`Lg5TestBoot`), and no
> ATDD (Cucumber)** in this feature. RULE-012/RULE-013 do not apply.
> Validation lives in CI smoke + manual visual inspection.

| AC (PRD §7 bullet)                                                                       | Validation                                                                                                                                                                                  |
|------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| First-time visitor can reach the six core docs in one click                              | Local: `make docs-install && make docs-build-firebase`, open `dist/index.html`, confirm nav. CI smoke: `docs-build-firebase` succeeds.                                                       |
| Trunk advance regenerates the surface, <10 min, no human action                          | CI smoke: `pages-deploy` + `firebase-deploy-docs` complete on `push:main`. Wall-clock from push to live ≤10 min (REQ-010) — verified by CI run duration on first main merge.                |
| `docs/preview` label produces a preview URL; no label → no preview                       | CI smoke: open a throwaway PR without label → `firebase-preview` job skips via `if:`. Add label → job runs, comments preview URL on the PR.                                                  |
| Preview reachable 7+ days, auto-expires                                                  | Firebase channel TTL set to `7d` in `firebase hosting:channel:deploy --expires 7d` invocation. Manual probe at day 7+1.                                                                       |
| All entries reachable without authentication from a fresh environment                    | Manual probe in incognito browser on the Pages URL and the Firebase docs URL after first main merge.                                                                                          |
| Source-state indicator visible on every surface (and PR id on previews)                  | Inspect rendered footer on the local preview build (`make docs-preview-local`), then on the deployed Pages, Firebase docs, and preview URLs.                                                  |
| Local search returns ≥1 result for a term known to appear in any of the six core docs    | Manual smoke: type the service name "loyalty-ledger" in the local preview's search box, confirm ≥1 hit.                                                                                       |
| Missing source artifact → stale or "no content yet" placeholder, surface still serves    | CI smoke: temporarily rename one of the expected artifacts (e.g. `dependency-graph.png`) in a branch, run `make docs-build-firebase`, confirm placeholder appears and exit code is 0.        |
| Broken internal link → warning, not failure                                              | CI smoke: introduce a deliberate broken link in a scaffold stub during initial PR, confirm `linkinator` step emits a `::warning::` and the job exits 0.                                       |
| Success-metric targets in §4 are instrumentable                                          | Each of the four metrics either (a) is observable from the GitHub Actions logs (lag, share of advances reflected), or (b) requires post-launch survey infrastructure flagged as out-of-scope. |

Test profiles (`@ActiveProfiles({"test","local"})`, RULE-012) and
Testcontainers (RULE-013) are **N/A** — no JVM test runtime.

## 10. Skipped sections (with justification)

- **§2 (Domain model)** — _N/A: the feature introduces no aggregates,
  entities, or value objects. No Java domain code is produced._
- **§3 (REST contracts, RULE-006)** — _N/A: no `@RestController`, no
  REST endpoints, no DTOs. The synchronous service contract docs
  (REQ-003) embed the Swagger UI HTML already shipped by feature 002._
- **§4 (Kafka contracts, RULE-007, RULE-010)** — _N/A: no Kafka
  producers, consumers, or Avro schemas in this feature. The
  asynchronous service contract docs (REQ-004) cross-link the Schema
  Registry and embed the AsyncAPI HTML already shipped by feature 002._
- **§5 (Persistence model, RULE-008)** — _N/A: no JPA entities, no
  tables, no Flyway migrations, no Outbox. The feature is stateless
  (build-and-publish only)._
- **§6 (Saga design, RULE-009)** — _N/A: no sagas; nothing to
  orchestrate._
- **`data-model.md` (whole companion document)** — _Skipped:
  introduces no persistent state, no domain events, no outbox
  payloads, no REST DTOs, no Avro schemas. Per
  [`sdd-designer.md` step 4](../../../.agent-os/subagents/sdd-designer.md),
  the data-model document is produced **iff** any of those exist;
  for feature 004 none do._
- **RULE-001 (stack baseline)** — _Touched only to assert
  invariance: the Node + pnpm tooling is **additive** to the JVM
  baseline (Spring Boot 3.4.2, Spring 6.2.2, JDK 21, Kotlin 21,
  Maven for services). The JVM baseline is unchanged._
- **RULE-004 (service-module-shape)** — _Deviation already
  justified inline in [`plan.md`](plan.md) §"Deviation from
  RULE-004"; not re-justified here._

## 11. Open questions

> Anything that surfaced during design that the PRD/Plan did not foresee.
> If a question changes a Plan ADR, STOP and re-run `/sdd-plan`.

| Question                                                                                                          | Impact         | Decider         | Due                  |
|-------------------------------------------------------------------------------------------------------------------|----------------|-----------------|----------------------|
| Confirm latest stable VitePress / firebase-tools / linkinator at scaffold time; adjust caret pins if drifted.     | Design (minor) | sdd-implementer | scaffold task        |
| Should `linkinator` follow external links too, with a longer allow-list, once the team has operational data?      | Design (minor; warn-not-fail keeps it safe either way) | stakeholder | post-launch, not blocking |
| Confirm the Firebase preview job's auto-comment-with-URL on the PR is acceptable (or should remain log-only)?     | Design (minor UX) | stakeholder  | scaffold task        |

None of the above impacts an ADR.

## Definition of Done (Design)

- [x] Every REQ-NNN from the PRD maps to ≥1 section (see §1 coverage
      summary + the final report matrix). REQ-001…REQ-020 all covered.
- [x] Every section either has content or appears in §10 with
      justification (§2–§6 skipped with one-line N/A each).
- [x] All constitutional rules touched are cited by stable RULE-ID
      (RULE-001, RULE-004, RULE-014, RULE-017, RULE-018; the Kafka/JPA/
      Saga/REST rules RULE-006/007/008/009/010/011/012/013 are
      explicitly N/A in §10).
- [x] All DTOs are records (RULE-015) — N/A (no DTOs).
- [x] All Kafka payloads have an Avro schema referenced (RULE-007) —
      N/A (no Kafka).
- [x] Every event-emitting aggregate has an outbox entry (RULE-008) —
      N/A (no aggregates).
- [x] Every `SagaStep<T>` has process + rollback semantics (RULE-009) —
      N/A (no sagas).
- [x] Module dependency graph has no cycles and matches RULE-004 —
      reinterpreted as the deliverable dependency graph in §8;
      RULE-004 deviation already justified in `plan.md`.
- [x] Configuration uses canonical prefixes (RULE-014) — reinterpreted
      in §7 for the non-Spring config surface; the canonical Spring
      prefixes are unchanged by this feature.
- [x] Test design maps every AC to a concrete validation home (§9).
- [x] Open questions explicitly listed (§11).
- [x] [`data-model.md`](data-model.md) cross-references resolved —
      data-model.md is intentionally absent; the skip is recorded in
      §10.
