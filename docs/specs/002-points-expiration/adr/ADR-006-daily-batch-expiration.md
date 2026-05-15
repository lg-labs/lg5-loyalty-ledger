# ADR-006 — Daily batch expiration strategy

## Context
We need to expire points after 12 months of inactivity. Checking this for every customer on every request is inefficient.

## Decision
We will implement a daily batch process (Scheduler) that identifies inactive customers and applies the expiration.

## Constitutional Impact
- **RULE-011 (Outbox Scheduler):** We will follow the same pattern as the outbox relay (scheduling enabled/disabled via property).
- **RULE-003 (Architecture):** The logic will reside in `application-service`.

## Consequences
- **Positive:** Low impact on real-time ingestion.
- **Negative:** Points might expire up to 24 hours after the exact 12-month mark (acceptable per PRD).
