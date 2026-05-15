# ADR-007 — New movement cause: `POINTS_EXPIRED`

## Context
Every movement must have a cause (REQ-002). Expiration is a new type of system-driven debit.

## Decision
Add `POINTS_EXPIRED` to the `loyalty_cause` Postgres ENUM and the domain `MovementCause` enum.

## Constitutional Impact
- **RULE-007 (Kafka/Avro):** The outbound Avro model for balance updates must include this new cause.

## Consequences
- **Positive:** Clear audit trail in `GET /movements`.
