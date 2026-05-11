# lg5-loyalty-ledger

Microservice built on top of the
[`lg5-spring`](https://github.com/lg-labs-pentagon/lg5-spring) framework,
following the conventions packaged in
[`lg5-spring-agent-os`](https://github.com/lg-labs-pentagon/lg5-spring-agent-os).

> Status: **active development** — features 001/002/003 closed and merged
> to `main`. Next up: `004-project-docs` (VitePress + GitHub Pages + Firebase).

## Quick start

This repository pins the agent operating layer as a git submodule:

```bash
git clone --recurse-submodules git@github.com:lg-labs/lg5-loyalty-ledger.git
# or, if you've already cloned without --recurse-submodules:
git submodule update --init --recursive

# Wire OpenCode symlinks (creates .opencode/ pointing into .agent-os/).
# Required after fresh clone or after bumping the submodule.
./.agent-os/scripts/install.sh
```

The submodule lives at `.agent-os/` and is pinned to **`lg5-spring-agent-os@v3.0.0`**
(validated against `lg5-spring` SHA `d0d754a`).

The `.opencode/` directory (symlinks) is gitignored; each developer
regenerates it locally via `install.sh`. The bundle artifacts
themselves (`skills/`, `commands/`, `subagents/`, `AGENTS.md`) live
inside `.agent-os/` and are the single source of truth.

## Repository layout (bootstrap)

```
lg5-loyalty-ledger/
├── AGENTS.md                            # consumer thin index → .opencode/AGENTS.md
├── README.md                            # this file
├── .agent-os/                           # submodule, pinned to v3.0.0
├── .opencode/                           # gitignored; symlinks → .agent-os/
└── docs/
    └── specs/
        ├── 001-loyalty-ledger/          # closed
        ├── 002-api-specifications/      # closed
        └── 003-ci-cd-canonical/         # closed
```

Once Build phase starts, scaffolded modules (`*-domain`, `*-api`, etc.)
will be added by the `/scaffold-service` command and a Maven multi-module
parent will be generated at the repo root.

## Spec-Driven Development

See [`AGENTS.md`](AGENTS.md) and
[`.agent-os/specs/README.md`](.agent-os/specs/README.md) for the full
workflow. As of bundle v3.0.0 the workflow has up to 7 phases (intent
is optional, verify is mandatory):

1. **`/sdd-intent`** *(optional)* — informal idea → one-page `intent.md`
   framing problem/users/outcome (no technology yet).
2. **`/sdd-specify`** — informal prompt → functional PRD.
3. **`/sdd-plan`** — PRD → architecture plan + ADRs.
4. **`/sdd-design`** — plan → `design.md` + `data-model.md` (concrete
   class signatures, REST contracts, Avro schemas, JPA model).
5. **`/sdd-tasks`** — design → atomic `TASK-NNN` with Given/When/Then AC.
6. **`/sdd-implement TASK-NNN`** — execute one task end-to-end (code +
   tests + commit).
7. **`/sdd-verify`** — gate: AC↔evidence matrix, constitutional
   spot-check. **Bloqueante.**

Approval gates are **between phases**, not between individual TASKs.
Specs closed under the previous 4-phase workflow (001/002/003) remain
valid; the 7-phase workflow applies to new features (004+).

## API Specs

The REST + Kafka surface of this service is documented spec-first
under [`docs/api/`](docs/api):

| File | Spec | Surface |
|---|---|---|
| [`docs/api/openapi.yaml`](docs/api/openapi.yaml) | OpenAPI 3.1.0 | `GET /loyalty/customers/{customerId}/balance`, `GET /loyalty/customers/{customerId}/movements` |
| [`docs/api/asyncapi.yaml`](docs/api/asyncapi.yaml) | AsyncAPI 2.6.0 | Inbound: `order-paid`, `order-cancelled`, `order-refunded`. Outbound: `customer-balance-updated` (Kafka key = `customerId`). |

### Lint

Both specs are linted with [Spectral](https://stoplight.io/open-source/spectral)
on every push and PR via the `API specs lint (Spectral)` CI job.
Run it locally with:

```bash
npx @stoplight/spectral-cli lint docs/api/openapi.yaml docs/api/asyncapi.yaml \
    --ruleset .spectral.yaml --fail-severity=warn
```

The ruleset extends `spectral:oas` + `spectral:asyncapi` and adds
repo-specific rules (every operation/channel must be documented;
every 2xx OpenAPI response must declare `application/vnd.api.v1+json`).

### Drift protection (REST contract test)

The REST integration tests (`CustomerBalanceControllerIT`,
`CustomerMovementsControllerIT`, `ErrorAdviceIT`) pipe every
RestAssured call through an `OpenApiContractFilter`
([source](lg5-loyalty-ledger-container/src/test/java/com/lg/platform/loyalty/container/api/contract/OpenApiContractFilter.java))
backed by `swagger-request-validator-restassured`. Any drift between
the controller's actual response and `docs/api/openapi.yaml` —
unexpected status code, wrong content type, missing/extra/typed-wrong
field — fails the IT with a precise pointer.

The validator is configured to ignore request-side validation (the
ITs deliberately send malformed inputs to exercise 4xx paths); only
the response contract is enforced.

The AsyncAPI side is **not** Java-contract-tested today — the
mature tooling for that (e.g. `microcks-testcontainers`) would be
over-engineered for a 4-channel surface. Spectral lint is the
guardrail there. The `.avsc` files remain the serialization source
of truth, gated by the existing Schema-Registry compatibility job.

## License

TBD.
