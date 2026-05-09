---
kind: prd
name: loyalty-ledger
version: 0.1.0
description: Functional PRD for the loyalty-ledger service — point balances and immutable ledger of credits/debits driven by order lifecycle events.
---

# PRD — `loyalty-ledger`

## 1. Summary

`loyalty-ledger` is a back-office service that maintains, for every
customer of the platform, a **loyalty point balance** and an **immutable
ledger of point movements** (credits and debits). Movements are driven
by the customer's order lifecycle: paid orders credit points; cancelled
or refunded orders debit the points previously credited for the same
order. The service exposes read operations to query a customer's current
balance and movement history, and publishes a business event each time a
balance changes so that other services on the platform can react.

## 2. Problem

The platform currently has no source of truth for customer loyalty
points: order events flow through the platform but no service translates
them into a durable, auditable balance. Without a ledger, we cannot
answer "how many points does customer X have today?", "why did the
balance change?", or "which order originated this credit?". Other
services (e.g. promotions, notifications) cannot react to balance
changes today because no event signals them.

## 3. Users

- **End customer (indirect)** — wants to see their current point balance
  and a history of how points were earned or removed, so they can trust
  the loyalty programme.
- **Customer-support agent** — wants to look up any customer's balance
  and movements, in reverse-chronological order with paging, to resolve
  enquiries and disputes.
- **Order-management service (system role)** — emits the business events
  that drive credits and debits (`order paid`, `order cancelled`,
  `order refunded`).
- **Downstream subscriber services (system role)** — react to the
  `customer balance updated` business event (e.g. promotions engine,
  notification service).

## 4. Success metrics

| Metric                                                          | Baseline | Target | Window      |
|-----------------------------------------------------------------|---------:|-------:|-------------|
| % of paid-order events that result in a credit movement         | n/a      | 100%   | rolling 24h |
| % of cancellation/refund events that result in a debit movement (when a prior credit exists) | n/a | 100% | rolling 24h |
| Duplicate input events that produce a duplicate movement        | n/a      | 0      | rolling 7d  |
| Lag between input event acceptance and outbound `balance updated` event | n/a | [NEEDS CLARIFICATION: SLA target?] | rolling 24h |
| Customer-support look-ups served successfully                   | n/a      | ≥ 99.9% | rolling 30d |

## 5. Requirements (in scope)

| ID      | Requirement                                                                                                                                                                                            | Acceptance                                                                                                                                                                                                                                                                |
|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| REQ-001 | When a customer's order becomes paid, the service credits points to that customer.                                                                                                                     | A credit movement appears in the customer's ledger and the customer's current balance increases by the credited amount.                                                                                                                                                    |
| REQ-002 | The number of points credited equals the order's monetary amount in whole units, rounded down (floor).                                                                                                 | An order paid for `12.95` credits exactly `12` points; an order paid for `100.00` credits exactly `100` points; an order paid for `0.50` credits `0` points and still produces a movement of value `0`. [NEEDS CLARIFICATION: must a zero-value credit produce a movement, or be skipped?] |
| REQ-003 | Crediting is idempotent with respect to the originating order: the same paid-order event arriving more than once must produce exactly one credit movement.                                             | Replaying the same paid-order event N times leaves the customer's balance and ledger identical to the state after a single delivery; no duplicate movement is appended.                                                                                                    |
| REQ-004 | When a customer's order is cancelled or refunded *after* it had been credited, the service debits the customer by the same amount that was credited for that order.                                    | A debit movement appears in the customer's ledger with the same magnitude as the prior credit for that order; the customer's current balance decreases by that amount.                                                                                                     |
| REQ-005 | If a cancellation/refund event arrives for an order that was never credited, the service must not produce a debit movement.                                                                            | No movement is appended; the customer's balance is unchanged. [NEEDS CLARIFICATION: should this case be observable/logged for ops, or silently ignored?]                                                                                                                   |
| REQ-006 | Debiting is idempotent with respect to the originating order and event type: the same cancellation/refund event arriving more than once must produce exactly one debit movement.                       | Replaying the same cancellation/refund event N times leaves the customer's balance and ledger identical to the state after a single delivery; no duplicate movement is appended.                                                                                           |
| REQ-007 | A debit must be allowed even if the customer's current balance is lower than the debit amount; the resulting balance may be negative.                                                                  | Given a balance of `5` and a debit of `12`, after applying the debit the balance is `-7` and the movement is recorded.                                                                                                                                                     |
| REQ-008 | A negative balance is a valid but observable state.                                                                                                                                                    | Customers with a negative balance appear in queries and movement history exactly like any other customer; the negativeness is not hidden. [NEEDS CLARIFICATION: is there a downstream signal/alert when a customer enters negative balance, or only the regular `balance updated` event?] |
| REQ-009 | The service exposes an operation to obtain a customer's current balance.                                                                                                                               | Given a customer identifier, the operation returns the customer's current point balance; the operation does not modify state.                                                                                                                                              |
| REQ-010 | The service exposes an operation to list a customer's movement history, ordered from most recent to oldest, with paging.                                                                               | Given a customer identifier and paging parameters, the operation returns a page of movements in reverse-chronological order; consecutive pages cover non-overlapping movements; the operation does not modify state.                                                       |
| REQ-011 | Each time the service appends a credit or debit movement, it publishes a `customer balance updated` business event.                                                                                    | A subscriber receives one `customer balance updated` event per appended movement, containing: customer identifier, the new balance after the movement, the delta applied (signed), and the cause (`order paid` or `order cancelled/refunded`).                             |
| REQ-012 | The published `customer balance updated` event is traceable back to the originating order event.                                                                                                       | The event payload includes the originating order identifier and the originating event type (`order paid` / `order cancelled` / `order refunded`).                                                                                                                          |
| REQ-013 | The ledger is append-only: existing movements cannot be modified or deleted.                                                                                                                           | No operation exposed by the service mutates or removes a previously recorded movement; corrections, if any, are expressed as new movements. [NEEDS CLARIFICATION: are corrections in scope for v1, or only via ops/back-fill outside the service contract?]                |
| REQ-014 | Every movement is traceable to the input business event that originated it.                                                                                                                            | A movement record carries: originating order identifier, originating event type, and the time the originating event was accepted by the service.                                                                                                                           |
| REQ-015 | The service is tolerant of duplicate input events (paid / cancelled / refunded). Duplicates do not produce duplicate movements (cf. REQ-003, REQ-006) and do not cause failures observable upstream.   | Replays of any input event leave the system in the same state as a single delivery and do not surface errors to the producer.                                                                                                                                              |

## 6. Out of scope

- **Loyalty point expiry / TTL** — _(reason: not mentioned by the user; assumed not required for v1; revisit when business defines an expiry policy)_.
- **Manual credit/debit by support agents** — _(reason: not mentioned; v1 is driven exclusively by order lifecycle events)_.
- **Multi-tier loyalty programmes (silver/gold/etc.)** — _(reason: not mentioned; the service maintains a single point balance per customer)_.
- **Cross-currency conversion** — _(reason: not mentioned; the rule "1 point per monetary unit" assumes a single currency upstream; mismatched currencies would be a separate contract)_.
- **Customer-facing UI / mobile push** — _(reason: this service publishes events; presentation layers are downstream subscribers' responsibility)_.
- **Authentication / authorisation policy for the read operations** — _(reason: not mentioned; assumed to follow the platform's existing service-call conventions; the PRD owns business intent, not access policy)_. [NEEDS CLARIFICATION: confirm read operations are internal-only or also exposed externally]
- **Retroactive back-fill of historical orders into the ledger at first deploy** — _(reason: not mentioned; assumed v1 starts from a zero ledger going forward)_.

## 7. Acceptance criteria (feature-level)

- [ ] All requirements REQ-001..REQ-015 are covered by automated acceptance scenarios.
- [ ] Replaying any input event N times produces the same ledger and balance as a single delivery (idempotency end-to-end).
- [ ] For every appended movement, exactly one `customer balance updated` event is observable by downstream subscribers, with cause and originating order traceability.
- [ ] A customer's read operations (`get balance`, `list movements`) return consistent results with the recorded movements and never alter state.
- [ ] No constitutional rule (`severity: must`) from the bundle's `CONSTITUTION.md` is violated by the implementation that satisfies this PRD.
- [ ] Negative balances are observable in `get balance` and never cause read or write operations to fail.

## 8. Open questions

| #   | Question                                                                                                                                       | Decider                | Due |
|-----|------------------------------------------------------------------------------------------------------------------------------------------------|------------------------|-----|
| Q1  | Should a zero-value credit (e.g. `0.50` floored to `0`) still append a movement of value `0`, or be skipped entirely? (REQ-002)                | product owner          | before `/sdd-plan` |
| Q2  | When a cancellation/refund arrives for an order with no prior credit, should we log/observe this case or silently ignore? (REQ-005)            | product owner          | before `/sdd-plan` |
| Q3  | Should entering a negative balance trigger a separate signal (alert / dedicated event) for ops, or is the regular `balance updated` enough? (REQ-008) | product owner    | before `/sdd-plan` |
| Q4  | Are manual corrections in scope for v1 (as new compensating movements), or strictly out of scope? (REQ-013)                                    | product owner          | before `/sdd-plan` |
| Q5  | Are read operations consumed only by other internal services, or also by an external/customer-facing channel? (out-of-scope auth note)         | product owner / sec    | before `/sdd-plan` |
| Q6  | What is the target lag between accepting an input event and emitting the corresponding `balance updated` event? (success metric)               | product owner          | before `/sdd-plan` |
| Q7  | What constitutes the *amount* of an order for the credit rule: gross total, net of taxes, net of shipping, or net of discounts? (REQ-002)      | product owner          | before `/sdd-plan` |
| Q8  | Is a "cancellation" and a "refund" the same business event from this service's point of view, or two distinct causes that must be distinguishable in the published event? (REQ-004, REQ-011) | product owner | before `/sdd-plan` |
| Q9  | If the same order is paid → cancelled → paid-again (re-paid), is the second payment a new credit (new order id assumption broken?), a no-op, or an error? | product owner | before `/sdd-plan` |
| Q10 | Stakeholder/owner of this PRD?                                                                                                                 | (to be named)          | before `/sdd-plan` |

## Definition of Done (PRD)

- [x] Every requirement has a stable ID (REQ-NNN). _(REQ-001..REQ-015)_
- [x] No technology mentioned (no Spring, Kafka, Postgres, REST, …). _(self-checked: no platform/protocol terms in sections 1-7)_
- [x] Every requirement has at least one acceptance criterion. _(each REQ row carries one observable acceptance)_
- [x] Pending clarifications marked with `[NEEDS CLARIFICATION: …]`. _(4 inline markers + 10 numbered open questions)_
- [x] Out-of-scope items explicitly listed with reason. _(7 items in §6)_
- [ ] Stakeholder/owner identified (in the open questions table). _(Q10 pending)_
