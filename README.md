# lg5-loyalty-ledger

Microservice built on top of the
[`lg5-spring`](https://github.com/lg-labs-pentagon/lg5-spring) framework,
following the conventions packaged in
[`lg5-spring-agent-os`](https://github.com/lg-labs-pentagon/lg5-spring-agent-os).

> Status: **bootstrap** — feature `001-loyalty-ledger` in the **Specify** phase.

## Quick start

This repository pins the agent operating layer as a git submodule:

```bash
git clone --recurse-submodules git@github.com:lg-labs/lg5-loyalty-ledger.git
# or, if you've already cloned without --recurse-submodules:
git submodule update --init --recursive
```

The submodule lives at `.agent-os/` and is pinned to **`lg5-spring-agent-os@v0.3.0`**
(validated against `lg5-spring` SHA `cbb6783`).

## Repository layout (bootstrap)

```
lg5-loyalty-ledger/
├── AGENTS.md                            # consumer thin index → .agent-os/AGENTS.md
├── README.md                            # this file
├── .agent-os/                           # submodule, pinned to v0.3.0
└── docs/
    └── specs/
        └── 001-loyalty-ledger/          # first feature; SDD artifacts land here
            ├── prd.md
            ├── plan.md
            ├── tasks.md
            └── adr/
```

Once Build phase starts, scaffolded modules (`*-domain`, `*-api`, etc.)
will be added by the `/scaffold-service` command and a Maven multi-module
parent will be generated at the repo root.

## Spec-Driven Development

See [`AGENTS.md`](AGENTS.md) and
[`.agent-os/specs/README.md`](.agent-os/specs/README.md) for the full
workflow. The four phases are:

1. **`/sdd-specify`** — informal prompt → functional PRD (no technology).
2. **`/sdd-plan`** — PRD → technical plan + ADRs + data model.
3. **`/sdd-tasks`** — plan → atomic `TASK-NNN` with Given/When/Then AC.
4. **`/sdd-implement TASK-NNN`** — execute one task end-to-end (code +
   tests + commit). Loops until `tasks.md` is exhausted.

Approval gates are **between phases**, not between individual TASKs.

## License

TBD.
