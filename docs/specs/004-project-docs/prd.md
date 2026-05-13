---
kind: spec
name: prd
feature: 004-project-docs
version: 0.2.0
description: Functional PRD for the loyalty-ledger project documentation surface, extended from docs-site infrastructure into mature technical documentation content.
---

# PRD — `project-docs`

## 1. Summary

Stakeholder verbatim (original intent):

> "As a developer joining the loyalty-ledger team, I want a single browsable
> web home where I can read the architecture overview, the operational
> runbooks, the live REST and event contracts, and the latest acceptance
> test report — all linked from one place and updated automatically when
> main moves. As a reviewer in a PR that touches docs, I want a temporary,
> shareable URL to inspect the proposed changes before approving."

Scope extension (current intent):

> "I want to update the current project documentation and complete the pages
> that are still unfinished, especially the technical architecture
> documentation, ADR landing page, onboarding, QuickStart, FAQ, and the
> service-specific explanations for Events, REST, DDD, and C4+1 views. The
> result should look like a current, high-quality technical documentation
> surface for the service."

The project offers a single, publicly reachable browsable home for the
`loyalty-ledger` reference documentation. From that home, any visitor can
reach the architecture overview, the synchronous service contract, the
asynchronous service contract, the latest acceptance-test report, the
decision-record index, and the onboarding runbook. This refresh extends the
surface beyond infrastructure and minimum-viable content into a mature
technical documentation surface with a complete architecture overview,
service-specific technical explanations, QuickStart guidance, FAQ content,
and richer navigable entry points for contributors and reviewers.

## 1.1 Scope alignment note

The technical documentation surface for `loyalty-ledger` must explain the
service in terms consistent with the `lg5-spring` framework and the
conventions of the lg5 ecosystem, while remaining specific to
`loyalty-ledger` itself.

`blank-service` may be consulted only as supporting ecosystem context to
enrich understanding of `lg5-spring`; it is not a normative source for the
structure, behavior, or architecture of `lg5-loyalty-ledger`. The
documentation must be grounded in the actual `lg5-loyalty-ledger`
repository, specifications, contracts, ADRs, and code.

## 2. Problem

The artifacts that describe how `loyalty-ledger` works — architecture,
service contracts, acceptance-test results, decision records, onboarding
guidance, and development conventions — already exist, but they are
scattered across build outputs, automation run results, repository folders,
and partially complete site sections. New contributors spend hours hunting
for them, reviewers of documentation changes can only see raw source diffs
(not the rendered result), and the current docs surface still contains pages
that act more like placeholders than useful technical entry points. The cost
of this scatter grows with every new feature shipped.

## 3. Users

- **Developer joining the loyalty-ledger team** — wants to read the
  architecture overview, the service contracts, the latest acceptance-test
  report, QuickStart guidance, and the onboarding runbook from a single
  starting point, without having to learn the repository layout first.
- **Reviewer of a proposed change that touches documentation** — wants to
  see the rendered result of the proposed documentation change in a
  temporary, shareable location before approving, instead of reasoning
  about raw source diffs.
- **Change author who modifies documentation** — wants an explicit,
  low-friction way to signal "please render a preview of my change" and
  receive a shareable location pointing at that rendering.
- **Contributor familiar with the service but not its rationale** — wants a
  service-specific explanation of the REST surface, event-driven model, DDD
  boundaries, and major design decisions without reading the codebase from
  scratch.
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
| Median time for a technical contributor to understand the service purpose, boundaries, and main write/read flows from the docs surface alone | `>30 min` | `<10 min` | first 30 days after refresh |
| Share of primary docs pages that act as useful technical entry points rather than placeholder surfaces | `<50%` | `100%` | first 30 days after refresh |

## 5. Requirements (in scope)

| ID | Requirement | Acceptance |
|----|-------------|------------|
| REQ-001 | Publish a single browsable home for the loyalty-ledger reference documentation reachable from one stable, well-known location. | A visitor who opens the well-known location in a browser lands on a home page that identifies the loyalty-ledger service. |
| REQ-002 | From the home, expose a one-click navigation entry to the architecture overview. | The home page contains a visible link labeled in a way that identifies it as the architecture overview; following the link displays the architecture overview content. |
| REQ-003 | From the home, expose a one-click navigation entry to the synchronous service contract. | The home page contains a visible link to the synchronous service contract; following the link displays the contract content. |
| REQ-004 | From the home, expose a one-click navigation entry to the asynchronous service contract. | The home page contains a visible link to the asynchronous service contract; following the link displays the contract content. |
| REQ-005 | From the home, expose a one-click navigation entry to the latest acceptance-test report. | The home page contains a visible link to the acceptance-test report; following the link displays the report content corresponding to the latest trunk state. |
| REQ-006 | From the home, expose a one-click navigation entry to the decision-record index. | The home page contains a visible link to the decision-record index; following the link displays the index of decision records. |
| REQ-007 | From the home, expose a one-click navigation entry to the onboarding runbook. The runbook in this feature was originally a minimum-viable stub authored by the stakeholder: a first-day setup checklist, a brief tour of the repository, and links to the other five core entries. This refresh may enrich the runbook, but it must remain focused on onboarding and operational orientation. | The home page contains a visible link to the onboarding runbook; following the link displays the runbook content with at least the three stated sections. |
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
| REQ-021 | The documentation surface must provide a complete technical architecture overview for `loyalty-ledger`, written for engineers who need to understand the service without starting from the codebase. | A reader can identify the service purpose, system role, main responsibilities, key module boundaries, and the primary write/read flows from the architecture section alone. |
| REQ-022 | The documentation surface must provide a `QuickStart` guide in English for technical contributors. | A new contributor can clone the repository, initialize required local dependencies, run the main build/test commands, preview the documentation locally, and identify the next recommended documents to read by following the `QuickStart` page alone. |
| REQ-023 | The documentation surface must provide an English `FAQ` covering recurring contributor and reviewer questions. | The `FAQ` page answers common questions about setup, repository structure, documentation sources of truth, architecture navigation, contracts, and expected workflows, and each answer links to a more authoritative page when applicable. |
| REQ-024 | The documentation surface must explain how `loyalty-ledger` models its REST interface, event-driven interactions, and domain boundaries in a service-specific way. | A reader can understand the service's HTTP read surface, inbound and outbound event roles, traceability expectations, and domain/application/infrastructure separation without relying on generic framework-only explanations. |
| REQ-025 | The documentation surface must include practical architectural views in the style of C4+1, using readable diagrams where helpful. | The site contains at least a system-context view, a container-or-module-level view, and one or more dynamic views of important service flows; the diagrams are understandable and consistent with the repository and specifications. |
| REQ-026 | Placeholder-driven sections of the documentation surface must be replaced by useful content or by navigable indexes that act as real entry points. | The primary pages for architecture, ADRs, API, and events no longer function as empty placeholder surfaces; each provides meaningful explanation, navigation, or indexing even when linked artifacts remain external. |
| REQ-027 | The documentation must describe `loyalty-ledger` using concepts and terminology consistent with `lg5-spring`, while remaining specific to the actual service implementation and decisions of this repository. | A reader can understand how `loyalty-ledger` fits within the `lg5-spring` ecosystem without the documentation reading like a generic framework template or a copy of another service's documentation. |
| REQ-028 | The documentation must be grounded in the real sources of truth of this repository. External ecosystem references may enrich understanding, but must not replace service-specific facts. | The published pages for architecture, onboarding, quick start, ADRs, REST, events, DDD, and FAQ are traceable to repository-local sources of truth; external references are used only for context and orientation. |

## 6. Out of scope

- **Authoring tutorials, marketing copy, or external promotion content** — _(reason: the surface is a reference for contributors and reviewers, not external promotion.)_
- **Replacing or substituting the canonical specifications under `docs/specs/`** — _(reason: those remain the canonical source of truth; the browsable home only links to and interprets them.)_
- **Per-proposed-change previews of anything other than the documentation surface** — _(reason: only the documentation surface gets previews; previewing other artifacts is explicitly excluded.)_
- **Documentation for services other than `loyalty-ledger`** — _(reason: this surface is scoped to `loyalty-ledger`; cross-service documentation belongs to a separate intent.)_
- **Gated or authenticated access** — _(reason: public reachability is a constraint.)_
- **Per-release historical archives of the surface** — _(reason: stakeholder decision is current-trunk-only, overwrite-on-advance.)_
- **Manual republication tooling for operators** — _(reason: regeneration is required to be automatic on trunk advance.)_
- **Formal localization or multilingual publication** — _(reason: this refresh standardizes on English for the documentation surface.)_
- **Treating `blank-service` as a template for `loyalty-ledger` architecture or behavior** — _(reason: it may enrich ecosystem understanding only; repository-local sources remain normative.)_

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
- [ ] A first-time technical contributor can use the documentation surface to understand what `loyalty-ledger` is, how it is structured, how it communicates, and how to get started locally.
- [ ] The architecture section explains the service through service-specific narrative plus readable diagrams, not only through raw links or placeholders.
- [ ] The ADR section acts as a real navigable index, with curated summaries and direct links to the underlying ADR documents.
- [ ] The API and Events sections provide explanatory context in addition to linking the generated contract viewers.
- [ ] External references such as `lg5-spring` and `blank-service` enrich ecosystem understanding but do not override repository-local sources of truth.

## 8. Open questions

_All clarifications raised in the Specify phase have been resolved by the
stakeholder before `/sdd-plan`. Resolution log:_

| # | Resolved question | Decision |
|---|------------------|----------|
| 1 | Which file types and artifact families count as "documentation"? Fixed set or open-ended? | **Open-ended**, starting from the six required entries in REQ-002…REQ-007. Captured as REQ-016. |
| 2 | Expected content of the onboarding runbook (REQ-007); owner? | **Minimum-viable stub** in the original feature wave, enriched in the extension while keeping onboarding as its focus. **Owner**: stakeholder (`lglabs`). |
| 3 | Should the surface offer search? | **Yes — local (client-side) search.** Captured as REQ-017. |
| 4 | Broken internal links: fail or warn? | **Warn — regeneration succeeds with a warning in the log.** Captured as REQ-018. |
| 5 | Missing source artifact for a required entry on a trunk advance? | **Serve the previous version with a "stale" indicator**, or a "no content yet" placeholder if no previous version exists. Captured as REQ-019. |
| 6 | Localization expectations for the refresh? | **Single language: English.** Captured in §6 (out of scope for multilingual support). |
| 7 | Can external ecosystem references define the service? | **No.** `lg5-spring` provides conceptual vocabulary; `blank-service` adds context only; repository-local sources remain normative. Captured as REQ-027 and REQ-028. |
| 8 | Should the technical-content wave be a new feature or an extension of feature 004? | **Extension of `004-project-docs`.** Preserve the same feature folder and history. |

## Definition of Done (PRD)

- [x] Every requirement has a stable ID (REQ-NNN).
- [x] No technology mentioned where a functional requirement is sufficient; implementation details are deferred to later phases.
- [x] Every requirement has at least one acceptance criterion.
- [x] Pending clarifications marked with `[NEEDS CLARIFICATION: …]`.
- [x] Out-of-scope items explicitly listed with reason.
- [x] Stakeholder/owner identified (in the open questions table).
