# AGENTS.md — lg5-loyalty-ledger

Microservice built on top of [`lg5-spring`](https://github.com/lg-labs-pentagon/lg5-spring) framework.

## Agent operating layer

This repository consumes the **`lg5-spring-agent-os`** bundle as a git
submodule pinned at a SemVer tag, mounted at `.agent-os/`.

```
.agent-os/                       # submodule → lg5-spring-agent-os@v0.3.0
├── AGENTS.md                    # upstream template (skill routing, command catalog, constitution)
├── rules/                       # 18 always-active rules; CONSTITUTION.md indexes the 15 must-rules
├── skills/                      # 7 thematic skills (load on demand)
├── commands/                    # 8 commands: 4 SDD orchestrators + 4 building-blocks
├── subagents/                   # 3 specialized subagents
└── specs/                       # SDD templates + worked examples
```

**Read [`.agent-os/AGENTS.md`](.agent-os/AGENTS.md)
first** — it carries the full skill routing table, the command catalog,
the rule cheat sheet, and the constitution callout. The rest of this file
is service-specific overlay.

## Service-specific context

- **Service name:** `lg5-loyalty-ledger`
- **Pinned bundle version:** `v0.3.0` (lg5-spring SHA `cbb6783`)
- **Stack:** Spring Boot 3.4.2, JDK 21, Maven (per RULE-001).
- **Module shape:** mirrors `blank-service` (per RULE-004). Modules will be
  scaffolded by `/scaffold-service` from inside `/sdd-implement` during Build.

## Spec-Driven Development workflow

This repo follows the bundle's spec-anchored SDD workflow:

```
/sdd-specify  →  /sdd-plan  →  /sdd-tasks  →  /sdd-implement (loop)
   prd.md       plan.md         tasks.md      code + tests + commit
                + adr/          (TASK-NNN)
                + data-model.md
   ── HUMAN ──► ── HUMAN ──► ── HUMAN ──►
   APPROVES     APPROVES     APPROVES
```

Per-feature artifacts live at `docs/specs/<NNN-slug>/`. Approval gates are
**between phases**, not between individual TASKs inside Build.

Active features:

| ID  | Slug              | Status   | Folder                                            |
|-----|-------------------|----------|---------------------------------------------------|
| 001 | loyalty-ledger    | Specify  | [`docs/specs/001-loyalty-ledger/`](docs/specs/001-loyalty-ledger/) |

## Branching & commits

- Feature work happens on `feature/<NNN-slug>` branches.
- Commits use Conventional Commits with the task ID:
  `feat(TASK-NNN): <title>`.
- One commit per `/sdd-implement TASK-NNN` invocation.
- PRs require review + CI green + linear history (branch protection on `main`).

## Hard rules (do not violate without an ADR)

The 15 constitutional rules from
[`.agent-os/rules/CONSTITUTION.md`](.agent-os/rules/CONSTITUTION.md) bind
every change. Overrides require a dedicated ADR under
`docs/specs/<NNN-slug>/adr/` justifying the deviation and time-boxing it.

## Reference repos (clone if missing under `/tmp/lg5-study/`)

- Framework: https://github.com/lg-labs-pentagon/lg5-spring
- Real example: https://github.com/lg-labs/food-ordering-system
- Skeleton: https://github.com/lg-labs/blank-service
