# Onboarding Runbook

Operational orientation for a new contributor on the **loyalty-ledger**
service. This page is intentionally narrow: it covers a first-day setup
checklist, a brief repository tour, and one-click links to the rest of the
docs surface.

For a step-by-step technical bootstrap (clone, toolchain, build, test, docs
loop) see the dedicated [**QuickStart**](/quickstart/). For deep technical
context see [**Architecture**](/architecture/). For recurring questions see
the [**FAQ**](/faq/).

## (a) First-day setup checklist

A short checklist suitable for printing or pinning. The [QuickStart](/quickstart/)
expands every step with rationale and troubleshooting.

1. Clone with submodules:
   `git clone <repo-url> && cd lg5-loyalty-ledger && git submodule update --init --recursive`.
   The `.agent-os/` submodule carries the SDD bundle (subagents, rules,
   skills, commands). A clone without `--recursive` will be missing it.
2. Install **JDK 21** (any OpenJDK 21 distribution). Verify with
   `java -version`.
3. Install **pnpm** (required for the docs site and Allure tooling).
4. Build the service skipping tests: `make install-skip-test`.
5. Run the acceptance tests end-to-end: `make run-acceptance-test`.
   First run is slow due to Testcontainers image pulls.
6. Render the docs site locally: `make docs-install && make docs-preview-local`.

If any step fails, jump to the [FAQ](/faq/) — the most common toolchain
problems are listed there. The detailed walk-through lives in the
[QuickStart](/quickstart/).

## (b) Repository tour

One paragraph per top-level concern. Read top-down for orientation; follow
the links for depth.

### `docs/`

Two distinct surfaces under one tree.

- `docs/specs/` — the **SDD specifications**, one subdirectory per feature
  (`001-loyalty-ledger/`, `002-api-specifications/`, `003-ci-cd-canonical/`,
  `004-project-docs/`, …). Each feature follows the seven-phase
  Spec-Driven Development workflow shipped by `lg5-spring-agent-os`
  (intent → prd → plan → adrs → design → tasks → implement → review).
  These documents are the **source of truth** for what the system should do.
- `docs/site/` — the **rendered docs surface** you are reading right now.
  A VitePress site that aggregates Architecture, QuickStart, FAQ, API,
  Events, ADRs, Runbook, and the live Allure acceptance report. It is
  built and deployed by CI on every push to the trunk; preview channels
  are produced for labelled PRs.
- `docs/api/openapi.yaml` and `docs/api/asyncapi.yaml` — the canonical
  REST and event contracts (the latter mirrors the Avro `.avsc` files
  under `lg5-loyalty-ledger-message-model`).

### `loyalty-ledger-*` Maven modules

The service is split into the canonical hexagonal/DDD layout. Each module
is summarised here; their roles, dependencies, and boundaries are explained
under [Architecture → DDD](/architecture/ddd) and [Architecture → C4+1](/architecture/c4-model).

- `loyalty-ledger-domain/` — `domain-core` (pure DDD, no Spring) and
  `application-service` (use cases, ports).
- `loyalty-ledger-api/` — REST controllers (`application/vnd.api.v1+json`).
- `loyalty-ledger-data-access/` — JPA adapters and outbox tables.
- `loyalty-ledger-message/` — `message-core` (Kafka producers/consumers)
  and `message-model` (Avro `.avsc` schemas, the wire-format source of truth).
- `loyalty-ledger-container/` — the only module with `@SpringBootApplication`
  and `application.yaml`. Build target for the runnable image.
- `loyalty-ledger-acceptance-test/` — Cucumber + Testcontainers + Wiremock
  ATDD harness extending `Lg5TestBoot[PortNone]`.
- `loyalty-ledger-support/` — `docker-compose` for local Kafka/DB.

### `Makefile`

Single entry point for every workflow. Always prefer a Make target over
invoking `mvn` or `pnpm` directly — the targets carry the canonical flags.
Notable targets:

- Build / test: `install-skip-test`, `run-acceptance-test`,
  `run-integration-test`.
- Local services: `docker-up`, `docker-down`, `run-apps`,
  `run-avro-model`.
- Docs: `docs-install`, `docs-preview-local`, `docs-build-pages`,
  `docs-build-firebase`, `docs-deploy-pages`.

### `.github/`

GitHub Actions workflows. Trunk pushes build the docs (Pages + Firebase
hosting) and publish the Allure acceptance report. PRs labelled
`docs/preview` get a Firebase preview channel with a bot comment carrying
the URL; fork PRs are intentionally skipped for secret safety.

### `.agent-os/`

Git submodule pinned to a tagged release of `lg5-spring-agent-os` (the SDD
bundle: subagents, rules, skills, commands). After a fresh clone run
`git submodule update --init --recursive`. Do not edit files inside the
submodule from this repo — bump the pin instead.

## (c) Where to go next

Once the local loop is green, the rest of the docs surface is one click away.

| Topic | Link |
| --- | --- |
| Step-by-step technical bootstrap | [QuickStart](/quickstart/) |
| Service purpose, boundaries, flows | [Architecture](/architecture/) |
| C4+1 views (context, container, dynamic) | [Architecture → C4+1](/architecture/c4-model) |
| DDD modules and ports/adapters | [Architecture → DDD](/architecture/ddd) |
| Read-only HTTP surface (OpenAPI) | [API (sync)](/api/) |
| Kafka surface (AsyncAPI / Avro) | [Events (async)](/events/) |
| Architectural Decision Records | [ADRs](/adr/) |
| Acceptance Report (live Allure) | [lglabs-loyalty-allure.web.app](https://lglabs-loyalty-allure.web.app/){target="_blank"} |
| Recurring contributor/reviewer questions | [FAQ](/faq/) |
