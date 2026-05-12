# ADR-005: Gate per-PR preview channels on the `docs/preview` label with 7-day TTL

- **Status:** Accepted
- **Date:** 2026-05-11
- **Deciders:** stakeholder (`lglabs`)
- **Consulted:** sdd-planner (this feature)
- **Informed:** loyalty-ledger team

## Context

REQ-012…REQ-015 require:

- **REQ-012** — Authors can opt in to a temporary, shareable rendering
  by applying an explicit label exactly named `docs/preview`.
- **REQ-013** — The preview location is distinct from the main
  surface, scoped to the proposed change, shareable as-is, no auth.
- **REQ-014** — At least 7 days reachable after the most recent opt-in
  event; auto-expires; no manual cleanup.
- **REQ-015** — Opt-in only; PRs without the label produce no preview.

ADR-002 commits to Firebase Hosting as one of the live targets and
explicitly notes that GitHub Pages does not support per-PR preview
channels natively. Firebase Hosting **does** support preview channels
with a configurable TTL and an isolated URL per channel — designed for
exactly this use case.

The `docs/preview` label is pre-created on the repo (confirmed by the
stakeholder on 2026-05-10).

## Decision

A GitHub Actions job triggered by `pull_request` events (opened,
synchronized, reopened, labeled) will deploy the Firebase **Build B**
(`base: '/'` — see ADR-004) to a Firebase preview channel **only
when** the PR carries a label whose name is exactly `docs/preview`.
The channel name is derived from the PR number (e.g. `pr-<N>`). The
channel TTL is set to **7 days** and is refreshed on every subsequent
opt-in event (any label-bearing PR activity that runs this job). No
GitHub Pages preview is produced; no Allure preview is produced (see
ADR-003).

## Alternatives considered

- **Always-on previews (every PR, no label).**
  - Pros: zero ceremony for authors.
  - Cons: violates REQ-015 (opt-in only); inflates Firebase channel
    inventory and CI minutes for PRs that do not touch docs.
  - Why not chosen: directly contradicts the requirement.

- **Preview gated by file-path matching** (e.g. PR touches `docs/**`).
  - Pros: auto-detects intent.
  - Cons: misses PRs that touch upstream artifacts feeding the docs
    (Swagger UI HTML, AsyncAPI HTML, dependency graph); creates
    previews where none are wanted; harder to explain to authors than
    "apply this label".
  - Why not chosen: stakeholder explicitly chose an explicit label
    signal (REQ-012).

- **Preview gated by a slash-command comment** (e.g. `/preview-docs`).
  - Pros: more flexible (can carry arguments).
  - Cons: requires a `repository-dispatch` listener; harder to discover
    than a label; the bot-style UX is heavier.
  - Why not chosen: label is simpler and was explicitly chosen by the
    stakeholder.

- **Manual deploy script run by the author locally.**
  - Pros: zero CI cost for unwanted previews.
  - Cons: requires every author to have Firebase credentials; not
    shareable until the URL is communicated by hand; defeats the
    automation premise of REQ-012.
  - Why not chosen: violates the spirit of REQ-012 ("low-friction
    opt-in").

## Consequences

- **Positive:** REQ-012, REQ-013, REQ-014, REQ-015 all directly
  covered.
- **Positive:** Firebase channels auto-expire — no manual cleanup
  (REQ-014).
- **Positive:** the surface-state indicator (REQ-020) on the preview
  shows the PR identifier in addition to the commit SHA, distinguishing
  it from the main surface unambiguously.
- **Negative:** authors must remember to apply the label. Mitigated by
  documenting it in the onboarding runbook (REQ-007) and surfacing it
  in the PR template description.
- **Negative:** the preview URL pattern leaks the PR number, which is
  public anyway on a public repo — no incremental disclosure.
- **Neutral:** label removal does not actively delete the channel; the
  channel expires on its own after 7 days from the last opt-in event,
  which matches REQ-014 exactly.

## Constitutional impact

- **RULE-004 (service-module-shape)** — **N/A.**
- **RULE-012 (test-profiles)** — **N/A.**
- **RULE-014 (configuration-prefixes)** — **N/A.**
- **RULE-017 (build-commands)** — **clarifies.** The CI job invokes
  the same Make target used by the main Firebase deploy
  (`make docs-deploy-firebase`) with a channel parameter, keeping the
  CI/local equivalence intact.
- **RULE-018 (reference-projects)** — **clarifies.** No reference
  project ships a label-gated Firebase preview today; this is the
  prototype. Upstreaming captured in ADR-006.

## Implementation notes

- PRD requirements covered: REQ-012, REQ-013, REQ-014, REQ-015, plus
  reinforcement of REQ-020 (the preview's source-state indicator
  identifies the proposed change).
- Pre-condition: a Firebase service account with permission to deploy
  to channels on site `lglabs-loyalty-docs` is stored as a GitHub
  Actions secret. Captured in plan.md §"Risks" as an external
  dependency.
- Pre-condition: label `docs/preview` exists on the repo (confirmed
  2026-05-10).
- The job MUST NOT deploy when the label is absent, and MUST NOT
  deploy from forks where the secret is unavailable. The latter is a
  GitHub Actions default for `pull_request` from forks; documented in
  plan.md §"Risks".

## Related ADRs

- ADR-002 (Firebase as the host that makes preview channels possible).
- ADR-003 (Allure has NO preview — scope distinction).
- ADR-004 (preview uses Build B with `base: '/'`).

## Definition of Done (ADR)

- [x] Status is `Accepted`.
- [x] Decision stated in active voice.
- [x] More than one alternative documented.
- [x] Consequences cover positive AND negative.
- [x] Constitutional impact names every relevant `must` rule.
- [x] No `must` override → no time-box needed.

---

_Originally drafted: 2026-05-11 · Last reviewed: 2026-05-11._
