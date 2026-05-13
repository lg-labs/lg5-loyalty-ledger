---
kind: spec
name: design
feature: 004-project-docs
version: 0.2.0
description: Detailed design for the loyalty-ledger documentation surface, including both docs-site infrastructure and mature technical documentation content.
---

# Design — `004-project-docs`

> Generated from [`prd.md`](prd.md) + [`plan.md`](plan.md) + [`adr/`](adr/).
> This design pins both the existing non-Java configuration surface and the
> new page-level editorial structure for the technical-content wave of
> `004-project-docs`.

## 1. Scope and boundaries

The previously delivered VitePress / Firebase / Pages infrastructure remains
valid. This refresh adds the page-level structure, editorial rules, source
mapping, navigation, cross-linking, and bounded diagram policy for the new
technical-content wave.

This extension covers:

- architecture overview content
- QuickStart content
- FAQ content
- ADR landing page content
- service-specific pages for DDD, REST, events, and C4+1 views
- refresh of `index.md`, `api/index.md`, `events/index.md`, and `runbook/index.md`

Out of scope for this refresh:

- production code changes
- runtime behavior changes
- OpenAPI or AsyncAPI contract redesign
- ADR content rewrites
- CI topology changes beyond navigation/content wiring already supported by the existing site

## 2. Editorial rules

- Every page must be specific to `loyalty-ledger`.
- `lg5-spring` provides conceptual vocabulary and ecosystem framing.
- `blank-service` may be consulted only as supporting context to enrich understanding of `lg5-spring`; it is not a normative definition of `loyalty-ledger`.
- Repository-local sources of truth take precedence whenever there is ambiguity.
- Pages must not copy raw spec or contract content unless they also add reader-oriented explanation.
- The site must distinguish clearly between overview pages, how-to pages, reference pages, and FAQ content.
- English only.

## 3. Page-to-source mapping

| Page | Primary sources |
|---|---|
| `index.md` | `README.md`, `docs/specs/001-loyalty-ledger/prd.md`, current docs-site structure |
| `quickstart/index.md` | `README.md`, `Makefile`, current runbook, actual repository bootstrap commands |
| `architecture/index.md` | `docs/specs/001-loyalty-ledger/prd.md`, `plan.md`, `data-model.md`, real module structure |
| `architecture/c4-model.md` | `docs/specs/001-loyalty-ledger/plan.md`, module layout, inbound/outbound contracts |
| `architecture/ddd.md` | `docs/specs/001-loyalty-ledger/plan.md`, `data-model.md`, domain/application/data/message modules |
| `architecture/rest.md` | `docs/api/openapi.yaml`, API-related tests, `docs/specs/001-loyalty-ledger/prd.md` |
| `architecture/events.md` | `docs/api/asyncapi.yaml`, ADRs, outbox/event flow from feature 001 |
| `api/index.md` | `docs/api/openapi.yaml` |
| `events/index.md` | `docs/api/asyncapi.yaml` |
| `adr/index.md` | `docs/specs/**/adr/*.md` |
| `faq/index.md` | all of the above, curated into recurring questions |
| `runbook/index.md` | current runbook, `README.md`, `Makefile`, repo layout |

## 4. Navigation structure

Top navigation should expose these entries:

- Architecture
- QuickStart
- API (sync)
- Events (async)
- ADRs
- FAQ
- Acceptance Report
- Runbook

Sidebar structure should expand `Architecture` into:

- Overview
- C4+1 Views
- DDD
- REST
- Events

## 5. Page structure definitions

### `index.md`

Sections:

- what this site is for
- who it is for
- start here
- core sections
- source-of-truth note

### `quickstart/index.md`

Sections:

- prerequisites
- clone and initialize
- build
- run tests
- preview docs locally
- recommended next reads

### `architecture/index.md`

Sections:

- service purpose
- responsibilities and non-goals
- system role
- high-level write flow
- high-level read flow
- links to deep dives

### `architecture/c4-model.md`

Sections:

- system context
- container/module view
- dynamic write flow
- dynamic read flow
- reading notes

### `architecture/ddd.md`

Sections:

- bounded context
- domain core
- application service
- ports and adapters
- module mapping
- why this separation matters here

### `architecture/rest.md`

Sections:

- intent of the HTTP surface
- resources and endpoints
- media type and response shape
- pagination
- error handling
- relation to OpenAPI

### `architecture/events.md`

Sections:

- inbound events
- outbound events
- idempotency and deduplication
- outbox role
- traceability model
- relation to AsyncAPI

### `api/index.md`

Sections:

- what the OpenAPI contract covers
- open viewer link
- raw spec link
- how to use this contract
- related architecture page

### `events/index.md`

Sections:

- what the AsyncAPI contract covers
- open viewer link
- raw spec link
- how to use this contract
- related architecture page

### `adr/index.md`

Sections:

- what ADRs mean in this repo
- how ADRs relate to specs
- featured decisions
- grouped ADR index
- compact ADR table

### `faq/index.md`

Sections:

- setup
- docs navigation
- architecture
- contracts
- workflow / SDD questions

### `runbook/index.md`

Sections:

- first-day checklist
- repository tour
- operational/doc links
- escalation / where to look next

## 6. ADR landing page model

`docs/site/adr/index.md` follows a hybrid pattern:

- curated prose at the top
- grouped sections by feature or topic
- a compact table at the bottom

Recommended table columns:

- ADR ID
- Feature
- Title
- Status
- Summary
- Link

Canonical truth remains the underlying ADR markdown file, not the summary row.

## 7. Mermaid policy

Required diagrams:

- one system-context diagram
- one module/container diagram
- one write-path dynamic flow
- one read-path dynamic flow

Diagram requirements:

- use Mermaid
- optimize for readability over exhaustiveness
- reflect `loyalty-ledger` real flows, not generic framework abstractions
- keep labels short
- include a short explanatory paragraph below each diagram

## 8. Cross-linking rules

- `index.md` links to every major section
- `quickstart/index.md` links to Architecture, Runbook, API, and Events
- `architecture/index.md` links to all architecture subpages
- `architecture/rest.md` links to `/api/`
- `architecture/events.md` links to `/events/`
- `adr/index.md` links to source ADR files
- `faq/index.md` links outward to deeper pages instead of duplicating them
- pages should prefer internal site links over raw repository paths when a docs-site page exists

## 9. Content quality rules

- avoid placeholder-style wording in primary pages
- avoid generic lg5-spring theory unless immediately tied back to `loyalty-ledger`
- prefer explanation of "why this service is shaped this way"
- keep paragraphs short and scannable
- use tables only where they improve comparison or navigation
- code snippets only when needed for onboarding or contract interpretation

## 10. Validation expectations

The content refresh is considered design-complete when:

- every primary page has a defined structure
- every page has an explicit primary source mapping
- Mermaid scope is defined and bounded
- the ADR page strategy is explicit
- navigation additions are fully specified
- overlap between QuickStart, FAQ, Architecture, and Runbook is intentionally partitioned

## 11. Existing infrastructure decisions kept intact

The following previous design decisions remain valid and are intentionally not
reopened by this refresh:

- Node tooling lives under `docs/site/`
- VitePress remains the site engine
- Pages + Firebase dual deploy remains in place
- Allure remains a separate cross-linked site
- preview channels remain label-gated
- placeholder and broken-link warn-not-fail behavior remains in place

## Definition of Done (Design refresh)

- [x] Every newly added PRD requirement maps to at least one design section.
- [x] Every target page has a defined structure.
- [x] Every page has explicit source mapping.
- [x] Navigation updates are fully specified.
- [x] ADR landing-page behavior is explicit.
- [x] Mermaid policy is bounded and practical.
- [x] Overlap between QuickStart, Runbook, Architecture, and FAQ is intentionally partitioned.
