# ADR-006: Upstream the docs scaffold as a reusable `lg5-vitepress-docs` bundle skill in v4.0.0

- **Status:** Accepted
- **Date:** 2026-05-11
- **Deciders:** stakeholder (`lglabs`)
- **Consulted:** sdd-planner (this feature)
- **Informed:** lg5-spring-agent-os maintainers, loyalty-ledger team

## Context

ADR-001 selects VitePress; ADR-002…ADR-005 define the dual-target
deploy, preview-channel, and asset-handling story. None of these
patterns currently exist in the lg5-spring-agent-os bundle (v3.0.0) or
in any reference service under `/tmp/lg5-study/`. This feature is the
prototype implementation.

Historical precedent in the bundle: skills `lg5-github-actions`,
`lg5-api-docs`, and `lg5-allure-report` were each born as one-service
prototypes and only became bundle skills after one full feature proved
the pattern.

The stakeholder wants the same lifecycle here: prove it in
loyalty-ledger first, then extract.

## Decision

After feature 004 merges to trunk, we will open a follow-up PR
upstream to the `lg5-spring-agent-os` repository that introduces:

- A new **skill** `lg5-vitepress-docs` containing the scaffold guidance
  (VitePress config skeleton, `docs/` directory shape, Make targets,
  reference CI job snippets, Firebase + Pages deploy patterns,
  preview-channel pattern).
- A new **command** `/scaffold-docs` that materializes the scaffold
  in a target service.
- **Mode F2** (skill + command, **no subagent**) — consistent with how
  `lg5-api-docs` and `lg5-allure-report` are shaped today.

The target bundle version is **v4.0.0**, a MINOR bump from v3.0.0
under the bundle's documented "additive features are MINOR" policy
(the constitution change-rule in `CONSTITUTION.md` ties MAJOR to
constitutional-rule changes, not to additive skills). The stakeholder
explicitly chose v4.0.0 as the target tag.

This ADR documents the **commitment** to upstream. It does NOT modify
the bundle from inside this feature.

## Alternatives considered

- **Upstream eagerly, before this feature merges.**
  - Pros: anyone else could use the skill immediately.
  - Cons: nothing has proven the shape works end-to-end; high risk of
    needing to rewrite the skill after first real use.
  - Why not chosen: contradicts the historical precedent of prove-first
    in the bundle.

- **Never upstream; keep the pattern in-service.**
  - Pros: zero coordination cost.
  - Cons: the next service to want docs has to reinvent everything;
    the bundle's value proposition is precisely to avoid this.
  - Why not chosen: contradicts the bundle's purpose.

- **Upstream as MAJOR (v5.0.0 if v4 used elsewhere; or v4.0.0 with
  MAJOR semantics).**
  - Pros: cleaner signal to consumers that the bundle expanded.
  - Cons: the constitution explicitly reserves MAJOR for changes to
    the 15 constitutional rules; this skill adds none, changes none,
    overrides none.
  - Why not chosen: misaligns with the bundle's documented versioning
    contract.

## Consequences

- **Positive:** the next lg5-spring service to need docs can run
  `/scaffold-docs` and inherit a battle-tested pattern.
- **Positive:** lessons learned in this feature (e.g. the exact
  parameterized `base` build, the label-gated preview job, the
  separate Allure site) flow into the bundle without lossy re-design.
- **Negative:** there is a window (between feature 004 merge and the
  upstream PR landing) during which the pattern lives only here; any
  fix found in another service in that window must be backported.
  Mitigated by keeping the window short (target: same week as merge).
- **Neutral:** no behavior change in feature 004 itself — this ADR is
  a forward commitment, not a runtime decision.

## Constitutional impact

- **RULE-004 (service-module-shape)** — **N/A.** This ADR scopes a
  follow-up PR in a different repo (the bundle), not this service.
- **RULE-017 (build-commands)** — **confirms.** The Make targets
  introduced in this feature (`make docs-build`, `make docs-deploy-*`,
  `make docs-preview`) become the canonical interface and are part of
  what the upstreamed skill standardizes.
- **RULE-018 (reference-projects)** — **clarifies.** Upstreaming makes
  the pattern citable from the bundle going forward, closing the
  current gap noted in ADR-001/-002/-003/-005.

## Implementation notes

- Out-of-scope of this feature: the upstream PR is opened **after**
  feature 004 merges. This ADR is a commitment record, not a task.
- Target repo: `lg5-spring-agent-os`.
- Target version: **v4.0.0** (additive MINOR over v3.0.0).
- Skill name: `lg5-vitepress-docs`. Command: `/scaffold-docs`.

## Related ADRs

- ADR-001…ADR-005 (this feature) — together define what gets
  upstreamed.

## Definition of Done (ADR)

- [x] Status is `Accepted`.
- [x] Decision stated in active voice.
- [x] More than one alternative documented.
- [x] Consequences cover positive AND negative.
- [x] Constitutional impact names every relevant `must` rule.
- [x] No `must` override → no time-box needed.

---

_Originally drafted: 2026-05-11 · Last reviewed: 2026-05-11._
