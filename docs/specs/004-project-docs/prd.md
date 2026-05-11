---
kind: spec
name: prd
feature: 004-project-docs
version: 0.1.0
description: Functional PRD for the loyalty-ledger project documentation surface. Technology-free.
---

# PRD — `project-docs`

## 1. Summary

Stakeholder verbatim:

> "As a developer joining the loyalty-ledger team, I want a single browsable
> web home where I can read the architecture overview, the operational
> runbooks, the live REST and event contracts, and the latest acceptance
> test report — all linked from one place and updated automatically when
> main moves. As a reviewer in a PR that touches docs, I want a temporary,
> shareable URL to inspect the proposed changes before approving."

The project will offer a single, publicly reachable browsable home for the
loyalty-ledger service's reference documentation. From that home, any
visitor can reach — in one click — the architecture overview, the
synchronous service contract, the asynchronous service contract, the
latest acceptance-test report, the decision-record index, and the
onboarding runbook. The surface regenerates automatically whenever the
trunk advances, and authors of proposed changes that touch documentation
can opt in to a temporary, shareable rendering of their change so
reviewers can inspect the rendered result before approving.

## 2. Problem

The artifacts that describe how loyalty-ledger works — architecture,
service contracts, acceptance-test results, decision records, onboarding
guidance — already exist, but they are scattered across build outputs,
automation run results, and repository folders. New contributors spend
hours hunting for them, reviewers of documentation changes can only see
raw source diffs (not the rendered result), and on-call operators have no
single bookmarkable home. The cost of this scatter grows with every new
feature shipped.

## 3. Users

- **Developer joining the loyalty-ledger team** — wants to read the
  architecture overview, the service contracts, the latest acceptance-test
  report, and the onboarding runbook from a single starting point, without
  having to learn the repository layout first.
- **Reviewer of a proposed change that touches documentation** — wants to
  see the rendered result of the proposed documentation change in a
  temporary, shareable location before approving, instead of reasoning
  about raw source diffs.
- **Change author who modifies documentation** — wants an explicit,
  low-friction way to signal "please render a preview of my change" and
  receive a shareable location pointing at that rendering.
- **On-call operator (future, secondary)** — wants a single bookmarkable
  home for runbooks during an incident. _(Listed for traceability; not the
  primary driver of this feature.)_

## 4. Success metrics

| Metric | Baseline | Target | Window |
|--------|---------:|-------:|--------|
| Median time for a new contributor to locate the six core documents (architecture, synchronous contract, asynchronous contract, acceptance report, decision index, onboarding runbook) starting from the repository root | `>15 min` | `<2 min` | first 30 days after launch |
| Share of trunk advances whose documentation surface reflects them without human action | `0%` | `100%` | rolling 30 days |
| Share of documentation-touching proposed changes that receive at least one review comment grounded in the rendered preview (not the raw diff) | `0%` | `>=70%` of opt-in previews | first 60 days after launch |
| End-to-end lag from a trunk advance to the regenerated surface reflecting it | n/a (no surface today) | `<10 minutes` | rolling 30 days |

## 5. Requirements (in scope)

| ID | Requirement | Acceptance |
|----|-------------|------------|
| REQ-001 | Publish a single browsable home for the loyalty-ledger reference documentation reachable from one stable, well-known location. | A visitor who opens the well-known location in a browser lands on a home page that identifies the loyalty-ledger service. |
| REQ-002 | From the home, expose a one-click navigation entry to the architecture overview. | The home page contains a visible link labeled in a way that identifies it as the architecture overview; following the link displays the architecture overview content. |
| REQ-003 | From the home, expose a one-click navigation entry to the synchronous service contract. | The home page contains a visible link to the synchronous service contract; following the link displays the contract content. |
| REQ-004 | From the home, expose a one-click navigation entry to the asynchronous service contract. | The home page contains a visible link to the asynchronous service contract; following the link displays the contract content. |
| REQ-005 | From the home, expose a one-click navigation entry to the latest acceptance-test report. | The home page contains a visible link to the acceptance-test report; following the link displays the report content corresponding to the latest trunk state. |
| REQ-006 | From the home, expose a one-click navigation entry to the decision-record index. | The home page contains a visible link to the decision-record index; following the link displays the index of decision records. |
| REQ-007 | From the home, expose a one-click navigation entry to the onboarding runbook. | The home page contains a visible link to the onboarding runbook; following the link displays the runbook content. |
| REQ-008 | The surface must be publicly reachable without any authentication step. | A visitor who has never authenticated against any system can open the well-known location and reach every entry listed in REQ-002…REQ-007. |
| REQ-009 | The surface must regenerate automatically whenever the trunk advances, with no human action required. | After a trunk advance, the surface reflects the new content within the freshness target, observed without any human-triggered regeneration. |
| REQ-010 | The end-to-end lag between a trunk advance and the surface reflecting that advance must be under ten minutes. | For each trunk advance, the time between the advance and the surface serving the new content is measurable and below ten minutes. |
| REQ-011 | The surface must reflect only the current trunk state (no per-release history). | Visiting the surface after a trunk advance no longer exposes the previous trunk state at the well-known location. |
| REQ-012 | An author of a proposed change must be able to opt in to a temporary, shareable rendering of that proposed change's documentation by applying an explicit label named `docs/preview` to the proposed change. | When the `docs/preview` label is applied to a proposed change, a shareable location is produced that renders the documentation as proposed by that change. |
| REQ-013 | The shareable preview location must be a location distinct from the main surface, scoped to the proposed change, and shareable as-is (anyone with the location can open it). | Reviewers can open the preview location in a browser without any authentication and see the rendering corresponding to the proposed change. |
| REQ-014 | A preview must remain reachable for at least seven days after the most recent opt-in event on the proposed change, then expire automatically without manual cleanup. | After seven days with no further opt-in event, the preview location is no longer reachable; no human action is required to make this happen. |
| REQ-015 | Previews must be opt-in only — proposed changes without the opt-in signal must not produce a preview. | A proposed change that does not carry the `docs/preview` label produces no preview location. |

## 6. Out of scope

- **Authoring tutorials, marketing copy, or external promotion content** — _(reason: the surface is a reference for contributors and reviewers, not external promotion; called out as a non-goal in the intent.)_
- **Replacing or substituting the canonical specifications under `docs/specs/`** — _(reason: those remain the canonical source of truth; the browsable home only links to and excerpts them.)_
- **Per-proposed-change previews of anything other than the documentation surface** — _(reason: only the documentation surface gets previews; previewing other artifacts is explicitly excluded by the intent.)_
- **Documentation for services other than loyalty-ledger** — _(reason: this surface is scoped to loyalty-ledger; cross-service documentation belongs to a separate intent.)_
- **Gated or authenticated access** — _(reason: this is an internal training project with no sensitive data; public reachability is a constraint.)_
- **Per-release historical archives of the surface** — _(reason: stakeholder decision is current-trunk-only, overwrite-on-advance.)_
- **Manual republication tooling for operators** — _(reason: regeneration is required to be automatic on trunk advance; a manual lever would contradict REQ-009.)_

## 7. Acceptance criteria (feature-level)

- [ ] A first-time visitor, given only the well-known location of the surface, can reach each of the six core documents (architecture overview, synchronous contract, asynchronous contract, acceptance-test report, decision-record index, onboarding runbook) in one click from the home.
- [ ] After a trunk advance, the surface reflects the new content automatically, within the freshness target of under ten minutes, with no human action.
- [ ] A proposed change that applies the `docs/preview` label produces a temporary, shareable preview location reachable without authentication; a proposed change without that label produces no preview.
- [ ] A preview is reachable for at least seven days after its most recent opt-in event and is no longer reachable beyond that, with no manual cleanup performed.
- [ ] All entries on the home are reachable without authentication from an environment that has never authenticated against any internal system.
- [ ] The success-metric targets in §4 are instrumentable: each target can be observed or measured after launch (even if the measurement instrument itself is delivered in a later phase).

## 8. Open questions

| Question | Decider | Due |
|---------|---------|-----|
| [NEEDS CLARIFICATION: Which file types and artifact families count as "documentation" for the purpose of being embedded in or linked from the surface — e.g. plain prose, structured contract documents, generated reports, diagrams, dependency or repository-evolution visualizations? Is the set fixed at the six entries in §5, or open-ended?] | stakeholder | before `/sdd-plan` |
| [NEEDS CLARIFICATION: What is the expected content of the onboarding runbook (REQ-007) — a checklist for first-day setup, a tour of the codebase, links to the other five documents, or something else? Who owns its content?] | stakeholder | before `/sdd-plan` |
| [NEEDS CLARIFICATION: Should the surface offer a search capability across all linked documents, or is one-click navigation from the home considered sufficient?] | stakeholder | before `/sdd-plan` |
| [NEEDS CLARIFICATION: When a regeneration detects a broken internal link inside the surface (a link from one document to another that no longer resolves), should the regeneration fail (and the surface keep serving the previous state) or succeed with a warning (and the surface serve the new state with a broken link)?] | stakeholder | before `/sdd-plan` |
| [NEEDS CLARIFICATION: When a trunk advance occurs but the source artifact for one of the six required entries is missing (e.g. the latest acceptance-test report has not been produced for this advance), should the surface (a) fail to regenerate, (b) regenerate and omit that entry, (c) regenerate and show the previous version of that entry with a "stale" indicator, or (d) something else?] | stakeholder | before `/sdd-plan` |
| [NEEDS CLARIFICATION: Are there accessibility expectations the surface must meet (e.g. screen-reader friendliness, contrast levels) and any localization or multi-language expectations, or is a single language with default accessibility acceptable for this internal training project?] | stakeholder | before `/sdd-plan` |
| [NEEDS CLARIFICATION: Are there visual-identity or branding expectations for the surface (logo, color palette, footer attribution), or is a neutral, unbranded presentation acceptable?] | stakeholder | before `/sdd-plan` |
| [NEEDS CLARIFICATION: Should the surface and each preview expose a visible indication of which trunk state (or which proposed-change state) they reflect, so a visitor can tell what they are looking at? If yes, what should that indication contain at minimum?] | stakeholder | before `/sdd-plan` |

## Definition of Done (PRD)

- [x] Every requirement has a stable ID (REQ-NNN).
- [x] No technology mentioned (no Spring, Kafka, Postgres, REST, …).
- [x] Every requirement has at least one acceptance criterion.
- [x] Pending clarifications marked with `[NEEDS CLARIFICATION: …]`.
- [x] Out-of-scope items explicitly listed with reason.
- [x] Stakeholder/owner identified (in the open questions table).
