# Architectural Decision Records

This page is the entry point to the ADRs of `lg5-loyalty-ledger`.

In this repository, ADRs capture the architectural decisions that explain why a
 feature, integration, or documentation surface was shaped in a particular way.
The ADR itself is always the canonical decision text. This page adds navigation
and context so you can find the relevant decision quickly.

## How ADRs relate to specs

Most ADRs in this repository live under `docs/specs/<feature>/adr/`.

That means a decision is usually tied to a feature context, such as:

- the core loyalty-ledger service behavior
- the CI/CD topology that protects contracts and schemas
- the documentation surface and publishing model

Use the feature folder when you need full planning context. Use this page when
you need the decision index first.

## Featured decisions

If you are new to the repo, these are the best ADRs to read first:

- **ADR-001 / feature 001**: outbox-only emission, no saga participation in v1
- **ADR-003 / feature 001**: idempotency keyed by originating event id
- **ADR-004 / feature 001**: materialized balance projection for fast reads
- **ADR-001 / feature 004**: VitePress as the docs site engine
- **ADR-002 / feature 004**: dual deploy to GitHub Pages and Firebase Hosting

Together, those decisions explain most of the service's core shape:

- how write-side event handling works
- how reads stay fast without abandoning the immutable ledger
- how outbound events are published safely
- how the docs surface is built and served

## ADR groups

### Feature 001 — Loyalty Ledger core behavior

- [ADR-001 — Outbox-only emission, no saga participation in v1](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/001-loyalty-ledger/adr/ADR-001-outbox-only-no-saga.md)
- [ADR-002 — Reuse order message model](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/001-loyalty-ledger/adr/ADR-002-reuse-order-message-model.md)
- [ADR-003 — Idempotency by originating event id](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/001-loyalty-ledger/adr/ADR-003-idempotency-by-event-id.md)
- [ADR-004 — Materialized balance projection](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/001-loyalty-ledger/adr/ADR-004-materialized-balance-projection.md)
- [ADR-005 — Cause enum on outbound Avro event](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/001-loyalty-ledger/adr/ADR-005-cause-enum-avro.md)

### Feature 003 — CI/CD canonicalization

- [ADR-001 — Preserve Spectral and schema jobs](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/003-ci-cd-canonical/adr/ADR-001-preserve-spectral-and-schema-jobs.md)
- [ADR-002 — Defer Firebase hosting](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/003-ci-cd-canonical/adr/ADR-002-defer-firebase-hosting.md)
- [ADR-003 — Defer MkDocs to feature 004](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/003-ci-cd-canonical/adr/ADR-003-defer-mkdocs-to-feature-004.md)

### Feature 004 — Documentation surface

- [ADR-001 — Use VitePress as the documentation site engine](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/004-project-docs/adr/ADR-001-vitepress-site-engine.md)
- [ADR-002 — Dual deploy to Pages and Firebase](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/004-project-docs/adr/ADR-002-dual-deploy-pages-and-firebase.md)
- [ADR-003 — Allure on a separate Firebase site](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/004-project-docs/adr/ADR-003-allure-separate-firebase-site.md)
- [ADR-004 — Dual base-path build](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/004-project-docs/adr/ADR-004-dual-base-path-build.md)
- [ADR-005 — Preview channels are label-gated](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/004-project-docs/adr/ADR-005-preview-channels-label-gated.md)
- [ADR-006 — Upstream the VitePress docs skill](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/004-project-docs/adr/ADR-006-upstream-vitepress-skill.md)

## ADR table

| ADR ID | Feature | Title | Status | Summary | Source |
| --- | --- | --- | --- | --- | --- |
| ADR-001 | `001-loyalty-ledger` | Outbox-only emission, no saga participation in v1 | Accepted | Uses Transactional Outbox for `customer-balance-updated` and explicitly avoids saga machinery in v1. | [Open](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/001-loyalty-ledger/adr/ADR-001-outbox-only-no-saga.md) |
| ADR-002 | `001-loyalty-ledger` | Reuse order message model | Accepted | Documents the inbound-contract ownership boundary and how the service relates to upstream order event schemas. | [Open](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/001-loyalty-ledger/adr/ADR-002-reuse-order-message-model.md) |
| ADR-003 | `001-loyalty-ledger` | Idempotency by originating event id | Accepted | Keys deduplication on event identity so replay and paid-again sequences remain distinguishable. | [Open](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/001-loyalty-ledger/adr/ADR-003-idempotency-by-event-id.md) |
| ADR-004 | `001-loyalty-ledger` | Materialized balance projection | Accepted | Keeps a current-balance projection alongside the immutable ledger so reads stay fast. | [Open](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/001-loyalty-ledger/adr/ADR-004-materialized-balance-projection.md) |
| ADR-005 | `001-loyalty-ledger` | Cause enum on outbound Avro event | Accepted | Defines the outbound cause enum, including `UNKNOWN` for forward compatibility. | [Open](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/001-loyalty-ledger/adr/ADR-005-cause-enum-avro.md) |
| ADR-001 | `003-ci-cd-canonical` | Preserve Spectral and schema jobs | Proposed | Keeps contract and schema gates alongside the canonical CI topology instead of discarding them. | [Open](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/003-ci-cd-canonical/adr/ADR-001-preserve-spectral-and-schema-jobs.md) |
| ADR-002 | `003-ci-cd-canonical` | Defer Firebase hosting | Proposed | Defers Firebase hosting decisions to the dedicated docs feature instead of forcing them into CI canonicalization. | [Open](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/003-ci-cd-canonical/adr/ADR-002-defer-firebase-hosting.md) |
| ADR-003 | `003-ci-cd-canonical` | Defer MkDocs to feature 004 | Proposed | Records that the docs site engine decision belongs to the later documentation feature. | [Open](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/003-ci-cd-canonical/adr/ADR-003-defer-mkdocs-to-feature-004.md) |
| ADR-001 | `004-project-docs` | Use VitePress as the documentation site engine | Accepted | Chooses VitePress as the static docs engine for the loyalty-ledger docs surface. | [Open](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/004-project-docs/adr/ADR-001-vitepress-site-engine.md) |
| ADR-002 | `004-project-docs` | Dual deploy to Pages and Firebase | Accepted | Publishes the docs surface to both GitHub Pages and Firebase Hosting. | [Open](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/004-project-docs/adr/ADR-002-dual-deploy-pages-and-firebase.md) |
| ADR-003 | `004-project-docs` | Allure on a separate Firebase site | Accepted | Keeps Allure as a separate deployed surface instead of embedding it inside the docs site. | [Open](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/004-project-docs/adr/ADR-003-allure-separate-firebase-site.md) |
| ADR-004 | `004-project-docs` | Dual base-path build | Accepted | Builds the site twice so the same content can serve correctly from Pages and Firebase. | [Open](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/004-project-docs/adr/ADR-004-dual-base-path-build.md) |
| ADR-005 | `004-project-docs` | Preview channels are label-gated | Accepted | Limits preview creation to explicitly labeled PRs and sets a bounded lifecycle for them. | [Open](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/004-project-docs/adr/ADR-005-preview-channels-label-gated.md) |
| ADR-006 | `004-project-docs` | Upstream the VitePress docs skill | Accepted | Records the intention to upstream the documentation pattern into the agent bundle. | [Open](https://github.com/lg-labs/lg5-loyalty-ledger/blob/main/docs/specs/004-project-docs/adr/ADR-006-upstream-vitepress-skill.md) |

## Read next

- [Architecture overview](/architecture/)
- [DDD](/architecture/ddd)
- [REST](/architecture/rest)
- [Events](/architecture/events)
