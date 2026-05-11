---
kind: spec
name: tasks
feature: 004-project-docs
version: 0.2.0
description: Atomic TASK list for the loyalty-ledger documentation surface (VitePress + dual deploy + label-gated previews + Allure cross-link). Decomposed from plan.md and design.md. Regenerated after layout decision (commit 42424bc) — all Node tooling now lives under docs/site/.
---

# Tasks — `004-project-docs`

> Generated from [`plan.md`](plan.md) and [`design.md`](design.md). Each
> task is atomic (≤1 day, 1-3 commits), references its source REQ-NNN,
> and has Given/When/Then acceptance criteria.
>
> `/sdd-implement <task-id>` consumes one task at a time and updates its
> `Status` field upon successful commit.
>
> **Regenerated 2026-05-11** after the layout decision recorded in
> [`design.md` §1 + §11](design.md): all Node tooling (`package.json`,
> `pnpm-lock.yaml`, `firebase.json`, `.firebaserc`, `.vitepress/`,
> Markdown source, `scripts/`, `allure-dist/`) lives **inside
> `docs/site/`**, not at the repo root. The previous tasks.md (commit
> `8a87c71`) was discarded (commit `42424bc`) because every path needed
> updating. Same overall shape (12 TASKs, same DAG, same REQ coverage),
> updated paths everywhere.

## Preamble — feature shape (read before consuming any TASK)

This feature is **docs-and-CI**, not a Maven service. Per [`plan.md`
§"Deviation from RULE-004"](plan.md) and [`design.md` §10](design.md),
it introduces **zero** Java/Spring/Kafka/JPA/Saga/Avro code. As a
consequence, throughout this TASK list:

- The **"Modules touched"** field names **deliverables** (e.g.
  `docs/site/`, `docs/site/firebase.json`, `Makefile`,
  `.github/workflows/c-integration.yml`, `docs/site/scripts/`) rather
  than Maven modules. This feature touches no Maven module.
- The repo root only gains: Makefile additions (six `docs-*` targets
  that `cd docs/site` first per [`design.md` §7.6](design.md)) and the
  six docs jobs in `.github/workflows/c-integration.yml` (each with
  `working-directory: docs/site` per [`design.md` §7.9](design.md)).
- The **"Skill"** field is `lg5-vitepress-docs (future, see ADR-006)`
  for VitePress / Firebase / Node-tooling work — that bundle skill is
  scheduled to ship in v4.0.0 of `lg5-spring-agent-os` as the ADR-006
  follow-up. For CI workflow work the existing skill is
  `lg5-github-actions`. For Makefile-only additions no specific skill
  applies.
- The **"Command / Subagent"** field is `(none — manual implementation
  following design.md §<N>)` for all TASKs. The future
  `/scaffold-docs` command (ADR-006 follow-up) does not exist yet;
  `/scaffold-service`, `/add-saga`, `/add-outbox`, `/add-kafka-listener`
  are all N/A.
- The **last TASK is NOT** the canonical "all ATDD scenarios green +
  zero `must` violations from `lg5-code-reviewer`" — there is no ATDD
  in this feature (per [`design.md` §9](design.md): no JVM test runtime,
  RULE-012/RULE-013 N/A). The final TASK is the **visual-smoke + final
  verification** TASK-012 below.

## Dependency DAG (verified acyclic)

```
TASK-001 (docs/site manifest) ─┬─► TASK-002 (vitepress scaffold) ─┬─► TASK-003 (check-artifacts.mjs)
                               │                                  │
                               │                                  ├─► TASK-010 (runbook stub)
                               │                                  │
                               ├─► TASK-004 (linkinator wrapper)  │
                               │                                  │
                               │                                  ▼
                               ├─► TASK-006 (Makefile targets) ──► TASK-007 (CI build jobs) ──► TASK-008 (CI deploy live)
                               │       ▲                              │                         ▲
                               │       │                              ├─► TASK-009 (CI preview) ┤
                               │       │                              │                         │
                               │       │                              ▼                         │
                               │       │                         TASK-011 (synthetic validation)│
                               │       │                                                        │
                               │       │                                                        ▼
TASK-005 (firebase config) ────┴───────┘                                                  TASK-012 (final verification)
```

Dependency list (machine-readable):

- TASK-001: —
- TASK-002: TASK-001
- TASK-003: TASK-002
- TASK-004: TASK-001
- TASK-005: —
- TASK-006: TASK-001, TASK-005
- TASK-007: TASK-002, TASK-003, TASK-004, TASK-006
- TASK-008: TASK-005, TASK-007
- TASK-009: TASK-005, TASK-007
- TASK-010: TASK-002
- TASK-011: TASK-007
- TASK-012: TASK-008, TASK-009, TASK-010, TASK-011

No cycles. TASK-001 and TASK-005 are roots; TASK-012 is the unique sink.

---

## TASK-001 — Add `docs/site/` Node manifest and pnpm scaffold

- **Status:** done (commit `e141dcd`)
- **References:** REQ-001, REQ-016 (foundational precondition for all docs deliverables); ADR-001 (VitePress)
- **Depends on:** —
- **Modules touched:** `docs/site/package.json`, `docs/site/pnpm-lock.yaml`, `.gitignore` (deliverables — no Maven module)
- **Skill:** `lg5-vitepress-docs (future, see ADR-006)`
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.1, §7.2](design.md))
- **Acceptance:**
  - **Given** a clean checkout of `feature/004-project-docs` with no
    `docs/site/` directory and no Node tooling at repo root,
  - **When** the implementer creates `docs/site/package.json` per
    [`design.md` §7.2](design.md) (with `packageManager: "pnpm@9.x"`,
    `type: "module"`, the three `docs:*` scripts that invoke
    `vitepress … .` (current dir), and the three `devDependencies`
    `vitepress`, `firebase-tools`, `linkinator` — confirming **latest
    stable major version of `vitepress`, `firebase-tools`, `linkinator`
    at scaffold time** per [`design.md` §11 Q1](design.md) and
    adjusting the caret pins if drifted), generates
    `docs/site/pnpm-lock.yaml` via `cd docs/site && pnpm install`,
    and appends `docs/site/node_modules/`,
    `docs/site/.vitepress/dist/`, `docs/site/.vitepress/cache/`,
    `docs/site/allure-dist/`, `docs/site/.firebase/`, and
    `docs/site/firebase-debug.log` to the repo-root `.gitignore`,
  - **Then** running `cd docs/site && pnpm install --frozen-lockfile`
    from a fresh clone succeeds with exit code 0, and `git status`
    reports a clean working tree (no untracked
    `docs/site/node_modules/`, `docs/site/.vitepress/dist/`, or
    `docs/site/allure-dist/`).

## TASK-002 — Scaffold the VitePress site under `docs/site/`

- **Status:** done (commit `ba046a2`)
- **References:** REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-016, REQ-017, REQ-018, REQ-020; ADR-001 (VitePress); ADR-004 (dual base-path build)
- **Depends on:** TASK-001
- **Modules touched:** `docs/site/.vitepress/`, `docs/site/index.md`, `docs/site/architecture/`, `docs/site/api/`, `docs/site/events/`, `docs/site/adr/`, `docs/site/runbook/`, `docs/site/public/` (deliverables — no Maven module)
- **Skill:** `lg5-vitepress-docs (future, see ADR-006)`
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.3, §7.4, §7.8](design.md))
- **Acceptance:**
  - **Given** TASK-001 is `done` and `cd docs/site && pnpm install`
    resolves cleanly,
  - **When** the implementer creates (a) `docs/site/.vitepress/config.ts`
    **verbatim** per [`design.md` §7.3](design.md) (title, nav,
    sidebar, `base` from `process.env.DOCS_BASE`, `srcDir: '.'`,
    `ignoreDeadLinks: true`, `themeConfig.search.provider: 'local'`,
    and the `vite.define` map for `__COMMIT_SHA__` / `__BUILD_TIME__`
    / `__PR_NUMBER__`); (b) `docs/site/.vitepress/theme/index.ts` and
    `docs/site/.vitepress/theme/SourceStateFooter.vue` per
    [`design.md` §7.4](design.md) (layout-slot override registered on
    the default theme's `layout-bottom` slot, rendering
    `Built from <sha> · <iso-timestamp> · _(PR #<n>)_` on every
    page); (c) `docs/site/index.md` (home page identifying the
    loyalty-ledger service with one-click links to the six core
    entries); (d) the five section directories
    `docs/site/architecture/`, `docs/site/api/`, `docs/site/events/`,
    `docs/site/adr/`, `docs/site/runbook/`, each with a stub
    `index.md` containing the VitePress include directive
    `<!--@include: ./_placeholder.md-->` per
    [`design.md` §7.8](design.md); (e) the empty `docs/site/public/`
    directory (target for upstream-artifact downloads),
  - **Then** running `cd docs/site && DOCS_BASE='/' pnpm run docs:dev`
    starts a local server with the home page reachable at
    `http://localhost:5173/`, the navigation shows entries to
    Architecture, API (sync), Events (async), ADRs, Acceptance Report
    (external link to the Allure URL), and Runbook, the local search
    box accepts a query and returns hits from the seeded content, and
    the source-state footer renders on every rendered page (with
    empty PR id locally — that's expected).

## TASK-003 — Implement `docs/site/scripts/check-artifacts.mjs`

- **Status:** done (commit `a1bba7c`)
- **References:** REQ-019; ADR-001 (VitePress)
- **Depends on:** TASK-002
- **Modules touched:** `docs/site/scripts/check-artifacts.mjs` (deliverable)
- **Skill:** `lg5-vitepress-docs (future, see ADR-006)`
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.8](design.md))
- **Acceptance:**
  - **Given** TASK-002 is `done` and the five section `index.md`
    files include `<!--@include: ./_placeholder.md-->`,
  - **When** the implementer creates a Node ES-module script
    `docs/site/scripts/check-artifacts.mjs` whose working directory
    is `docs/site/` (it is invoked from there by the `docs:build`
    npm script) and that iterates the **five-row artifact table**
    verbatim from [`design.md` §7.8](design.md) — checking the four
    on-disk paths **relative to `docs/site/`**: `public/dependency-graph.png`,
    `public/gource.mp4`, `api/swagger-ui.html`, `events/asyncapi.html`,
    plus the optional Allure HEAD-probe row (no on-disk artifact) —
    for each missing artifact the script writes a
    `<section>/_placeholder.md` file containing the exact placeholder
    copy from the table and emits one `::warning file=<path>::<message>`
    line on stdout, then exits 0 (never blocks the build); the
    script is wired into the `docs:build` npm script (already declared
    in TASK-001's `package.json` as `node scripts/check-artifacts.mjs
    && vitepress build .`),
  - **Then** running `cd docs/site && DOCS_BASE='/' pnpm run docs:build`
    on a checkout where none of the four on-disk artifacts exist
    produces (a) four `_placeholder.md` files under `architecture/`,
    `api/`, `events/` with the design-specified copy, (b) four
    `::warning::` lines on stdout, (c) a successful build (exit
    code 0) emitting `docs/site/.vitepress/dist/` in which the four
    section pages render the placeholder copy.

## TASK-004 — Implement `docs/site/scripts/linkinator-to-annotations.mjs`

- **Status:** done (commit `0168f61`)
- **References:** REQ-018; ADR-001 (VitePress)
- **Depends on:** TASK-001
- **Modules touched:** `docs/site/scripts/linkinator-to-annotations.mjs` (deliverable)
- **Skill:** `lg5-vitepress-docs (future, see ADR-006)`
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.7](design.md))
- **Acceptance:**
  - **Given** TASK-001 is `done` and `linkinator` is installed via
    `docs/site/package.json` `devDependencies`,
  - **When** the implementer creates a ~20-LOC Node ES-module wrapper
    `docs/site/scripts/linkinator-to-annotations.mjs` whose working
    directory is `docs/site/` and that runs
    `pnpm exec linkinator .vitepress/dist --recurse --skip "<allow-list>" --silent`
    (with the allow-list pattern verbatim from [`design.md`
    §7.7](design.md):
    `^https?://(?!(lglabs-loyalty-docs|lglabs-loyalty-allure)\.web\.app|.+\.github\.io/lg5-loyalty-ledger)`
    — implementer **confirms latest stable major version of linkinator
    at scaffold time** per [`design.md` §11 Q1](design.md), and may
    refine the allow-list per [`design.md` §11 Q2](design.md) if
    operational data accumulates), parses the output, and emits one
    `::warning file=<page>,line=<n>::Broken link <url> -> <status>`
    line per broken link, then exits 0 unconditionally,
  - **Then** running the script (from `docs/site/`) against a
    `.vitepress/dist/` directory containing one deliberate broken
    intra-site link (e.g. `[x](./does-not-exist.html)`) produces
    exactly one `::warning::` line on stdout naming the offending
    page and target URL, and exits with code 0; running it against a
    clean `.vitepress/dist/` produces no warnings and exits 0.

## TASK-005 — Add `docs/site/firebase.json` and `docs/site/.firebaserc`

- **Status:** done (commit `cde1ce0`)
- **References:** REQ-001, REQ-005, REQ-008, REQ-013; ADR-002 (dual deploy); ADR-003 (separate Allure site); ADR-005 (preview channels)
- **Depends on:** —
- **Modules touched:** `docs/site/firebase.json`, `docs/site/.firebaserc` (deliverables)
- **Skill:** `lg5-vitepress-docs (future, see ADR-006)`
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.5](design.md))
- **Acceptance:**
  - **Given** Firebase project `lglabs-loyalty` exists with sites
    `lglabs-loyalty-docs` and `lglabs-loyalty-allure` already created
    (pre-condition documented in [`plan.md` §"Cross-cutting concerns →
    External dependencies"](plan.md)),
  - **When** the implementer creates `docs/site/firebase.json` and
    `docs/site/.firebaserc` **verbatim** per [`design.md`
    §7.5](design.md) — two `hosting` entries with `target: docs`
    (`public: ".vitepress/dist"`, `cleanUrls: true`,
    `trailingSlash: false`) and `target: allure`
    (`public: "allure-dist"`, `cleanUrls: false`,
    `trailingSlash: false`); `.firebaserc` mapping
    `default: lglabs-loyalty` and the two hosting targets `docs →
    lglabs-loyalty-docs`, `allure → lglabs-loyalty-allure`; both files
    interpreted relative to `docs/site/`,
  - **Then** running (from `docs/site/`)
    `pnpm exec firebase use lglabs-loyalty` succeeds,
    `pnpm exec firebase target:apply hosting docs lglabs-loyalty-docs`
    and `pnpm exec firebase target:apply hosting allure
    lglabs-loyalty-allure` are no-ops (already wired in `.firebaserc`),
    and `pnpm exec firebase deploy --only hosting:docs --dry-run`
    completes with exit code 0 (confirming the JSON parses and
    targets resolve relative to `docs/site/`).

## TASK-006 — Add docs Makefile targets (root `Makefile`, `cd docs/site` first)

- **Status:** done (commit `6880683`)
- **References:** REQ-001, REQ-005, REQ-009, REQ-010 (foundational for CI invocation); RULE-017 (Make-as-canonical-entrypoint)
- **Depends on:** TASK-001, TASK-005
- **Modules touched:** `Makefile` (deliverable, repo root)
- **Skill:** (none — Makefile additions only)
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.6](design.md))
- **Acceptance:**
  - **Given** TASK-001 is `done` (so `pnpm` resolves inside
    `docs/site/`) and TASK-005 is `done` (so `firebase` is configured
    in `docs/site/.firebaserc`),
  - **When** the implementer appends the **six docs targets** verbatim
    from [`design.md` §7.6](design.md) to the existing repo-root
    `Makefile` — every target prefixes its underlying invocation with
    `cd docs/site &&` so the developer types `make docs-*` from the
    repo root and the target handles the directory transition
    transparently: `docs-install` (`cd docs/site && pnpm install
    --frozen-lockfile`), `docs-build-pages` (`cd docs/site &&
    DOCS_BASE='/lg5-loyalty-ledger/' pnpm run docs:build`),
    `docs-build-firebase` (`cd docs/site && DOCS_BASE='/' pnpm run
    docs:build`), `docs-preview-local` (`cd docs/site && pnpm run
    docs:dev`), `docs-deploy-pages` (CI-only parity wrapper —
    documented as such), `docs-deploy-firebase` (`cd docs/site &&
    pnpm exec firebase deploy --only hosting:docs --project
    lglabs-loyalty`) — and adds them all to `.PHONY`,
  - **Then** `make -n docs-install docs-build-pages docs-build-firebase
    docs-preview-local docs-deploy-firebase` (run from repo root)
    prints (without executing) the exact `cd docs/site && …`
    commands above; `make docs-install` followed by
    `make docs-build-firebase` produces
    `docs/site/.vitepress/dist/index.html` on disk.

## TASK-007 — Add CI build jobs (`docs-build-pages`, `docs-build-firebase`)

- **Status:** done (commit `bbf8158`)
- **References:** REQ-001, REQ-008, REQ-009, REQ-010, REQ-011, REQ-016, REQ-018, REQ-019; RULE-017 (Make targets invoked from CI), RULE-018 (canonical patterns); ADR-004 (dual base-path build)
- **Depends on:** TASK-002, TASK-003, TASK-004, TASK-006
- **Modules touched:** `.github/workflows/c-integration.yml` (deliverable, repo root)
- **Skill:** `lg5-github-actions`
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.9 rows 1-2](design.md))
- **Acceptance:**
  - **Given** TASKs 002, 003, 004, and 006 are `done`,
  - **When** the implementer adds two new jobs to
    `.github/workflows/c-integration.yml` — `docs-build-pages` and
    `docs-build-firebase` — **both jobs declare
    `defaults: { run: { working-directory: docs/site } }`** so that
    every step's shell starts inside `docs/site/`. Both are triggered
    on `push:main` **and** `pull_request`, both perform
    `pnpm/action-setup@v3` → `make docs-install` (run from repo
    root, since the Makefile already `cd`s) → download CI artifacts
    from the feature 002 / feature 003 jobs (`swagger-ui.html`,
    `asyncapi.html`, `dependency-graph.png`, `gource.mp4`) into the
    paths expected by `scripts/check-artifacts.mjs` — namely
    `docs/site/public/dependency-graph.png`,
    `docs/site/public/gource.mp4`, `docs/site/api/swagger-ui.html`,
    `docs/site/events/asyncapi.html` (the exact relative paths from
    [`design.md` §7.8 table](design.md)), then `make docs-build-pages`
    (respectively `make docs-build-firebase`), then run the linkinator
    wrapper from TASK-004 against `docs/site/.vitepress/dist`, and
    finally `actions/upload-artifact@v4` with names `docs-dist-pages`
    and `docs-dist-firebase` (each archiving
    `docs/site/.vitepress/dist/**`) per
    [`design.md` §7.9 rows 1-2](design.md),
  - **Then** opening a throwaway PR triggers both jobs; both exit 0
    even when one of the upstream artifacts is absent (placeholder
    path); the GitHub Actions log shows `::warning::` annotations
    from `check-artifacts.mjs` and `linkinator-to-annotations.mjs`
    (if any dead links exist); the two artifacts `docs-dist-pages`
    and `docs-dist-firebase` appear under the workflow run's
    Artifacts panel and contain `index.html` at their root.

## TASK-008 — Add CI deploy jobs (live, main-only): Pages, Firebase docs, Firebase Allure (with copy step)

- **Status:** done (commit `3aecdf0`)
- **References:** REQ-001, REQ-005, REQ-008, REQ-009, REQ-010, REQ-011; RULE-017, RULE-018; ADR-002 (dual deploy); ADR-003 (separate Allure site)
- **Depends on:** TASK-005, TASK-007
- **Modules touched:** `.github/workflows/c-integration.yml` (deliverable, repo root)
- **Skill:** `lg5-github-actions`
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.9 rows 3-5 + the explicit `firebase-deploy-allure` step sequence](design.md))
- **Acceptance:**
  - **Given** TASK-007 is `done` (build jobs upload `docs-dist-pages`
    and `docs-dist-firebase`), TASK-005 is `done` (Firebase config
    valid in `docs/site/`), and the secret
    `FIREBASE_SERVICE_ACCOUNT_LGLABS_LOYALTY` is configured on the
    repo (per [`plan.md` Risk R1](plan.md)),
  - **When** the implementer adds **three** deploy jobs to
    `.github/workflows/c-integration.yml`, all gated by
    `if: github.event_name == 'push' && github.ref == 'refs/heads/main'`:
    (a) `pages-deploy` — `needs: docs-build-pages`, downloads
    artifact `docs-dist-pages`, has `permissions: { pages: write,
    id-token: write }`, uses `actions/deploy-pages@v4`; (b)
    `firebase-deploy-docs` — `needs: docs-build-firebase`,
    `defaults: { run: { working-directory: docs/site } }`, downloads
    artifact `docs-dist-firebase` to `docs/site/.vitepress/dist`,
    runs `pnpm exec firebase deploy --only hosting:docs --project
    lglabs-loyalty --non-interactive` with the Firebase
    service-account secret exported as `GOOGLE_APPLICATION_CREDENTIALS`;
    (c) `firebase-deploy-allure` — `needs:` the existing feature-003
    Allure-generation job, `defaults: { run: { working-directory:
    docs/site } }`, **with the explicit copy step from
    [`design.md` §7.9 step sequence](design.md)**: download the
    upstream `allure-report` artifact via
    `actions/download-artifact@v4` with `path: docs/site/allure-dist`
    (this is the **single source of truth** for how the Allure
    report reaches the Firebase site — `docs/site/allure-dist/` is
    gitignored and never exists in a checkout), then
    `pnpm install --frozen-lockfile`, then `pnpm exec firebase
    deploy --only hosting:allure --project lglabs-loyalty
    --non-interactive` (uploads the contents of
    `docs/site/allure-dist/` per `docs/site/firebase.json` target
    `allure`),
  - **Then** merging a no-op PR to `main` triggers all three deploy
    jobs in parallel; each completes with exit code 0 within the
    REQ-010 ten-minute window; opening
    `https://lglabs-pentagon.github.io/lg5-loyalty-ledger/`,
    `https://lglabs-loyalty-docs.web.app/`, and
    `https://lglabs-loyalty-allure.web.app/` in incognito serves the
    three live surfaces without any authentication step, and the
    Allure URL serves the latest acceptance-test report content.

## TASK-009 — Add CI preview job (`firebase-preview`, label-gated, with PR auto-comment)

- **Status:** todo
- **References:** REQ-012, REQ-013, REQ-014, REQ-015, REQ-020; RULE-017, RULE-018; ADR-002 (dual deploy); ADR-005 (label-gated preview channels)
- **Depends on:** TASK-005, TASK-007
- **Modules touched:** `.github/workflows/c-integration.yml` (deliverable, repo root)
- **Skill:** `lg5-github-actions`
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.9 row 6 + auto-comment step](design.md))
- **Acceptance:**
  - **Given** TASK-007 is `done`, TASK-005 is `done`, the GitHub
    label `docs/preview` exists on the repo, and the secret
    `FIREBASE_SERVICE_ACCOUNT_LGLABS_LOYALTY` is configured,
  - **When** the implementer adds the `firebase-preview` job per
    [`design.md` §7.9 row 6](design.md) with
    `defaults: { run: { working-directory: docs/site } }`, the `if:`
    guard `${{ github.event.pull_request != null && contains(github.event.pull_request.labels.*.name, 'docs/preview') && github.event.pull_request.head.repo.full_name == github.repository }}`
    (the trailing clause implements the **fork-skip** documented in
    [`plan.md` Risk R3](plan.md)), `needs: docs-build-firebase`,
    declares `permissions: { pull-requests: write }` (required for
    the auto-comment step), downloads `docs-dist-firebase` to
    `docs/site/.vitepress/dist`, sets
    `PR_NUMBER=${{ github.event.pull_request.number }}` (consumed by
    the source-state footer for REQ-020), runs `pnpm exec firebase
    hosting:channel:deploy pr-${PR_NUMBER} --only docs --expires 7d
    --project lglabs-loyalty --non-interactive` (the `--expires 7d`
    satisfies REQ-014), captures the channel URL from the CLI
    output, AND — per **stakeholder decision Q3 (a) (auto-comment)
    from [`design.md` §11](design.md), implemented per the
    "auto-comment step" in [`design.md` §7.9](design.md)** — adds a
    final step that posts the URL on the PR via
    `gh pr comment $PR_NUMBER --body "Docs preview: $URL (expires
    in 7 days)"`,
  - **Then** opening a throwaway PR **without** the `docs/preview`
    label causes the `firebase-preview` job to be **skipped** (no
    preview URL produced — REQ-015); applying the `docs/preview`
    label re-runs the workflow, the job runs to completion within
    ten minutes, the channel URL
    `https://lglabs-loyalty-docs--pr-<N>-<hash>.web.app/` is
    reachable in incognito (REQ-013), the source-state footer on
    every preview page displays the PR number (REQ-020), and a bot
    comment containing the preview URL appears on the PR
    (stakeholder decision Q3 (a)); a fork PR with the same label
    causes the job to be skipped via the fork-skip clause (Risk R3)
    and the GitHub Actions log records a clear notice.

## TASK-010 — Author the onboarding runbook stub at `docs/site/runbook/index.md`

- **Status:** todo
- **References:** REQ-007
- **Depends on:** TASK-002
- **Modules touched:** `docs/site/runbook/index.md` (deliverable)
- **Skill:** `lg5-vitepress-docs (future, see ADR-006)`
- **Command / Subagent:** (none — content authored by the stakeholder per [`design.md` §1](design.md))
- **Acceptance:**
  - **Given** TASK-002 is `done` (the `docs/site/runbook/` section
    exists with a stub `index.md` and `_placeholder.md` include),
  - **When** the stakeholder (`lglabs`, owner per [`prd.md` §8 row
    2](prd.md)) replaces `docs/site/runbook/index.md` with a
    minimum-viable runbook containing **three explicit sections**
    per REQ-007: (a) a first-day setup checklist (clone, install
    JDK 21, install pnpm, run `make install-skip-test`,
    run `make run-acceptance-test`, run `make docs-install &&
    make docs-preview-local` to render the site locally), (b) a
    brief tour of the repository (one paragraph per top-level
    directory: `docs/` (split: `docs/specs/` for SDD specs,
    `docs/site/` for the rendered docs surface), `loyalty-ledger-*`
    Maven modules per RULE-004, `Makefile`, `.github/`, `.agent-os/`),
    and (c) one-click cross-links to the other five core entries
    (Architecture, API, Events, ADRs, Acceptance Report),
  - **Then** running `make docs-build-firebase` and opening
    `docs/site/.vitepress/dist/runbook/index.html` shows the three
    named sections in order, with all five cross-links resolving
    (verified by clicking each in the local preview server).

## TASK-011 — Synthetic broken-link + missing-artifact validation in CI

- **Status:** todo
- **References:** REQ-018, REQ-019
- **Depends on:** TASK-007
- **Modules touched:** `docs/site/architecture/index.md` (transient broken link), `docs/site/public/dependency-graph.png` (transient deletion / skipped download) — all reverted at TASK end
- **Skill:** `lg5-vitepress-docs (future, see ADR-006)`
- **Command / Subagent:** (none — manual validation following [`design.md` §9 rows 8-9](design.md))
- **Acceptance:**
  - **Given** TASK-007 is `done` and the CI build jobs are running on
    `pull_request`,
  - **When** the implementer opens a throwaway commit on the feature
    branch that (a) introduces **one** deliberate broken intra-site
    link in `docs/site/architecture/index.md` (e.g.
    `[broken](./does-not-exist.md)`), AND (b) deletes **one** of the
    upstream-artifact download steps in the `docs-build-firebase`
    job (e.g. removes the step that downloads `dependency-graph.png`
    into `docs/site/public/`, so `scripts/check-artifacts.mjs` has
    to emit a placeholder for the architecture section), pushes the
    commit, observes the CI run, and then reverts both changes in a
    follow-up commit on the same branch,
  - **Then** the CI run for the broken commit shows: (a) the
    `docs-build-firebase` job exits with code 0 (REQ-018: no failure);
    (b) the GitHub Actions log contains a `::warning::` annotation
    from `linkinator-to-annotations.mjs` naming the broken link;
    (c) the same job's log contains a `::warning::` from
    `check-artifacts.mjs` naming the missing artifact
    (`docs/site/public/dependency-graph.png`); (d) the uploaded
    `docs-dist-firebase` artifact contains the architecture section
    rendering the placeholder copy from [`design.md` §7.8 table row
    1](design.md); (e) the revert commit's CI run is clean (no
    warnings).

## TASK-012 — Final verification: live surfaces + preview round-trip

- **Status:** todo
- **References:** REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, REQ-013, REQ-014, REQ-015, REQ-017, REQ-020 (and transitively all others via the live surface). **Note:** this TASK replaces the canonical "all ATDD scenarios green + zero `must` violations" final TASK because this feature has no JVM test runtime — see [`design.md` §9](design.md) and the preamble above.
- **Depends on:** TASK-008, TASK-009, TASK-010, TASK-011
- **Modules touched:** none (verification-only)
- **Skill:** `lg5-vitepress-docs (future, see ADR-006)`
- **Command / Subagent:** (none — manual visual verification)
- **Acceptance:**
  - **Given** TASKs 008, 009, 010, and 011 are `done` and the feature
    branch has been merged to `main`,
  - **When** the implementer (1) opens
    `https://lglabs-pentagon.github.io/lg5-loyalty-ledger/` in
    incognito; (2) opens `https://lglabs-loyalty-docs.web.app/` in
    incognito; (3) opens `https://lglabs-loyalty-allure.web.app/` in
    incognito; (4) opens a fresh throwaway PR with a one-character
    edit to `docs/site/runbook/index.md`, applies the `docs/preview`
    label, waits up to ten minutes, and notes the bot-commented
    preview URL,
  - **Then** all four checks pass observably: (a) both
    `lglabs-pentagon.github.io/lg5-loyalty-ledger/` and
    `lglabs-loyalty-docs.web.app/` serve the home page with all six
    nav entries reachable in one click (REQ-001…REQ-007), and the
    local search box returns ≥1 hit for the term "loyalty-ledger"
    (REQ-017); (b) `lglabs-loyalty-allure.web.app/` serves the latest
    acceptance report (REQ-005); (c) the source-state footer on
    **every** page of all three surfaces shows the short SHA of the
    latest `main` commit and an ISO-8601 timestamp within the last 10
    minutes (REQ-009, REQ-010, REQ-020); (d) the throwaway-PR preview
    URL is reachable in incognito (REQ-013), its footer additionally
    shows the PR number (REQ-020), and the bot comment on the PR
    contains the preview URL (stakeholder decision Q3 (a)); (e)
    closing/un-labeling the PR and waiting 7 days + 1 hour confirms
    the channel URL is no longer reachable (REQ-014) — this last
    sub-check may be deferred to a calendar follow-up note rather
    than blocking TASK closure.

## Definition of Done (Tasks)

- [x] Every TASK references ≥1 REQ-NNN.
- [x] Every TASK has Given/When/Then acceptance criteria.
- [x] Every TASK is ≤1 day of work / 1-3 commits.
- [x] Dependencies form a DAG (no cycles) — verified against the
      ASCII dep graph above and the machine-readable list.
- [x] First TASK is the smallest precondition (`docs/site/package.json`
      + pnpm scaffold + `.gitignore` updates).
- [ ] Last TASK is "all ATDD scenarios green + zero `must` violations"
      — **N/A for this feature**: no ATDD exists (per [`design.md`
      §9](design.md): no JVM test runtime, RULE-012/RULE-013 N/A).
      The last TASK (TASK-012) is the **visual-smoke + final
      verification** equivalent. This is the only intentionally
      unchecked DoD box.
- [x] Each TASK names the exact module(s)/deliverable(s), skill(s),
      and command(s)/subagent(s) it uses (with the documented
      reinterpretation in the preamble: deliverables instead of Maven
      modules, all paths `docs/site/`-prefixed; `lg5-vitepress-docs
      (future, see ADR-006)` / `lg5-github-actions` / `(none)` for
      skills; `(none — manual implementation)` for commands).
