# ADR-001: Use VitePress as the documentation site engine

- **Status:** Accepted
- **Date:** 2026-05-11
- **Deciders:** stakeholder (`lglabs`)
- **Consulted:** sdd-planner (this feature)
- **Informed:** loyalty-ledger team

## Context

Feature 004 must publish a single browsable home for the loyalty-ledger
reference documentation (PRD §1; REQ-001…REQ-007, REQ-017). The surface
must:

- Be a static site, regenerated on every trunk advance (REQ-009, REQ-010).
- Offer local (client-side) search across all linked documents (REQ-017).
- Embed Markdown plus HTML fragments produced by external generators
  shipped by features 001-003: the Swagger UI HTML wrapper (synchronous
  contract — REQ-003), the AsyncAPI Studio HTML wrapper (asynchronous
  contract — REQ-004), a `dependency-graph.png`, a `gource.mp4`
  (architecture / history), the ADR index (REQ-006), and a cross-link to
  the latest Allure report (REQ-005).
- Display a visible source-state indicator on every page (REQ-020).
- Grow open-endedly with future sections (REQ-016) without contract
  change.

Feature 003 ADR-003 already removed the original MkDocs `docs` job from
`c-integration.yml` and explicitly deferred the site-engine choice to
this feature.

## Decision

We will use **VitePress** to author and build the documentation surface.
The site source lives under `docs/` at the repo root (new directory
introduced by this feature).

## Alternatives considered

- **MkDocs-Material** — Python-based, was attempted in feature 003 and
  removed via feature 003 ADR-003.
  - Pros: simple, mature, widely used.
  - Cons: weaker dynamic JS/HTML embed story (Swagger UI / AsyncAPI
    Studio render in iframes only); Python toolchain on a Node-leaning
    workspace; was already explicitly retired one feature ago.
  - Why not chosen: directly contradicted by feature 003 ADR-003.

- **Docusaurus** — React-based SSG.
  - Pros: rich ecosystem, MDX, plugins.
  - Cons: heavier React SSR stack; more dependencies for a single
    reference site; configuration surface area is large.
  - Why not chosen: overkill for one service's reference site.

- **Antora** — multi-repo, AsciiDoc-first.
  - Pros: strong at federating docs across many repos.
  - Cons: AsciiDoc-first whereas this repo is Markdown-first
    (`docs/specs/**/*.md`); the multi-repo strength is unused (single
    service in scope per REQ "loyalty-ledger only").
  - Why not chosen: format mismatch + zero leverage from its core feature.

- **Backstage TechDocs** — TechDocs plugin inside a Backstage portal.
  - Pros: integrates with a broader developer-portal story.
  - Cons: requires standing up and operating Backstage — orders of
    magnitude more infrastructure than the surface itself.
  - Why not chosen: cost/value ratio incompatible with an internal
    training project.

## Consequences

- **Positive:** Markdown-native, Vite-fast HMR for authoring; built-in
  client-side search satisfies REQ-017 without bolt-ons; Vue runtime
  permits embedding the existing Swagger UI / AsyncAPI HTML wrappers as
  raw HTML pages co-located with the Markdown tree.
- **Positive:** Node toolchain aligns with what is already needed in CI
  for Swagger UI / AsyncAPI generation in feature 002.
- **Negative:** Introduces a Node build step (Node + a package manager)
  into a workspace whose primary stack is JVM. Mitigated by isolating
  the Node concerns under `docs/` and `package.json` at repo root.
- **Neutral:** The team must learn one more tool. The surface area used
  is small (config + Markdown + a few HTML pass-throughs).

## Constitutional impact

- **RULE-001 (stack-baseline)** — **N/A.** This rule pins JVM versions;
  the docs site runs in CI as Node tooling and does not relax or replace
  any JVM baseline.
- **RULE-004 (service-module-shape)** — **clarifies (deviation).** The
  canonical 8-module Maven shape does not anticipate a top-level `docs/`
  Node project. This feature introduces `docs/` as a sibling to the
  Maven modules. It is **not** a Maven module and does not pretend to
  be. See plan.md §"Deviation from RULE-004" for the full justification.
- **RULE-005 (no-custom-annotations)** — **N/A.** No Java annotations
  introduced.
- **RULE-006 (rest-media-type)** — **N/A directly.** The Swagger UI HTML
  wrapper produced by feature 002 already documents the
  `application/vnd.api.v1+json` content type; VitePress only embeds it.
- **RULE-014 (configuration-prefixes)** — **N/A.** No Spring
  configuration is added.
- **RULE-017 (build-commands)** — **clarifies.** New Make targets will
  wrap the Node invocations (`make docs-install`, `make docs-build`,
  `make docs-preview`) so the dev-loop entry point remains Make per the
  rule's spirit. See plan.md §"Cross-cutting concerns".
- **RULE-018 (reference-projects)** — **clarifies.** No lg5-spring
  reference repo currently ships a VitePress site; this feature is the
  prototype. The follow-up commitment to upstream the pattern as a
  bundle skill is captured in ADR-006.

## Implementation notes

- PRD: `docs/specs/004-project-docs/prd.md` — REQ-001…REQ-007, REQ-017,
  REQ-020.
- Plan: `docs/specs/004-project-docs/plan.md`.
- Related ADRs in this feature: ADR-002 (dual deployment), ADR-004
  (dual-build base paths), ADR-006 (upstreaming as a bundle skill).
- Related ADR in feature 003: ADR-003 (deferred site-engine choice to
  this feature).

## Related ADRs

- ADR-002, ADR-003, ADR-004, ADR-005, ADR-006 (this feature).
- feature 003 ADR-003 (deferred decision).

## Definition of Done (ADR)

- [x] Status is `Accepted`.
- [x] Decision stated in active voice.
- [x] More than one alternative documented.
- [x] Consequences cover positive AND negative.
- [x] Constitutional impact names every relevant `must` rule.
- [x] No `must` override → no time-box needed.

---

_Originally drafted: 2026-05-11 · Last reviewed: 2026-05-11._
