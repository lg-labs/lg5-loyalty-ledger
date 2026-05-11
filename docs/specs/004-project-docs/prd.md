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
| REQ-007 | From the home, expose a one-click navigation entry to the onboarding runbook. The runbook in this feature is a minimum-viable stub authored by the stakeholder: a first-day setup checklist, a brief tour of the repository, and links to the other five core entries. | The home page contains a visible link to the onboarding runbook; following the link displays the runbook content with at least the three stated sections. |
| REQ-008 | The surface must be publicly reachable without any authentication step. | A visitor who has never authenticated against any system can open the well-known location and reach every entry listed in REQ-002…REQ-007. |
| REQ-009 | The surface must regenerate automatically whenever the trunk advances, with no human action required. | After a trunk advance, the surface reflects the new content within the freshness target, observed without any human-triggered regeneration. |
| REQ-010 | The end-to-end lag between a trunk advance and the surface reflecting that advance must be under ten minutes. | For each trunk advance, the time between the advance and the surface serving the new content is measurable and below ten minutes. |
| REQ-011 | The surface must reflect only the current trunk state (no per-release history). | Visiting the surface after a trunk advance no longer exposes the previous trunk state at the well-known location. |
| REQ-012 | An author of a proposed change must be able to opt in to a temporary, shareable rendering of that proposed change's documentation by applying an explicit label named `docs/preview` to the proposed change. | When the `docs/preview` label is applied to a proposed change, a shareable location is produced that renders the documentation as proposed by that change. |
| REQ-013 | The shareable preview location must be a location distinct from the main surface, scoped to the proposed change, and shareable as-is (anyone with the location can open it). | Reviewers can open the preview location in a browser without any authentication and see the rendering corresponding to the proposed change. |
| REQ-014 | A preview must remain reachable for at least seven days after the most recent opt-in event on the proposed change, then expire automatically without manual cleanup. | After seven days with no further opt-in event, the preview location is no longer reachable; no human action is required to make this happen. |
| REQ-015 | Previews must be opt-in only — proposed changes without the opt-in signal must not produce a preview. | A proposed change that does not carry the `docs/preview` label produces no preview location. |
| REQ-016 | The set of documentation surfaced by the home is open-ended, starting from the six required entries (REQ-002…REQ-007); additional sections may be added in future advances without contract change. | The site can grow with new sections beyond the six required entries; adding a section does not require revisiting this PRD. |
| REQ-017 | The surface must offer a local (client-side) search capability across all linked documents. | A visitor can enter a query on the home and receive a list of matching documents reachable in one click from the result. |
| REQ-018 | A regeneration that detects a broken internal link (a link from one document to another that no longer resolves) must succeed and publish the new state, while emitting a warning in the regeneration log for human follow-up. | A broken internal link does not block publishing; the regeneration log records the broken link as a warning. |
| REQ-019 | When a trunk advance occurs but the source artifact for one of the six required entries (REQ-002…REQ-007) is missing for that advance, the surface must regenerate and either serve the previous version of that entry with a visible "stale" indicator next to it, or, if no previous version exists, serve a visible placeholder labeled to indicate that no content has been produced yet. | After a trunk advance where one of the six source artifacts is missing, the surface remains reachable and the affected entry shows either a stale indicator on the previous version or a "no content yet" placeholder. |
| REQ-020 | Each surface (the main surface and every preview) must expose a visible indication of the source state it reflects, containing at minimum the short commit identifier of the source trunk state and a timestamp of the regeneration. Preview surfaces must additionally show the identifier of the proposed change they correspond to. | Any visitor can read the source-state indicator on every surface and identify which trunk state and (for previews) which proposed-change state they are looking at. |

## 6. Out of scope

- **Authoring tutorials, marketing copy, or external promotion content** — _(reason: the surface is a reference for contributors and reviewers, not external promotion; called out as a non-goal in the intent.)_
- **Replacing or substituting the canonical specifications under `docs/specs/`** — _(reason: those remain the canonical source of truth; the browsable home only links to and excerpts them.)_
- **Per-proposed-change previews of anything other than the documentation surface** — _(reason: only the documentation surface gets previews; previewing other artifacts is explicitly excluded by the intent.)_
- **Documentation for services other than loyalty-ledger** — _(reason: this surface is scoped to loyalty-ledger; cross-service documentation belongs to a separate intent.)_
- **Gated or authenticated access** — _(reason: this is an internal training project with no sensitive data; public reachability is a constraint.)_
- **Per-release historical archives of the surface** — _(reason: stakeholder decision is current-trunk-only, overwrite-on-advance.)_
- **Manual republication tooling for operators** — _(reason: regeneration is required to be automatic on trunk advance; a manual lever would contradict REQ-009.)_
- **Formal accessibility conformance (e.g. WCAG audit) and localization or multi-language support** — _(reason: this is an internal training project; the surface targets a single language with the default accessibility inherited from the underlying presentation; formal conformance can be revisited as a follow-up feature.)_
- **Custom branding or visual identity (logo, color palette, corporate footer)** — _(reason: stakeholder explicitly accepts a neutral, default presentation; branding can be revisited when a real visual identity exists.)_

## 7. Acceptance criteria (feature-level)

- [ ] A first-time visitor, given only the well-known location of the surface, can reach each of the six core documents (architecture overview, synchronous contract, asynchronous contract, acceptance-test report, decision-record index, onboarding runbook) in one click from the home.
- [ ] After a trunk advance, the surface reflects the new content automatically, within the freshness target of under ten minutes, with no human action.
- [ ] A proposed change that applies the `docs/preview` label produces a temporary, shareable preview location reachable without authentication; a proposed change without that label produces no preview.
- [ ] A preview is reachable for at least seven days after its most recent opt-in event and is no longer reachable beyond that, with no manual cleanup performed.
- [ ] All entries on the home are reachable without authentication from an environment that has never authenticated against any internal system.
- [ ] Every surface (main and preview) shows a visible indicator of the source state it reflects (short commit identifier + regeneration timestamp; previews additionally identify the proposed change).
- [ ] Local (client-side) search across the surface returns at least one result for any term known to appear in any of the six core documents.
- [ ] When a source artifact is missing for one of the six required entries, the surface still serves successfully — either the previous version with a "stale" indicator, or a placeholder labeled "no content yet" if no previous version exists.
- [ ] A regeneration with a broken internal link produces a visible warning entry but does not block publishing.
- [ ] The success-metric targets in §4 are instrumentable: each target can be observed or measured after launch (even if the measurement instrument itself is delivered in a later phase).

## 8. Open questions

_All clarifications raised in the Specify phase have been resolved by
the stakeholder before `/sdd-plan`. Resolution log:_

| # | Resolved question | Decision |
|---|------------------|----------|
| 1 | Which file types and artifact families count as "documentation"? Fixed set or open-ended? | **Open-ended**, starting from the six required entries in REQ-002…REQ-007. Captured as REQ-016. |
| 2 | Expected content of the onboarding runbook (REQ-007); owner? | **Minimum-viable stub** in this feature: first-day setup checklist + brief repository tour + links to the other five entries. **Owner**: stakeholder (`lglabs`). Captured inline in REQ-007. |
| 3 | Should the surface offer search? | **Yes — local (client-side) search**. Captured as REQ-017. |
| 4 | Broken internal links: fail or warn? | **Warn — regeneration succeeds with a warning in the log**. Captured as REQ-018. |
| 5 | Missing source artifact for a required entry on a trunk advance? | **Serve the previous version with a "stale" indicator**, or a "no content yet" placeholder if no previous version exists. Captured as REQ-019. |
| 6 | Accessibility / localization expectations? | **None formal**: single language; default accessibility inherited from the underlying presentation. Captured in §6 (out of scope). |
| 7 | Visual identity / branding expectations? | **Neutral, default presentation**. No custom logo or palette. Captured in §6 (out of scope). |
| 8 | Visible indicator of source state on each surface? | **Yes — short commit identifier + regeneration timestamp; previews also identify the proposed change**. Captured as REQ-020. |

## Definition of Done (PRD)

- [x] Every requirement has a stable ID (REQ-NNN).
- [x] No technology mentioned (no Spring, Kafka, Postgres, REST, …).
- [x] Every requirement has at least one acceptance criterion.
- [x] Pending clarifications marked with `[NEEDS CLARIFICATION: …]`.
- [x] Out-of-scope items explicitly listed with reason.
- [x] Stakeholder/owner identified (in the open questions table).
