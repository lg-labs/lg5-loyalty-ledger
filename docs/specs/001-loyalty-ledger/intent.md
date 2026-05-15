---
kind: intent
name: loyalty-ledger
version: 0.1.0
description: Pre-PRD intent one-pager. Captures the WHY and the problem framing before /sdd-specify produces a functional PRD.
---

# Intent — loyalty-ledger

> **Use this template via `/sdd-intent`.**
> The Intent is a **one-pager** that frames the problem *before* writing
> a PRD. It captures the **why**, the **who**, and the **desired
> outcome** — never the **what** (that's the PRD's job) or the **how**
> (that's the Plan's job).
>
> Keep it tight: if you exceed one screen, you are over-specifying.
> Mark unresolved questions with `[NEEDS CLARIFICATION: <question>]`.

## 1. Problem statement

Businesses cannot reward customer loyalty or manage point-based incentives because there is no centralized, reliable, and multi-tenant system to track point balances and transaction history.

## 2. Who feels it

- **Customer** — cannot see their current balance or trust that their "earn" actions (like purchases) result in points.
- **Tenant Admin** — cannot audit point distributions or reconcile rewards given to customers across different campaigns.
- **Platform Engineer** — suffers from managing fragmented point-tracking logic scattered across multiple services.

## 3. Why now

We are seeing a high churn rate in our multi-tenant platform because tenants lack basic retention tools like loyalty programs. Launching this now allows us to capture the upcoming holiday season marketing campaigns for our top-tier tenants.

## 4. Desired outcome

Tenants can reliably issue points to customers, and customers can see and spend their balances in real-time without discrepancy or delay, across all supported storefronts.

## 5. Success metrics

| Metric | Baseline | Target | Window |
|--------|---------:|-------:|--------|
| Balance Inquiry Latency | `> 500ms` | `< 50ms` | 30 days |
| Discrepancy Support Tickets | `15/week` | `< 1/week` | 60 days |
| Active Loyalty Users | `0` | `10,000` | 90 days |

## 6. Non-goals

- **Currency Conversion** — (reason: this service only handles 'points' as a unit, not FX rates for fiat cash-outs)
- **Frontend UI Components** — (reason: this is a backend-only ledger service; UIs are handled by the Storefront team)
- **Tax Reporting** — (reason: regulatory reporting for rewards is deferred to the Accounting service)

## 7. Constraints and hints

- **Multi-tenancy** — (source: Platform Architecture) Every transaction and balance must be strictly isolated by Tenant ID.
- **Auditability** — (source: Legal/Compliance) Transaction history must be immutable; no "updates" to existing ledger entries.
- **High Concurrency** — (rationale: point earn events can spike during flash sales or global events)

## 8. Open questions

| Question | Decider | Due |
|---------|---------|-----|
| Do points have an expiration policy at the ledger level? | Product Manager | 2026-05-20 |
| [NEEDS CLARIFICATION: What is the maximum point precision (integers vs decimals)?] | Stakeholder | TBD |

## Definition of Done (Intent)

- [x] Problem statement is one sentence, observation-flavored (not solution-flavored).
- [x] At least one user role identified with their specific pain.
- [x] "Why now" honestly answered (urgency or its absence).
- [x] Desired outcome described in observable terms, no solution naming.
- [x] At least one measurable success metric with baseline + target.
- [x] Non-goals list is explicit, not empty.
- [x] Open questions tabled (or "none" with a one-line justification).
- [x] Intent fits on one screen (~1 page).
