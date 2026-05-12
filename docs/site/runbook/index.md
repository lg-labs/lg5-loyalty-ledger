# Onboarding Runbook

Minimum viable runbook for a new contributor on the **loyalty-ledger** service.
Three sections: a first-day checklist, a tour of the repository, and one-click
links to the rest of the docs surface.

## (a) First-day setup

Sequential. Each step assumes the previous one succeeded.

1. **Clone the repo.**

   ```bash
   git clone git@github.com:lg-labs/loyalty-ledger.git
   cd loyalty-ledger
   git submodule update --init --recursive
   ```

   The `.agent-os/` directory is a git submodule pinned to the bundle tag
   (currently `v3.0.0`); without `--recursive` the SDD subagents and rules will
   be missing.

2. **Install JDK 21.** Any distribution of OpenJDK 21 works. If you use
   `asdf`, `~/.asdf/installs/java/openjdk-21.x.x` is the convention; if you
   use SDKMAN, `sdk install java 21.0.x-tem`. Verify with `java -version` —
   the major version must be `21`.

3. **Install pnpm.** Required for the docs site (Vitepress) and Allure
   tooling. `npm i -g pnpm` or `corepack enable && corepack prepare pnpm@latest --activate`.

4. **Build the service (skip tests for speed).**

   ```bash
   make install-skip-test
   ```

   This is the canonical Maven build target; it produces all module jars and
   the runnable container image inputs.

5. **Run the acceptance tests** to confirm the toolchain end-to-end.

   ```bash
   make run-acceptance-test
   ```

   This brings up the Testcontainers stack (Kafka, DB) and runs the Cucumber
   scenarios. Expect first run to be slow due to image pulls.

6. **Render the docs site locally.**

   ```bash
   make docs-install && make docs-preview-local
   ```

   Opens a local Vitepress dev server. Use this to preview any change you
   make to `docs/site/` before pushing.

## (b) Repo tour

One paragraph per top-level concern. Read top-down for orientation.

### `docs/`

Two distinct surfaces under one tree.
`docs/specs/` holds the **SDD specifications** — one subdirectory per
feature (`001-loyalty-ledger/`, `002-api-specifications/`,
`003-ci-cd-canonical/`, `004-project-docs/`, …). Each feature follows the
seven-phase Spec-Driven Development workflow shipped by `lg5-spring-agent-os`
(intent → prd → plan → adrs → tasks → implement → review). These are the
**source of truth** for what the system should do.
`docs/site/` is the **rendered docs surface** — a Vitepress site that
aggregates architecture, API, events, ADRs, runbook, and the live Allure
acceptance report. It is built and deployed by CI on every push to the
trunk; preview channels are produced for labelled PRs.

### `loyalty-ledger-*` Maven modules

Per **RULE-004** the service is split into the canonical lg5-spring
hexagonal/DDD layout:

- `loyalty-ledger-domain/` — `domain-core` (pure DDD, no Spring) +
  `application-service` (use cases, ports).
- `loyalty-ledger-api/` — REST controllers (`application/vnd.api.v1+json`).
- `loyalty-ledger-data-access/` — JPA adapters, outbox tables.
- `loyalty-ledger-message/` — `message-core` (Kafka producers/consumers) +
  `message-model` (Avro `.avsc` schemas).
- `loyalty-ledger-container/` — only place with `@SpringBootApplication`
  and `application.yaml`. Build target for the runnable image.
- `loyalty-ledger-acceptance-test/` — Cucumber + Testcontainers + Wiremock
  ATDD harness extending `Lg5TestBoot[PortNone]`.
- `loyalty-ledger-support/` — `docker-compose` for local Kafka/DB.

### `Makefile`

Single entry point for every workflow. Notable targets:
`install-skip-test`, `run-acceptance-test`, `run-integration-test`,
`docker-up` / `docker-down`, `run-apps`, `run-avro-model`,
`docs-install`, `docs-preview-local`, `docs-build-pages`,
`docs-build-firebase`, `docs-deploy-pages`. Always prefer the Make target
over invoking `mvn` / `pnpm` directly — the targets carry the canonical
flags.

### `.github/`

GitHub Actions workflows. Trunk pushes build the docs (Pages + Firebase
hosting) and publish the Allure report; labelled PRs (`docs/preview`) get
a Firebase preview channel with a bot comment carrying the URL. Fork PRs
are skipped for secret-safety.

### `.agent-os/`

Git submodule pinned to a tagged release of `lg5-spring-agent-os` (the
SDD bundle: subagents, rules, skills, commands). After a fresh clone run
`git submodule update --init --recursive`. Do not edit files inside the
submodule from this repo — bump the pin instead.

## (c) Other docs

Once your local dev loop is green, the rest of the docs surface is one
click away:

| Topic | Link |
| --- | --- |
| Architecture overview | [`/architecture/`](/architecture/) |
| API (synchronous, OpenAPI) | [`/api/`](/api/) |
| Events (asynchronous, AsyncAPI / Avro) | [`/events/`](/events/) |
| Architecture Decision Records | [`/adr/`](/adr/) |
| Acceptance Report (live Allure) | [lglabs-loyalty-allure.web.app](https://lglabs-loyalty-allure.web.app/) |
