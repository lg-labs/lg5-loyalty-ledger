# ADR-003: Deploy the Allure report to a separate Firebase site and cross-link it from the VitePress home

- **Status:** Accepted
- **Date:** 2026-05-11
- **Deciders:** stakeholder (`lglabs`)
- **Consulted:** sdd-planner (this feature)
- **Informed:** loyalty-ledger team

## Context

REQ-005 requires one-click navigation from the documentation home to
the latest acceptance-test report. The Allure report is the
acceptance-test report shipped by features 001-003: it is regenerated
on every CI run and produced as a self-contained static site under
its own file structure (it is not Markdown, it is not authored by
humans, and its asset layout is owned by the Allure tooling).

The home (VitePress) regenerates on every trunk advance. The Allure
report regenerates more often (every test run). Bundling Allure assets
inside the VitePress dist would mean:

- Regenerating the entire VitePress dist on every test run (wasteful).
- Inflating the VitePress bundle with thousands of Allure asset files
  unrelated to the surface's own search index (REQ-017).
- Coupling the docs-publish DAG to the test DAG.

The Firebase project `lglabs-loyalty` (per ADR-002) has a pre-created
second site, `lglabs-loyalty-allure`, designed for exactly this
purpose.

## Decision

We will deploy the Allure report to its own Firebase site
(`lglabs-loyalty-allure`) on every trunk advance and **link** to it
from the VitePress home under the "Acceptance Test Report" entry
(REQ-005). We will **not** embed the Allure assets inside the
VitePress dist. Allure deploy is **live-overwrite-on-main only** — no
preview channel, no per-PR Allure publication.

## Alternatives considered

- **Embed Allure inside the VitePress dist** (e.g. as
  `public/allure/`).
  - Pros: single URL, single deploy job.
  - Cons: see "Context" above — wasteful bundle size, coupled DAGs,
    invalidates the VitePress search index with non-Markdown assets.
  - Why not chosen: cost/value worse than a separate site.

- **Allure on GitHub Pages too (mirror it).**
  - Pros: symmetry with the docs surface (which is dual-deployed —
    ADR-002).
  - Cons: Pages publishes a single source per repo; multiplexing two
    sites under one Pages deployment requires path-based routing that
    interacts awkwardly with VitePress's `base` (ADR-004). Extra
    complexity for no observable user benefit (the Allure URL is
    linked, not bookmarked by humans).
  - Why not chosen: complexity outweighs benefit; the Firebase mirror
    is sufficient.

- **No Allure publication; link to the GitHub Actions artifact
  download.**
  - Pros: zero hosting cost beyond GitHub.
  - Cons: artifact downloads require authentication; REQ-008 demands
    public reachability without authentication; the artifact also
    expires.
  - Why not chosen: violates REQ-008.

## Consequences

- **Positive:** the docs-build job and the Allure-deploy job are
  independent — slow Allure runs do not block the docs surface from
  publishing.
- **Positive:** REQ-005 satisfied via a stable Firebase URL that is
  always fresh.
- **Negative:** users open two different hostnames (one for docs, one
  for Allure). Mitigated by clear labeling on the home and by the
  source-state indicator (REQ-020) on both surfaces.
- **Negative:** the Allure site has no preview-channel story; a PR
  that changes acceptance-test output does not produce a preview
  Allure. Mitigated by ADR-005's scope (previews cover docs only) and
  by the fact that PR-time test feedback already lives in the GitHub
  Actions check page.

## Constitutional impact

- **RULE-004 (service-module-shape)** — **N/A.** Hosting topology is
  not a Maven-module concern.
- **RULE-012 (test-profiles)** — **N/A.** Allure consumes the test
  output, it does not change how tests run.
- **RULE-013 (testcontainers-opt-in)** — **N/A.**
- **RULE-018 (reference-projects)** — **clarifies.** No reference
  project ships an Allure-on-Firebase publication today; this feature
  is the prototype. Upstreaming captured in ADR-006.

## Implementation notes

- PRD requirements covered: REQ-005, with side-coverage of REQ-008
  (public reachability — Firebase serves without auth).
- The Allure deploy job consumes the `allure` artifact already produced
  by the ATDD job in `c-integration.yml` (feature 003).
- Cross-link from VitePress: the "Acceptance Test Report" sidebar
  entry is an external link to
  `https://lglabs-loyalty-allure.web.app/` (canonical) or the
  equivalent default-domain Firebase URL.

## Related ADRs

- ADR-002 (dual hosting, defines the Firebase project + sites).
- ADR-005 (preview channels — does NOT apply to Allure).
- feature 003 ADR-003 (the `dependency-graph` and `gource` artifacts
  this feature embeds in VitePress, deliberately distinguished from
  Allure which is NOT embedded).

## Definition of Done (ADR)

- [x] Status is `Accepted`.
- [x] Decision stated in active voice.
- [x] More than one alternative documented.
- [x] Consequences cover positive AND negative.
- [x] Constitutional impact names every relevant `must` rule.
- [x] No `must` override → no time-box needed.

---

_Originally drafted: 2026-05-11 · Last reviewed: 2026-05-11._
