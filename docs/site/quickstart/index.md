# QuickStart

Use this page when you want to get productive quickly on
`lg5-loyalty-ledger` without reading the whole repository first.

The sequence below is intentionally practical:

1. clone the repo correctly
2. bootstrap the local toolchain
3. build the service
4. run the main verification commands
5. preview the docs locally
6. jump to the right deeper docs pages

## Prerequisites

Before you start, make sure your machine has:

- JDK 21
- Maven
- Docker / Docker Compose
- Node.js with `pnpm`
- Git with submodule support

If you use `asdf` or SDKMAN for Java, verify that `java -version` reports a
major version of `21`.

## Clone and initialize the repository

Clone with submodules from the start if possible:

```bash
git clone --recurse-submodules git@github.com:lg-labs/lg5-loyalty-ledger.git
cd lg5-loyalty-ledger
```

If you already cloned without submodules:

```bash
git submodule update --init --recursive
```

Then wire the local OpenCode symlinks:

```bash
./.agent-os/scripts/install.sh
```

This step creates the local `.opencode/` symlink structure that points into the
agent bundle shipped in `.agent-os/`.

## Build the service

The canonical fast build is:

```bash
make install-skip-test
```

Use this first to confirm that the multi-module Maven build resolves correctly.

If you want the full build including tests:

```bash
make install
```

## Run the main verification commands

Start from the commands that give the most signal for the least setup.

### Unit and integration-oriented verification

```bash
make run-unit-test
make run-integration-test
```

### Acceptance-style verification

```bash
make run-acceptance-test
```

This is the best single command when you want confidence that the service still
holds together end to end.

### Contract-related checks

If you are touching Avro contracts or schema registration behavior:

```bash
make run-avro-model
make check-schema-compat
```

If you are touching the REST or AsyncAPI specs directly, the repository also
ships the canonical specs under `docs/api/`.

## Bring up local infrastructure when needed

If your workflow requires local Kafka or Postgres outside the test harness:

```bash
make docker-up
```

And when you are done:

```bash
make docker-down
```

For the application itself:

```bash
make run-app
```

## Preview the docs locally

When you are working on `docs/site`, install the docs dependencies and start the
local preview server:

```bash
make docs-install
make docs-preview-local
```

Use the local preview to review navigation, page structure, and writing flow
before pushing docs changes.

## Know the main repository areas

If you only remember one mental map, use this one:

| Area | What it is for |
| --- | --- |
| `docs/specs/` | SDD feature artifacts and source-of-truth planning docs |
| `docs/site/` | Published documentation surface |
| `lg5-loyalty-ledger-domain/` | Domain core and application-service logic |
| `lg5-loyalty-ledger-api/` | Read-only REST surface |
| `lg5-loyalty-ledger-data-access/` | JPA, Liquibase, persistence adapters |
| `lg5-loyalty-ledger-message/` | Kafka listeners, publishers, Avro models |
| `lg5-loyalty-ledger-container/` | Spring Boot composition root |
| `lg5-loyalty-ledger-acceptance-test/` | Acceptance and container-backed tests |
| `lg5-loyalty-ledger-support/` | Local infra and supporting scripts |

## What to read next

Once your local bootstrap is working, continue in this order:

1. [Architecture overview](/architecture/)
2. [C4+1 Views](/architecture/c4-model)
3. [DDD](/architecture/ddd)
4. [REST](/architecture/rest)
5. [Events](/architecture/events)
6. [Onboarding Runbook](/runbook/)

If your focus is contract work, jump directly to:

- [API (sync)](/api/)
- [Events (async)](/events/)
