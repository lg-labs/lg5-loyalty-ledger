# Intent — `project-docs`

## 1. Problem statement

Developers joining or reviewing the loyalty-ledger service cannot find the
architecture overview, the operational runbooks, the live service contracts
(both synchronous and asynchronous), and the latest acceptance-test report
in one place — these artifacts exist but are scattered across build outputs,
automation run results, and repository folders, so onboarding and review take
longer than they should.

## 2. Who feels it

- **Developer joining the loyalty-ledger team** — spends hours hunting for
  "where is the architecture diagram / contract / runbook?" instead of
  reading and contributing.
- **Reviewer in a proposed change that touches documentation** — cannot
  see the *rendered* result of the change before approving, only the raw
  source diff.
- **On-call operator** (future) — has no single bookmarkable home for
  runbooks during an incident. _(secondary today, but listed to anchor
  non-goal scope.)_

## 3. Why now

The three previously closed features have just produced a large body of
artifacts: synchronous and asynchronous service contracts, an acceptance-test
report, dependency graphs, repository-evolution visualizations, and several
decision records. They are all valuable and all hidden. The cost of *not*
having a browsable home grows with every new feature shipped, and the team
is about to onboard new contributors.

## 4. Desired outcome

Anyone with a browser can reach a single, always-current, publicly
accessible location and from there reach — in one click — the architecture
overview, the synchronous service contract, the asynchronous service
contract, the latest acceptance-test report, the decision-record index,
and the onboarding runbook. When the trunk advances, that location reflects
the new state without manual intervention. When a proposed change wants
reviewers to see the rendered documentation result, the author can signal
that intent and obtain a temporary, shareable location scoped to that
change; reviewers open it, inspect, and approve (or reject) on what they
actually see.

## 5. Success metrics

| Metric | Baseline | Target | Window |
|--------|---------:|-------:|--------|
| Median time for a new contributor to locate the six core documents (architecture, sync contract, async contract, acceptance report, decision index, onboarding runbook) starting from the repository root | `>15 min` (self-reported, current scatter) | `<2 min` | first 30 days after launch |
| Share of trunk advances whose documentation surface reflects them without human action | `0%` | `100%` | rolling 30 days |
| Share of documentation-touching proposed changes that receive at least one review comment grounded in the rendered preview (not the raw diff) | `0%` (no preview exists) | `>=70%` of opt-in previews | first 60 days after launch |

## 6. Non-goals

- Authoring tutorials or marketing copy — _(reason: this is a reference
  surface for contributors, not external promotion.)_
- Replacing or substituting the specifications under `docs/specs/` — _(reason:
  those remain the canonical source of truth; the browsable home only links
  to and excerpts them.)_
- Hosting per-proposed-change previews of the production automation pipeline
  — _(reason: only the documentation surface gets previews; the production
  pipeline is out of scope.)_
- Documenting other services in the wider ecosystem — _(reason: this surface
  is scoped to loyalty-ledger; cross-service documentation is a separate
  intent.)_
- Gated or authenticated access — _(reason: this is an internal training
  project with no sensitive data; public reachability is a constraint, see
  §7.)_

## 7. Constraints and hints

- The documentation surface must regenerate automatically on every trunk
  advance — _(source: stakeholder.)_
- The documentation surface must be publicly reachable without
  authentication — _(source: stakeholder; this is an internal training
  project and carries no personal data.)_
- Per-proposed-change previews must be **opt-in** (only when the change
  author signals documentation intent), not automatic on every change —
  _(source: stakeholder, to keep noise and cost low.)_
- The surface must include — at minimum — architecture overview,
  synchronous service contract, asynchronous service contract,
  acceptance-test report, decision-record index, and onboarding runbook —
  _(source: stakeholder.)_

## 8. Open questions

| Question | Decider | Due |
|---------|---------|-----|
| [NEEDS CLARIFICATION: how long should an opt-in preview remain reachable after the proposed change is merged or closed?] | stakeholder | before `/sdd-specify` |
| [NEEDS CLARIFICATION: what is the acceptable freshness lag between a trunk advance and the regenerated surface — minutes? one hour?] | stakeholder | before `/sdd-specify` |
| [NEEDS CLARIFICATION: should the surface expose historical versions (per release) or only the current trunk state?] | stakeholder | before `/sdd-specify` |
| [NEEDS CLARIFICATION: what signal does a change author use to opt in to a preview — a label, a commit-message convention, a checkbox?] | stakeholder | before `/sdd-specify` |

## Definition of Done (Intent)

- [x] Problem statement is one sentence, observation-flavored (not solution-flavored).
- [x] At least one user role identified with their specific pain.
- [x] "Why now" honestly answered (urgency or its absence).
- [x] Desired outcome described in observable terms, no solution naming.
- [x] At least one measurable success metric with baseline + target.
- [x] Non-goals list is explicit, not empty.
- [x] Open questions tabled (or "none" with a one-line justification).
- [x] Intent fits on one screen (~1 page).
