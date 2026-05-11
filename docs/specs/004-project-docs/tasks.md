---
kind: spec
name: tasks
feature: 004-project-docs
version: 0.1.0
description: Atomic TASK list for the loyalty-ledger documentation surface (VitePress + dual deploy + label-gated previews + Allure cross-link). Decomposed from plan.md and design.md.
---

# Tasks — `004-project-docs`

> Generated from [`plan.md`](plan.md) and [`design.md`](design.md). Each
> task is atomic (≤1 day, 1-3 commits), references its source REQ-NNN,
> and has Given/When/Then acceptance criteria.
>
> `/sdd-implement <task-id>` consumes one task at a time and updates its
> `Status` field upon successful commit.

## Preamble — feature shape (read before consuming any TASK)

This feature is **docs-and-CI**, not a Maven service. Per [`plan.md`
§"Deviation from RULE-004"](plan.md) and [`design.md` §10](design.md),
it introduces **zero** Java/Spring/Kafka/JPA/Saga/Avro code. As a
consequence, throughout this TASK list:

- The **"Modules touched"** field names **deliverables** (e.g.
  `docs/`, `firebase.json`, `Makefile`, `.github/workflows/c-integration.yml`,
  `scripts/`) rather than Maven modules. This feature touches no
  Maven module.
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
TASK-001 (root manifest) ─┬─► TASK-002 (vitepress scaffold) ─┬─► TASK-003 (check-artifacts.mjs)
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
TASK-005 (firebase config) ─┴──────┘                                                  TASK-012 (final verification)
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

## TASK-001 — Add repo-root Node manifest and pnpm scaffold

- **Status:** todo
- **References:** REQ-001, REQ-016 (foundational precondition for all docs deliverables); ADR-001 (VitePress)
- **Depends on:** —
- **Modules touched:** `package.json`, `.gitignore` (deliverables — no Maven module)
- **Skill:** `lg5-vitepress-docs (future, see ADR-006)`
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.1, §7.2](design.md))
- **Acceptance:**
  - **Given** a clean checkout of `feature/004-project-docs` with no
    `package.json` at repo root and no `node_modules/`,
  - **When** the implementer adds `package.json` per [`design.md`
    §7.2](design.md) (with `packageManager: "pnpm@9.x"`, `type: "module"`,
    the three `docs:*` scripts, and the three `devDependencies`
    `vitepress`, `firebase-tools`, `linkinator` — confirming **latest
    stable major version at scaffold time** per [`design.md` §11
    Q1](design.md)), generates `pnpm-lock.yaml` via `pnpm install`,
    and appends `node_modules/`, `docs/.vitepress/dist/`,
    `docs/.vitepress/cache/`, and `firebase-debug.log` to `.gitignore`,
  - **Then** running `pnpm install --frozen-lockfile` from a fresh
    clone succeeds with exit code 0, and `git status` reports a clean
    working tree (no untracked `node_modules/` or `dist/`).

## TASK-002 — Scaffold the VitePress site under `docs/`

- **Status:** todo
- **References:** REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-016, REQ-017, REQ-018, REQ-020; ADR-001 (VitePress); ADR-004 (dual base-path build)
- **Depends on:** TASK-001
- **Modules touched:** `docs/` (deliverable — no Maven module)
- **Skill:** `lg5-vitepress-docs (future, see ADR-006)`
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.3, §7.4, §7.8](design.md))
- **Acceptance:**
  - **Given** TASK-001 is `done` and `pnpm install` resolves cleanly,
  - **When** the implementer creates (a) `docs/.vitepress/config.ts`
    **verbatim** per [`design.md` §7.3](design.md) (title, nav, sidebar,
    `base` from `process.env.DOCS_BASE`, `ignoreDeadLinks: true`,
    `themeConfig.search.provider: 'local'`, and the `vite.define` map
    for `__COMMIT_SHA__` / `__BUILD_TIME__` / `__PR_NUMBER__`); (b)
    `docs/.vitepress/theme/index.ts` and `docs/.vitepress/theme/SourceStateFooter.vue`
    per [`design.md` §7.4](design.md) (layout-slot override rendering
    `Built from <sha> · <iso-timestamp> · _(PR #<n>)_` on every page);
    (c) `docs/index.md` (home page identifying the loyalty-ledger
    service with one-click links to the six core entries); (d) the
    five section directories `docs/architecture/`, `docs/api/`,
    `docs/events/`, `docs/adr/`, `docs/runbook/`, each with a stub
    `index.md` that contains the VitePress include directive
    `<!--@include: ./_placeholder.md-->` per [`design.md`
    §7.8](design.md),
  - **Then** running `DOCS_BASE='/' pnpm run docs:dev` starts a local
    server with the home page reachable at `http://localhost:5173/`,
    the navigation shows entries to Architecture, API, Events, ADRs,
    Acceptance Report (external link to the Allure URL), and Runbook,
    the local search box accepts a query and returns hits from the
    seeded content, and the source-state footer renders on every
    rendered page (with empty PR id locally — that's expected).

## TASK-003 — Implement `scripts/check-artifacts.mjs`

- **Status:** todo
- **References:** REQ-019; ADR-001 (VitePress)
- **Depends on:** TASK-002
- **Modules touched:** `scripts/check-artifacts.mjs` (deliverable)
- **Skill:** `lg5-vitepress-docs (future, see ADR-006)`
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.8](design.md))
- **Acceptance:**
  - **Given** TASK-002 is `done` and the five section `index.md`
    files include `<!--@include: ./_placeholder.md-->`,
  - **When** the implementer creates a Node ES-module script
    `scripts/check-artifacts.mjs` that iterates the **five-row
    artifact table** verbatim from [`design.md` §7.8](design.md)
    (`docs/public/dependency-graph.png`, `docs/public/gource.mp4`,
    `docs/api/swagger-ui.html`, `docs/events/asyncapi.html`, plus the
    Allure URL HEAD-probe row which is a no-op on disk) — for each
    missing artifact the script writes a `<section>/_placeholder.md`
    file containing the exact placeholder copy from the table and
    emits one `::warning file=<path>::<message>` line on stdout — and
    wires the script into the `docs:build` npm script (already
    declared in TASK-001's `package.json` as
    `node scripts/check-artifacts.mjs && vitepress build docs`),
  - **Then** running `make docs-build-firebase` (will exist after
    TASK-006; for this TASK, run `DOCS_BASE='/' pnpm run docs:build`
    directly) on a checkout where none of the four on-disk artifacts
    exist produces (a) four `_placeholder.md` files with the design-
    specified copy, (b) four `::warning::` lines on stdout, (c) a
    successful build (exit code 0) emitting `docs/.vitepress/dist/`
    in which the four section pages render the placeholder copy.

## TASK-004 — Implement `scripts/linkinator-to-annotations.mjs`

- **Status:** todo
- **References:** REQ-018; ADR-001 (VitePress)
- **Depends on:** TASK-001
- **Modules touched:** `scripts/linkinator-to-annotations.mjs` (deliverable)
- **Skill:** `lg5-vitepress-docs (future, see ADR-006)`
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.7](design.md))
- **Acceptance:**
  - **Given** TASK-001 is `done` and `linkinator` is installed via
    `devDependencies`,
  - **When** the implementer creates a ~20-LOC Node ES-module wrapper
    `scripts/linkinator-to-annotations.mjs` that runs `pnpm exec
    linkinator docs/.vitepress/dist --recurse --skip "<allow-list>"
    --silent` (with the allow-list pattern verbatim from [`design.md`
    §7.7](design.md): `^https?://(?!(lglabs-loyalty-docs|lglabs-loyalty-allure)\.web\.app|.+\.github\.io/lg5-loyalty-ledger)`
    — implementer **confirms latest stable major version of linkinator
    at scaffold time** per [`design.md` §11 Q1](design.md), and may
    refine the allow-list per [`design.md` §11 Q2](design.md) if
    operational data accumulates), parses the output, and emits one
    `::warning file=<page>,line=<n>::Broken link <url> -> <status>`
    line per broken link, then exits 0 unconditionally,
  - **Then** running the script against a `dist/` directory containing
    one deliberate broken intra-site link (e.g. `[x](./does-not-exist.html)`)
    produces exactly one `::warning::` line on stdout naming the
    offending page and target URL, and exits with code 0; running it
    against a clean `dist/` produces no warnings and exits 0.

## TASK-005 — Add `firebase.json` and `.firebaserc` at repo root

- **Status:** todo
- **References:** REQ-001, REQ-005, REQ-008, REQ-013; ADR-002 (dual deploy); ADR-003 (separate Allure site); ADR-005 (preview channels)
- **Depends on:** —
- **Modules touched:** `firebase.json`, `.firebaserc` (deliverables)
- **Skill:** `lg5-vitepress-docs (future, see ADR-006)`
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.5](design.md))
- **Acceptance:**
  - **Given** Firebase project `lglabs-loyalty` exists with sites
    `lglabs-loyalty-docs` and `lglabs-loyalty-allure` already created
    (pre-condition documented in [`plan.md` §"Cross-cutting concerns →
    External dependencies"](plan.md)),
  - **When** the implementer creates `firebase.json` and `.firebaserc`
    at repo root **verbatim** per [`design.md` §7.5](design.md) — two
    `hosting` entries with `target: docs` (`public:
    docs/.vitepress/dist`, `cleanUrls: true`) and `target: allure`
    (`public: allure-report`, `cleanUrls: false`); `.firebaserc` mapping
    `default: lglabs-loyalty` and the two hosting targets,
  - **Then** running `pnpm exec firebase use lglabs-loyalty` succeeds,
    `pnpm exec firebase target:apply hosting docs lglabs-loyalty-docs`
    and `pnpm exec firebase target:apply hosting allure
    lglabs-loyalty-allure` are no-ops (already wired in `.firebaserc`),
    and `pnpm exec firebase deploy --only hosting:docs --dry-run`
    completes with exit code 0 (confirming the JSON parses and
    targets resolve).

## TASK-006 — Add docs Makefile targets

- **Status:** todo
- **References:** REQ-001, REQ-005, REQ-009, REQ-010 (foundational for CI invocation); RULE-017 (Make-as-canonical-entrypoint)
- **Depends on:** TASK-001, TASK-005
- **Modules touched:** `Makefile` (deliverable)
- **Skill:** (none — Makefile additions only)
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.6](design.md))
- **Acceptance:**
  - **Given** TASK-001 is `done` (so `pnpm` resolves) and TASK-005 is
    `done` (so `firebase` is configured),
  - **When** the implementer appends the **six docs targets** verbatim
    from [`design.md` §7.6](design.md) to the existing `Makefile`:
    `docs-install` (`pnpm install --frozen-lockfile`), `docs-build-pages`
    (`DOCS_BASE='/lg5-loyalty-ledger/' pnpm run docs:build`),
    `docs-build-firebase` (`DOCS_BASE='/' pnpm run docs:build`),
    `docs-preview-local` (`pnpm run docs:dev`), `docs-deploy-pages`
    (CI-only parity wrapper — local target documents that production
    publication is CI-only), `docs-deploy-firebase` (`pnpm exec
    firebase deploy --only hosting:docs --project lglabs-loyalty`) —
    and adds them to `.PHONY`,
  - **Then** `make -n docs-install docs-build-pages docs-build-firebase
    docs-preview-local docs-deploy-firebase` prints (without executing)
    the exact underlying commands above; `make docs-install`
    followed by `make docs-build-firebase` produces
    `docs/.vitepress/dist/index.html` on disk.

## TASK-007 — Add CI build jobs (`docs-build-pages`, `docs-build-firebase`)

- **Status:** todo
- **References:** REQ-001, REQ-008, REQ-009, REQ-010, REQ-011, REQ-016, REQ-018, REQ-019; RULE-017 (Make targets invoked from CI), RULE-018 (canonical patterns); ADR-004 (dual base-path build)
- **Depends on:** TASK-002, TASK-003, TASK-004, TASK-006
- **Modules touched:** `.github/workflows/c-integration.yml` (deliverable)
- **Skill:** `lg5-github-actions`
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.9](design.md))
- **Acceptance:**
  - **Given** TASKs 002, 003, 004, and 006 are `done`,
  - **When** the implementer adds two new jobs to
    `.github/workflows/c-integration.yml` — `docs-build-pages` and
    `docs-build-firebase` — both triggered on `push:main` **and**
    `pull_request`, both performing `pnpm/action-setup@v3` →
    `make docs-install` → download CI artifacts from the feature 002 /
    feature 003 jobs (`swagger-ui.html`, `asyncapi.html`,
    `dependency-graph.png`, `gource.mp4`) into the paths expected by
    `scripts/check-artifacts.mjs`, then `make docs-build-pages`
    (respectively `make docs-build-firebase`), then run the linkinator
    wrapper from TASK-004 against `docs/.vitepress/dist`, and finally
    `actions/upload-artifact@v4` with names `docs-dist-pages` and
    `docs-dist-firebase` per [`design.md` §7.9 rows 1-2](design.md),
  - **Then** opening a throwaway PR triggers both jobs; both exit 0
    even when one of the upstream artifacts is absent (placeholder
    path); the GitHub Actions log shows `::warning::` annotations from
    `check-artifacts.mjs` and `linkinator-to-annotations.mjs` (if any
    dead links exist); the two artifacts `docs-dist-pages` and
    `docs-dist-firebase` appear under the workflow run's Artifacts
    panel.

## TASK-008 — Add CI deploy jobs (live, main-only): Pages, Firebase docs, Firebase Allure

- **Status:** todo
- **References:** REQ-001, REQ-005, REQ-008, REQ-009, REQ-010, REQ-011; RULE-017, RULE-018; ADR-002 (dual deploy); ADR-003 (separate Allure site)
- **Depends on:** TASK-005, TASK-007
- **Modules touched:** `.github/workflows/c-integration.yml` (deliverable)
- **Skill:** `lg5-github-actions`
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.9 rows 3-5](design.md))
- **Acceptance:**
  - **Given** TASK-007 is `done` (build jobs upload `docs-dist-pages`
    and `docs-dist-firebase`), TASK-005 is `done` (Firebase config
    valid), and the secret `FIREBASE_SERVICE_ACCOUNT_LGLABS_LOYALTY`
    is configured on the repo (per [`plan.md` Risk R1](plan.md)),
  - **When** the implementer adds **three** deploy jobs to
    `.github/workflows/c-integration.yml`, all gated by
    `if: github.event_name == 'push' && github.ref == 'refs/heads/main'`:
    (a) `pages-deploy` — `needs: docs-build-pages`, downloads
    artifact `docs-dist-pages`, has `permissions: { pages: write,
    id-token: write }`, uses `actions/deploy-pages@v4`; (b)
    `firebase-deploy-docs` — `needs: docs-build-firebase`, downloads
    `docs-dist-firebase` to `docs/.vitepress/dist`, runs `pnpm exec
    firebase deploy --only hosting:docs --project lglabs-loyalty
    --non-interactive` with the Firebase service-account secret
    exported as `GOOGLE_APPLICATION_CREDENTIALS`; (c)
    `firebase-deploy-allure` — `needs:` the existing feature-003
    Allure-generation job, downloads the `allure-report` artifact,
    runs `pnpm exec firebase deploy --only hosting:allure --project
    lglabs-loyalty --non-interactive`,
  - **Then** merging a no-op PR to `main` triggers all three deploy
    jobs in parallel; each completes with exit code 0 within the
    REQ-010 ten-minute window; opening
    `https://lglabs-pentagon.github.io/lg5-loyalty-ledger/`,
    `https://lglabs-loyalty-docs.web.app/`, and
    `https://lglabs-loyalty-allure.web.app/` in incognito serves the
    three live surfaces without any authentication step.

## TASK-009 — Add CI preview job (`firebase-preview`, label-gated, with PR auto-comment)

- **Status:** todo
- **References:** REQ-012, REQ-013, REQ-014, REQ-015, REQ-020; RULE-017, RULE-018; ADR-002 (dual deploy); ADR-005 (label-gated preview channels)
- **Depends on:** TASK-005, TASK-007
- **Modules touched:** `.github/workflows/c-integration.yml` (deliverable)
- **Skill:** `lg5-github-actions`
- **Command / Subagent:** (none — manual implementation following [`design.md` §7.9 row 6](design.md))
- **Acceptance:**
  - **Given** TASK-007 is `done`, TASK-005 is `done`, the GitHub
    label `docs/preview` exists on the repo, and the secret
    `FIREBASE_SERVICE_ACCOUNT_LGLABS_LOYALTY` is configured,
  - **When** the implementer adds the `firebase-preview` job per
    [`design.md` §7.9 row 6](design.md) with the `if:` guard
    `${{ github.event.pull_request != null && contains(github.event.pull_request.labels.*.name, 'docs/preview') && github.event.pull_request.head.repo.full_name == github.repository }}`
    (the trailing clause implements the **fork-skip** documented in
    [`plan.md` Risk R3](plan.md)), `needs: docs-build-firebase`,
    downloads `docs-dist-firebase` to `docs/.vitepress/dist`, sets
    `PR_NUMBER=${{ github.event.pull_request.number }}` (consumed by
    the source-state footer for REQ-020), runs `pnpm exec firebase
    hosting:channel:deploy pr-${PR_NUMBER} --only docs --expires 7d
    --project lglabs-loyalty --non-interactive` (the `--expires 7d`
    satisfies REQ-014), captures the channel URL from the CLI output,
    AND — per **stakeholder decision Q3 (a) (auto-comment) from
    [`design.md` §11](design.md)** — adds a final step with
    `permissions: { pull-requests: write }` that posts the URL on
    the PR via `gh pr comment $PR_NUMBER --body "Docs preview: $URL
    (expires in 7 days)"`,
  - **Then** opening a throwaway PR **without** the `docs/preview`
    label causes the `firebase-preview` job to be **skipped** (no
    preview URL produced — REQ-015); applying the `docs/preview`
    label re-runs the workflow, the job runs to completion within
    ten minutes, the channel URL `https://lglabs-loyalty-docs--pr-<N>-<hash>.web.app/`
    is reachable in incognito (REQ-013), the source-state footer on
    every preview page displays the PR number (REQ-020), and a bot
    comment containing the preview URL appears on the PR (stakeholder
    decision Q3 (a)); a fork PR with the same label causes the job
    to be skipped via the fork-skip clause (Risk R3) and the GitHub
    Actions log records a clear notice.

## TASK-010 — Author the onboarding runbook stub

- **Status:** todo
- **References:** REQ-007
- **Depends on:** TASK-002
- **Modules touched:** `docs/runbook/index.md` (deliverable)
- **Skill:** `lg5-vitepress-docs (future, see ADR-006)`
- **Command / Subagent:** (none — content authored by the stakeholder per [`design.md` §1](design.md))
- **Acceptance:**
  - **Given** TASK-002 is `done` (the `docs/runbook/` section exists
    with a stub `index.md` and `_placeholder.md` include),
  - **When** the stakeholder (`lglabs`, owner per [`prd.md` §8 row
    2](prd.md)) replaces `docs/runbook/index.md` with a
    minimum-viable runbook containing **three explicit sections**
    per REQ-007: (a) a first-day setup checklist (clone, install
    JDK 21, install pnpm, run `make install-skip-test`,
    run `make run-acceptance-test`), (b) a brief tour of the
    repository (one paragraph per top-level directory: `docs/`,
    `loyalty-ledger-*` Maven modules per RULE-004, `Makefile`,
    `.github/`, `.agent-os/`), and (c) one-click cross-links to the
    other five core entries (Architecture, API, Events, ADRs,
    Acceptance Report),
  - **Then** running `make docs-build-firebase` and opening
    `docs/.vitepress/dist/runbook/index.html` shows the three named
    sections in order, with all five cross-links resolving (verified
    by clicking each in the local preview server).

## TASK-011 — Synthetic broken-link + missing-artifact validation in CI

- **Status:** todo
- **References:** REQ-018, REQ-019
- **Depends on:** TASK-007
- **Modules touched:** `docs/` (transient broken link), upstream-artifact path (transient deletion) — all reverted at TASK end
- **Skill:** `lg5-vitepress-docs (future, see ADR-006)`
- **Command / Subagent:** (none — manual validation following [`design.md` §9 rows 8-9](design.md))
- **Acceptance:**
  - **Given** TASK-007 is `done` and the CI build jobs are running on
    `pull_request`,
  - **When** the implementer opens a throwaway commit on the feature
    branch that (a) introduces **one** deliberate broken intra-site
    link in `docs/architecture/index.md` (e.g. `[broken](./does-not-exist.md)`),
    AND (b) deletes **one** of the upstream artifacts that the build
    expects on disk (e.g. removes the
    `docs-build-firebase`-job's download step for `dependency-graph.png`,
    so `scripts/check-artifacts.mjs` has to emit a placeholder),
    pushes the commit, observes the CI run, and then reverts both
    changes in a follow-up commit on the same branch,
  - **Then** the CI run for the broken commit shows: (a) the
    `docs-build-firebase` job exits with code 0 (REQ-018: no failure);
    (b) the GitHub Actions log contains a `::warning::` annotation
    from `linkinator-to-annotations.mjs` naming the broken link;
    (c) the same job's log contains a `::warning::` from
    `check-artifacts.mjs` naming the missing artifact; (d) the
    uploaded `docs-dist-firebase` artifact contains the architecture
    section rendering the placeholder copy from [`design.md` §7.8
    table row 1](design.md); (e) the revert commit's CI run is clean
    (no warnings).

## TASK-012 — Final verification: live surfaces + preview round-trip

- **Status:** todo
- **References:** REQ-001, REQ-005, REQ-008, REQ-009, REQ-010, REQ-012, REQ-013, REQ-014, REQ-015, REQ-020 (and transitively all others via the live surface). **Note:** this TASK replaces the canonical "all ATDD scenarios green + zero `must` violations" final TASK because this feature has no JVM test runtime — see [`design.md` §9](design.md) and the preamble above.
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
    edit to `docs/runbook/index.md`, applies the `docs/preview`
    label, waits up to ten minutes, and notes the bot-commented
    preview URL,
  - **Then** all four checks pass observably: (a) both
    `lglabs-pentagon.github.io/lg5-loyalty-ledger/` and
    `lglabs-loyalty-docs.web.app/` serve the home page with all six
    nav entries reachable in one click (REQ-001…REQ-007); (b)
    `lglabs-loyalty-allure.web.app/` serves the latest acceptance
    report (REQ-005); (c) the source-state footer on **every** page
    of all three surfaces shows the short SHA of the latest `main`
    commit and an ISO-8601 timestamp within the last 10 minutes
    (REQ-009, REQ-010, REQ-020); (d) the throwaway-PR preview URL
    is reachable in incognito (REQ-013), its footer additionally
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
- [x] First TASK is the smallest precondition (`package.json` +
      pnpm scaffold).
- [ ] Last TASK is "all ATDD scenarios green + zero `must` violations"
      — **N/A for this feature**: no ATDD exists (per [`design.md`
      §9](design.md): no JVM test runtime, RULE-012/RULE-013 N/A).
      The last TASK (TASK-012) is the **visual-smoke + final
      verification** equivalent. This is the only intentionally
      unchecked DoD box.
- [x] Each TASK names the exact module(s)/deliverable(s), skill(s),
      and command(s)/subagent(s) it uses (with the documented
      reinterpretation in the preamble: deliverables instead of Maven
      modules; `lg5-vitepress-docs (future, see ADR-006)` /
      `lg5-github-actions` / `(none)` for skills; `(none — manual
      implementation)` for commands).
