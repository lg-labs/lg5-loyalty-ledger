# `docs/site/` — loyalty-ledger documentation site

VitePress site that aggregates Architecture, QuickStart, FAQ, API, Events,
ADRs, Runbook and the live Allure acceptance report for the
`lg5-loyalty-ledger` service.

Source layout, deploy topology and editorial rules are defined in
[`docs/specs/004-project-docs/`](../specs/004-project-docs/).

## Prerequisites

- Node ≥ 20 (only needed indirectly via `pnpm`).
- [`pnpm`](https://pnpm.io/) ≥ 9 — required (the lockfile is a pnpm lockfile).
  Install via `npm i -g pnpm` or `corepack enable && corepack prepare pnpm@latest --activate`.

All other tooling (VitePress, linkinator, firebase-tools) is local to this
directory and installed by `make docs-install`.

## Local preview workflows

All commands are run **from the repository root** through the canonical
`Makefile` (which `cd`s into `docs/site/` for you). Always prefer the Make
target over invoking `pnpm` directly — the targets carry the canonical flags
and the `DOCS_BASE` environment variable.

### 1. Hot-reload dev server (recommended while editing)

```bash
make docs-install        # one-time per checkout (resolves pnpm deps)
make docs-preview-local  # starts VitePress dev server on http://localhost:5173/
```

Edits to any `.md` under `docs/site/` are reflected in the browser without a
restart. The base path is the default `/`.

### 2. Production-like preview (recommended to sanity-check the build)

```bash
make docs-preview-built  # builds dist + serves it on http://localhost:4173/
```

This target:

1. Builds the production bundle with `DOCS_BASE='/'` (same flags as the
   Firebase Hosting deploy).
2. Serves `.vitepress/dist/` statically via `vitepress preview` on
   `http://localhost:4173/`.

Use this instead of `docs-preview-local` whenever you want to verify what the
live site will actually look like — including production-only behaviour such
as `cleanUrls`, the local-search index, and the source-state footer.

> **Note on the source-state footer.** Locally the footer renders
> `Built from dev · <timestamp>` because `COMMIT_SHA` / `BUILD_TIME` /
> `PR_NUMBER` are not injected outside CI. That is expected.

### 3. Build-only targets (no preview)

```bash
make docs-build-firebase  # DOCS_BASE='/'                       (Firebase target)
make docs-build-pages     # DOCS_BASE='/lg5-loyalty-ledger/'    (GitHub Pages target)
```

Both produce `.vitepress/dist/`. Useful when you only need the static output
(for example to inspect a single generated HTML page).

## CI-produced artifacts (placeholders locally)

Four artifacts are produced by upstream CI jobs and downloaded into
`docs/site/` before the production build:

| Path (under `docs/site/`)        | Producer                                  |
|----------------------------------|-------------------------------------------|
| `public/dependency-graph.png`    | feature-002 dependency-graph job           |
| `public/gource.mp4`              | feature-002 repo-activity job              |
| `api/swagger-ui.html`            | feature-003 OpenAPI viewer job             |
| `events/asyncapi.html`           | feature-003 AsyncAPI viewer job            |

Locally these files are absent. `scripts/check-artifacts.mjs` runs before
every build and writes a placeholder `_placeholder.md` for each missing
artifact, emitting one `::warning::` line per missing item. The build never
fails on a missing artifact (this is the documented warn-don't-fail policy
from [`design.md` §7.8](../specs/004-project-docs/design.md)).

If you see `::warning file=public/...::` lines during `make docs-build-*`,
that is expected locally.

## Directory layout

```
docs/site/
├── .vitepress/
│   ├── config.ts                 # site config (nav, sidebar, vite.define)
│   └── theme/                    # default-theme extension + source-state footer
├── architecture/                 # Architecture (index, c4-model, ddd, rest, events)
├── api/                          # Synchronous contract landing + Swagger UI host
├── events/                       # Asynchronous contract landing + AsyncAPI host
├── adr/                          # Hybrid ADR landing page
├── faq/                          # Contributor / reviewer FAQ
├── quickstart/                   # Step-by-step technical bootstrap
├── runbook/                      # Onboarding runbook
├── public/                       # Static assets (dependency graph, gource, etc.)
├── scripts/
│   ├── check-artifacts.mjs       # CI-artifact presence check (warn-don't-fail)
│   └── linkinator-to-annotations.mjs   # link-check wrapper (warn-don't-fail)
├── package.json
└── pnpm-lock.yaml
```

## Related Make targets

| Target                  | Purpose                                           |
|-------------------------|---------------------------------------------------|
| `make docs-install`     | Install pnpm deps (frozen lockfile)               |
| `make docs-preview-local` | Hot-reload dev server (`http://localhost:5173/`)|
| `make docs-preview-built` | Build + serve dist on `http://localhost:4173/`  |
| `make docs-build-firebase` | Build for Firebase Hosting (`DOCS_BASE='/'`)   |
| `make docs-build-pages`    | Build for GitHub Pages (base `/lg5-loyalty-ledger/`) |
| `make docs-deploy-firebase` | Deploy to Firebase Hosting (requires service account) |
| `make docs-deploy-pages`    | Pages deploy is CI-only (no-op locally)        |

## See also

- [Feature 004 spec set](../specs/004-project-docs/) — PRD / Plan / Design / Tasks.
- [`.github/workflows/c-integration.yml`](../../.github/workflows/c-integration.yml) — CI build + deploy + preview jobs.
- [VitePress docs](https://vitepress.dev/) — upstream framework.
