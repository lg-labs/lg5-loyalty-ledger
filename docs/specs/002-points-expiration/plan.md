---
kind: plan
feature: 002-points-expiration
version: 0.1.0
status: drafted
---

# Plan: Points Expiration — `002-points-expiration`

> Generated from [`prd.md`](prd.md). This phase defines the architecture and module impact.

## 1. Architectural Overview

The expiration process will be a **scheduled background job** that queries the database for inactive customers and triggers a debit movement for each.

### 1.1 Module Impact
- `lg5-loyalty-ledger-domain-core`: Add `POINTS_EXPIRED` to `MovementCause`.
- `lg5-loyalty-ledger-data-access`: Update Liquibase DDL for the new ENUM value; add a repository method to find inactive customer IDs.
- `lg5-loyalty-ledger-application-service`: Create `ExpirationService` and `ExpirationScheduler`.
- `lg5-loyalty-ledger-message-model`: Update Avro schema for balance updates to include the new cause.

## 2. ADRs
- [ADR-006: Daily batch expiration strategy](adr/ADR-006-daily-batch-expiration.md)
- [ADR-007: New movement cause `POINTS_EXPIRED`](adr/ADR-007-new-expiration-cause.md)

## 3. Dependency Graph
```
TASK-021 (DDL/ENUM) ──► TASK-022 (Domain/Repo) ──► TASK-023 (Logic/Scheduler) ──► TASK-024 (ATDD)
```

## 4. Risks & Mitigations
- **Performance:** Scanning the entire `movement` table could be slow.
- **Mitigation:** Use the existing `idx_movement_customer_appended` and consider a specialized query or a new "last_activity" column in `customer_balance` if performance degrades.
