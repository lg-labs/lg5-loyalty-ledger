---
kind: spec
name: plan
feature: 004-project-docs
version: 0.2.0
description: Technical plan for the loyalty-ledger documentation surface, including both docs-site infrastructure and mature technical documentation content.
---

# Plan — `004-project-docs`

> Generated from [`prd.md`](prd.md) and the ADRs under [`adr/`](adr/).
> The plan describes the **how** at the architectural and editorial level.
> This refreshed version extends feature 004 beyond the documentation-surface
> infrastructure into a complete technical documentation surface for
> `loyalty-ledger`, including architecture explanations, onboarding guidance,
> QuickStart material, FAQ content, and service-specific technical views for
> REST, events, DDD, and C4+1.

## Deviation from RULE-004

RULE-004 prescribes the canonical 8-module Maven shape for lg5-spring
**services** (`-domain-core`, `-application-service`, `-api`,
`-data-access`, `-message-core`, `-message-model`, `-external`,
`-container`, plus `-acceptance-test` and `-support`). **Feature 004 is
a documentation-and-CI feature, not a Maven module.** It introduces no
Java code, no Spring beans, no domain logic, no Avro schemas, no JPA
entities. RULE-004's modules continue to exist unchanged for the
service itself; this feature adds a sibling top-level directory `docs/`
whose contents are Markdown + Node tooling.

This refresh does not change that reasoning. It extends the **content**
published on the docs surface, not the runtime behavior or module shape of
the service.

## Architecture overview

This feature still does not produce Maven modules. The deliverables are:

```
<repo-root>/
├── docs/
│   ├── specs/                                  # existing — SDD specs
│   └── site/                                   # docs surface + content pages
│       ├── .vitepress/
│       ├── index.md                            # technical home page
│       ├── quickstart/                         # contributor bootstrap
│       ├── architecture/                       # overview + C4+1 + DDD + REST + Events
│       ├── api/                                # OpenAPI landing + interpretation
│       ├── events/                             # AsyncAPI landing + interpretation
│       ├── adr/                                # hybrid ADR landing page
│       ├── faq/                                # recurring contributor/reviewer questions
│       ├── runbook/                            # onboarding / operational orientation
│       ├── public/                             # static assets
│       ├── scripts/                            # check-artifacts.mjs + linkinator wrapper
│       ├── firebase.json
│       ├── .firebaserc
│       ├── package.json
│       └── pnpm-lock.yaml
├── Makefile
└── .github/workflows/c-integration.yml
```

**Layout decision (2026-05-11)** remains unchanged: all Node tooling lives
inside `docs/site/`. The repo root stays Maven-pure.

The existing Maven modules (`loyalty-ledger-domain-*`,
`loyalty-ledger-api`, `loyalty-ledger-data-access`,
`loyalty-ledger-message-*`, `loyalty-ledger-container`,
`loyalty-ledger-acceptance-test`, `loyalty-ledger-support`) are still
**not touched** by this feature.

## Documentation maturity extension

Feature `004-project-docs` originally established the documentation surface
itself: VitePress, navigation, CI build/deploy jobs, preview channels,
placeholder handling, search, and source-state footer behavior.

This extension keeps that infrastructure intact and expands the feature scope
to cover the maturity of the documentation content published on that surface.

The refreshed feature now includes:

- a complete technical architecture overview for `loyalty-ledger`
- a contributor-oriented `QuickStart` page in English
- an English `FAQ` for contributors and reviewers
- service-specific documentation for REST, events, DDD, and C4+1 views
- replacement of placeholder-first sections with useful entry-point content
- a hybrid ADR landing page with curated summaries plus direct links to the
  original ADR documents

This is still a documentation-and-CI feature. It introduces no production
Java logic, no Spring beans, no JPA model, no Avro schema behavior changes,
 and no service-runtime behavior changes.

## Deliverable ↔ requirement matrix

Every PRD requirement is covered by one or more deliverables.

| Deliverable | Covers REQ |
|---|---|
| `docs/site/index.md` (technical home page with audience-oriented entry points) | REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-016 |
| `docs/site/quickstart/index.md` | REQ-022, REQ-028 |
| `docs/site/architecture/index.md` | REQ-021, REQ-024, REQ-026, REQ-027, REQ-028 |
| `docs/site/architecture/c4-model.md` | REQ-025, REQ-027, REQ-028 |
| `docs/site/architecture/ddd.md` | REQ-024, REQ-027, REQ-028 |
| `docs/site/architecture/rest.md` | REQ-024, REQ-027, REQ-028 |
| `docs/site/architecture/events.md` | REQ-024, REQ-027, REQ-028 |
| `docs/site/api/index.md` (viewer landing + explanatory context) | REQ-003, REQ-024, REQ-026, REQ-028 |
| `docs/site/events/index.md` (viewer landing + explanatory context) | REQ-004, REQ-024, REQ-026, REQ-028 |
| `docs/site/adr/index.md` (hybrid curated index + direct links) | REQ-006, REQ-026, REQ-028 |
| `docs/site/faq/index.md` | REQ-023, REQ-027, REQ-028 |
| `docs/site/runbook/index.md` (refreshed onboarding / operational orientation) | REQ-007, REQ-028 |
| `docs/site/.vitepress/config.ts` and navigation/sidebar wiring | REQ-001, REQ-002, REQ-003, REQ-004, REQ-006, REQ-007, REQ-016, REQ-017 |
| `docs/site/.vitepress/theme/SourceStateFooter.vue` | REQ-020 |
| `docs/site/scripts/check-artifacts.mjs` + placeholder policy | REQ-019 |
| post-build link-check step | REQ-018 |
| `c-integration.yml :: docs-build-pages` job | REQ-001, REQ-008, REQ-009, REQ-010, REQ-011, REQ-016 |
| `c-integration.yml :: docs-build-firebase` job | REQ-001, REQ-008, REQ-009, REQ-010, REQ-011 |
| `c-integration.yml :: pages-deploy` job | REQ-001, REQ-008, REQ-009, REQ-010, REQ-011 |
| `c-integration.yml :: firebase-deploy-docs` job | REQ-001, REQ-008, REQ-009, REQ-010, REQ-011 |
| `c-integration.yml :: firebase-deploy-allure` job | REQ-005, REQ-008 |
| `c-integration.yml :: firebase-preview` job | REQ-012, REQ-013, REQ-014, REQ-015, REQ-020 |

Every REQ-001…REQ-028 appears in the table.

## Documentation source-of-truth policy

The documentation surface must be grounded in repository-local facts.

Source priority for `loyalty-ledger` documentation is:

1. Repository-local sources of truth
2. Ecosystem conceptual baseline
3. Supporting ecosystem context only

Repository-local sources of truth:

- `docs/specs/001-loyalty-ledger/*`
- `docs/api/openapi.yaml`
- `docs/api/asyncapi.yaml`
- ADRs under `docs/specs/**/adr/*.md`
- the actual repository structure and production code

Ecosystem conceptual baseline:

- `lg5-spring` documentation

Supporting ecosystem context only:

- `blank-service`, used only to enrich understanding of `lg5-spring`
  concepts and ecosystem terminology; never as a normative definition of
  `loyalty-ledger` structure, behavior, or architecture

If a repository-local source conflicts with any external reference, the
repository-local source wins.

## Editorial model

The documentation surface is organized by reader intent rather than by raw file
location alone.

Page classes:

- **Overview** — high-level entry points and orientation
- **How-to** — contributor steps and guided workflows
- **Reference** — contract- and model-oriented factual material
- **FAQ** — short answers to recurring questions with links to deeper pages

Planned mapping:

| Page | Class | Primary reader question |
|---|---|---|
| `index.md` | Overview | Where do I start? |
| `quickstart/index.md` | How-to | How do I get productive quickly? |
| `architecture/index.md` | Overview | What is this service and how does it work at a high level? |
| `architecture/c4-model.md` | Reference / explanation | What are the system, container, and main dynamic views? |
| `architecture/ddd.md` | Explanation / reference | How are domain and application boundaries modeled here? |
| `architecture/rest.md` | Reference / explanation | How should I understand the HTTP surface? |
| `architecture/events.md` | Reference / explanation | How should I understand inbound and outbound events? |
| `api/index.md` | Reference | Where is the OpenAPI contract and how do I read it? |
| `events/index.md` | Reference | Where is the AsyncAPI contract and how do I read it? |
| `adr/index.md` | Overview / reference | What decisions exist and where are they recorded? |
| `faq/index.md` | FAQ | What are the most common contributor and reviewer questions? |
| `runbook/index.md` | How-to / orientation | What should I do on day one and where do I go next? |

## ADR index decision

The ADR landing page is intentionally hybrid rather than fully automatic.

It will contain:

- a short explanation of what ADRs mean in this repository
- grouped navigation by feature or topic
- curated highlights for the most important decisions
- a compact table with ADR id, feature, title, status, one-line summary, and
  direct link to the original ADR markdown file

Rationale:

- a fully automatic index is cheap but low-context
- a fully manual page is richer but drifts more easily
- the hybrid shape preserves navigation quality without introducing complex
  automation

## ADR index

- [ADR-001](adr/ADR-001-vitepress-site-engine.md) — Use VitePress as the documentation site engine.
- [ADR-002](adr/ADR-002-dual-deploy-pages-and-firebase.md) — Dual-deploy to GitHub Pages and Firebase Hosting.
- [ADR-003](adr/ADR-003-allure-separate-firebase-site.md) — Allure on a separate Firebase site, cross-linked (not embedded).
- [ADR-004](adr/ADR-004-dual-base-path-build.md) — Build VitePress twice with different `base` paths.
- [ADR-005](adr/ADR-005-preview-channels-label-gated.md) — Per-PR Firebase preview channels, label-gated, 7-day TTL.
- [ADR-006](adr/ADR-006-upstream-vitepress-skill.md) — Upstream the pattern as bundle skill `lg5-vitepress-docs` in v4.0.0.

## Diagram policy

Diagrams are allowed and encouraged where they improve understanding of the
real service.

Mandatory diagram targets for this extension:

- one system-context view
- one container-or-module-level view
- one write-path dynamic flow
- one read-path dynamic flow

Diagram rules:

- diagrams must be specific to `loyalty-ledger`
- diagrams must remain readable in plain markdown source
- diagrams must simplify understanding rather than mirror the code line-by-line
- generic framework-only diagrams are not sufficient

## Sequenced steps

The original feature-004 infrastructure work remains complete. The extension
adds a second wave of content-maturity work.

High-level dependency sketch:

`prd-refresh -> plan-refresh -> design-refresh -> tasks-refresh -> architecture-content -> quickstart-faq-adr-content -> api-events-runbook-refresh -> docs-validation -> verify-refresh`

Expected content implementation order:

1. architecture overview
2. C4+1 views
3. DDD / REST / events pages
4. QuickStart
5. ADR landing page
6. FAQ
7. API / events landing-page enrichment
8. runbook + home-page refresh
9. navigation / search / coherence validation

## Cross-cutting concerns

- **Topics / channels:** N/A — no Kafka traffic in this feature.
  Asynchronous service contract docs reference the Schema Registry topic
  information already shipped by feature 002.
- **Schema registry:** N/A directly — RULE-007 is not modified.
  `docs/site/events/` may interpret the asynchronous contract, but the
  service contract itself remains the source of truth. **Owner**: docs author.
- **Observability:** the source-state indicator (REQ-020) on every
  surface is the in-band observability for "did the regeneration
  succeed?" Broken-link warnings (REQ-018) are emitted to the CI job
  log. No external dashboards are added. **Owner**: stakeholder.
- **Security:** Firebase service account stored as a GitHub Actions
  secret (`FIREBASE_SERVICE_ACCOUNT_LGLABS_LOYALTY`). Pages uses the
  default `GITHUB_TOKEN` with `pages: write, id-token: write`
  permissions on the deploy job only. No new secrets beyond Firebase.
  **Owner**: stakeholder (configures secret); CI maintainer
  (workflow YAML).
- **Data lifecycle:** the surface is overwrite-on-advance (REQ-011);
  no archival. Preview channels auto-expire at 7 days (REQ-014).
  **Owner**: Firebase Hosting.
- **External dependencies:** Firebase project and sites remain required
  pre-conditions. `lg5-spring` and `blank-service` may inform ecosystem
  understanding only; they are not authoritative sources for `loyalty-ledger`.
- **Build-tool baseline:** Node + pnpm remain additive to the JVM stack;
  the service runtime baseline is unchanged.

## Risks

| ID | Risk | Mitigation | Owner |
|----|------|------------|-------|
| R1 | Firebase service-account secret missing or rotated on the GitHub side; live deploy and preview both fail. | Capture the exact secret name in `design.md`; add a CI-time precheck job that fails fast with a clear message; stakeholder verifies before first run. | stakeholder |
| R2 | Pre-condition drift (Firebase site renamed, Pages source flipped to "Deploy from a branch", label `docs/preview` renamed). | The CI jobs name these explicitly; any drift produces a deterministic CI failure. Document the four pre-conditions at the top of the docs section of the workflow. | stakeholder |
| R3 | PR previews from forked repositories cannot access the Firebase secret (GitHub Actions default). | Document the limitation in the runbook; the preview job is a no-op on fork PRs and logs a clear notice. | stakeholder |
| R4 | Pages `base` path breaks if the repo is renamed away from `lg5-loyalty-ledger`. | The `base` value lives in one environment variable consumed by the two builds; a repo rename requires updating one workflow input. | CI maintainer |
| R5 | Dual-build doubles VitePress build time per CI run. | VitePress build is fast on this content size; acceptable unless the site grows substantially. | CI maintainer |
| R6 | Documentation becomes framework-generic instead of service-specific. | Enforce repository-local source priority and page-level source mapping in `design.md`. | docs author |
| R7 | Architecture pages duplicate information already present in specs or contracts without adding interpretation. | Require each page to add explanation, navigation, and reader-oriented context rather than copying raw source material. | docs author |
| R8 | The ADR landing page drifts from the underlying ADR files. | Keep ADR summaries short, link every row to the original ADR, and treat the underlying ADR markdown as the canonical text. | docs author |
| R9 | Diagrams become too detailed or hard to maintain. | Limit diagrams to a small mandatory set and prefer readability over exhaustiveness. | docs author |
| R10 | QuickStart and Runbook overlap excessively. | Distinguish `QuickStart` as contributor bootstrap and `Runbook` as onboarding / operational orientation. | docs author |
| R11 | External references (`lg5-spring`, `blank-service`) are interpreted as normative for `loyalty-ledger`. | State explicitly that they provide conceptual ecosystem context only; repository-local sources remain normative. | docs author |

## Estimated artifact count

Additional content-maturity wave:

- New files: ~6-8 markdown pages
- Modified files: ~6-8 existing markdown pages plus spec refresh files
- New runtime artifacts: none
- New production code: none
- New CI jobs: none
- Validation scope: local docs build, navigation review, searchability review, link coherence review

## Definition of Done (Plan refresh)

- [x] Every newly added PRD requirement (`REQ-021`+) is covered by at least one deliverable.
- [x] Repository-local sources of truth are explicitly prioritized over ecosystem references.
- [x] The documentation page map is explicit and complete enough to drive the Tasks phase.
- [x] The ADR landing-page strategy is defined (hybrid curated/index model).
- [x] Diagram expectations are explicit and limited to a maintainable set.
- [x] The extension remains docs-only and introduces no service-runtime behavior changes.
