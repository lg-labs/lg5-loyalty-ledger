# ADR-002: Dual-deploy the documentation surface to GitHub Pages and Firebase Hosting

- **Status:** Accepted
- **Date:** 2026-05-11
- **Deciders:** stakeholder (`lglabs`)
- **Consulted:** sdd-planner (this feature)
- **Informed:** loyalty-ledger team

## Context

REQ-001 requires "one stable, well-known location"; REQ-008 requires
public reachability without authentication; REQ-009/REQ-010 require
automatic regeneration with sub-10-minute lag; REQ-012…REQ-015 require
opt-in per-PR previews with 7-day TTL.

GitHub Pages and Firebase Hosting differ in capability: Pages is
zero-cost and GitHub-native but has no per-PR preview channels;
Firebase supports preview channels natively but introduces an external
vendor dependency. The stakeholder explicitly wants operational
experience with both and accepts the redundancy.

Pre-conditions (confirmed by the stakeholder on 2026-05-10):

- GitHub repo `lg-labs/lg5-loyalty-ledger` has Pages enabled with
  source = "GitHub Actions".
- Firebase project `lglabs-loyalty` exists with two pre-created sites:
  `lglabs-loyalty-docs` (for the VitePress surface) and
  `lglabs-loyalty-allure` (for the Allure report — see ADR-003).
- A repository label named exactly `docs/preview` is pre-created
  (used by ADR-005).

## Decision

We will deploy the same VitePress output to **both** GitHub Pages and
Firebase Hosting on every trunk advance. The two are equal-rank live
mirrors, not primary/fallback. Per-PR previews go to Firebase only,
gated by the `docs/preview` label (see ADR-005). The Allure report
goes to a separate Firebase site (see ADR-003).

## Alternatives considered

- **Pages only.**
  - Pros: zero external dependency, lowest operational cost.
  - Cons: no native per-PR preview channels → REQ-012…REQ-015 unmet
    without a custom workaround.
  - Why not chosen: contradicts REQ-012…REQ-015.

- **Firebase only.**
  - Pros: covers previews natively; single deployment target simplifies
    CI.
  - Cons: introduces vendor lock; loses the "GitHub-native, zero
    external cost" baseline; loses the explicit redundancy benefit the
    stakeholder values.
  - Why not chosen: stakeholder explicitly wants both for learning and
    redundancy.

- **Pages live + Firebase preview-only.**
  - Pros: minimizes Firebase usage to its differentiator (previews).
  - Cons: a preview environment that differs from production (different
    host, different `base` path) is a worse review experience; loses A/B
    compare value of two equal-rank mirrors.
  - Why not chosen: stakeholder prefers symmetry between the two live
    mirrors.

## Consequences

- **Positive:** redundancy — either target failing still serves the
  surface. REQ-001 stays satisfied even under one outage.
- **Positive:** A/B observation between two hosting platforms is
  available to the team (learning value).
- **Positive:** REQ-012…REQ-015 covered via the Firebase preview
  channel mechanism (see ADR-005).
- **Negative:** two deploy targets means two CI jobs, two sets of
  credentials, two `base`-path builds (see ADR-004). Operationally
  heavier than a single-target design.
- **Negative:** the two live URLs are not the same string; the team
  must pick one as the canonical bookmark. **Decision (this ADR):** the
  GitHub Pages URL is the canonical bookmark for human linking; the
  Firebase URL is the secondary mirror. Both are advertised on the
  home page.
- **Neutral:** REQ-011 (current-trunk-only, no per-release history) is
  satisfied identically on both targets (overwrite-on-advance).

## Constitutional impact

- **RULE-001 (stack-baseline)** — **N/A.** Deployment targets do not
  alter the JVM baseline.
- **RULE-004 (service-module-shape)** — **N/A.** Deployment is a CI
  concern, not a Maven-module concern.
- **RULE-008 (outbox-mandatory)** — **N/A.** No Kafka traffic.
- **RULE-014 (configuration-prefixes)** — **N/A.** No Spring
  configuration.
- **RULE-017 (build-commands)** — **clarifies.** Make targets will wrap
  the deploy invocations (`make docs-deploy-pages`,
  `make docs-deploy-firebase`) for local-equivalence; CI jobs invoke
  the same targets.
- **RULE-018 (reference-projects)** — **clarifies.** No lg5-spring
  reference repo currently dual-deploys docs; this feature establishes
  the pattern. Upstreaming captured in ADR-006.

## Implementation notes

- PRD requirements covered: REQ-001, REQ-008, REQ-009, REQ-010,
  REQ-011, plus the foundation for REQ-012…REQ-015 (see ADR-005).
- Pre-conditions captured here are repeated in plan.md §"Risks" as
  external dependencies that must be verified before the first CI run.
- Firebase project: `lglabs-loyalty`. Sites: `lglabs-loyalty-docs`,
  `lglabs-loyalty-allure`.
- GitHub Pages source: "GitHub Actions".

## Related ADRs

- ADR-001 (site engine).
- ADR-003 (Allure on a separate Firebase site).
- ADR-004 (dual `base` paths — required because Pages and Firebase
  serve at different roots).
- ADR-005 (preview channels gated by label).
- feature 003 ADR-002 (deferred Firebase Hosting from feature 003 to
  here).

## Definition of Done (ADR)

- [x] Status is `Accepted`.
- [x] Decision stated in active voice.
- [x] More than one alternative documented.
- [x] Consequences cover positive AND negative.
- [x] Constitutional impact names every relevant `must` rule.
- [x] No `must` override → no time-box needed.

---

_Originally drafted: 2026-05-11 · Last reviewed: 2026-05-11._
