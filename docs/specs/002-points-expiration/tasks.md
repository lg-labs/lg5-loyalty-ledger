---
kind: tasks
feature: 002-points-expiration
version: 0.1.0
description: Atomic tasks for implementing points expiration.
---

# Tasks — `002-points-expiration`

## TASK-021 — DDL: Add `POINTS_EXPIRED` cause to Liquibase and Avro
- **Status:** todo
- **References:** REQ-002, ADR-007, RULE-007
- **Modules touched:** `data-access`, `message-model`
- **Acceptance:**
  - **Given** the `loyalty_cause` ENUM and `BalanceUpdateCause.avsc`
  - **When** a new Liquibase migration runs
  - **Then** the DB allows `POINTS_EXPIRED` as a cause, and `make run-avro-model` generates the updated Java classes.

## TASK-022 — Repo: Find inactive customers
- **Status:** todo
- **References:** REQ-001, ADR-006
- **Modules touched:** `data-access`
- **Acceptance:**
  - **Given** customers with movements older than 12 months and others with recent movements
  - **When** the repo method `findCustomersInactiveSince(DateTime)` is called
  - **Then** it returns only the IDs of the inactive customers.

## TASK-023 — Service: Expiration logic + Scheduler
- **Status:** todo
- **References:** REQ-002, REQ-003, RULE-011, ADR-006
- **Modules touched:** `application-service`
- **Acceptance:**
  - **Given** an inactive customer with balance > 0
  - **When** the scheduler fires
  - **Then** a `DEBIT` movement of type `POINTS_EXPIRED` is created, balance becomes 0, and an outbox message is registered.

## TASK-024 — ATDD: Expiration scenarios
- **Status:** todo
- **References:** REQ-001..REQ-004, RULE-012
- **Modules touched:** `acceptance-test`
- **Acceptance:**
  - **Given** a scenario "Customer points expire after 12 months"
  - **When** the system time is advanced (or test data is backdated) and the process runs
  - **Then** the full flow is verified end-to-end.
