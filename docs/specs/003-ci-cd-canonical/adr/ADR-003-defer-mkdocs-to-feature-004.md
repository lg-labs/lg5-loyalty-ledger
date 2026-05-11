# ADR-003 — Defer MkDocs `docs` job to feature 004 (VitePress migration)

- **Status**: Accepted
- **Date**: 2026-05-10
- **Feature**: `003-ci-cd-canonical`
- **Decided by**: user (msg 2026-05-10, option 1 of 4)

## Context

The canonical `c-integration.yml` template shipped by `lg5-spring-agent-os`
v0.3.5 includes a `docs` job that runs `mkdocs build` against
`./<svc>-support/app/mkdocs.yml`. The job:

- Installs `mkdocs-material`, `pillow`, `cairosvg`.
- Downloads `dependency-graph` (from `build`) and `gource` (from
  `visualization`) artifacts into `./<svc>-support/app/docs/{img,video}`.
- Runs `cd ./<svc>-support/app && mkdocs build`.
- Uploads the resulting `./site/` as artifact `mkdocs`.

This service has neither `lg5-loyalty-ledger-support/app/` nor
`mkdocs.yml`. The job fails on the first PR run with:

```
Error: Config file 'mkdocs.yml' does not exist.
##[error]Process completed with exit code 1.
```

(See run `25634364684` job `75244725259` on PR #4.)

Independently, the user has already decided (this same session, just
before this ADR was written) that **feature 004 will publish project
docs using VitePress**, not MkDocs, with dual deployment to GitHub
Pages and Firebase Hosting (project `lglabs-loyalty`, two sites:
`lglabs-loyalty-docs` for VitePress + `lglabs-loyalty-allure` for the
Allure report). MkDocs is therefore obsolete for this repo before it
ever runs once.

## Decision

**Remove the `docs` job from `c-integration.yml` in feature 003.**
Feature 004 will reintroduce a redesigned job (or job set:
`docs-build` + `pages-deploy` + `firebase-deploy`) that builds
VitePress and deploys to both targets.

The four upstream artifacts that the deleted job consumed
(`dependency-graph`, `gource`) are **not** removed from `build` and
`visualization`: feature 004 will consume them inside the VitePress
site under `architecture/` (dependency graph as image) and
`history/` (gource video).

## Alternatives considered

1. **Skip the job conditionally** (`if: false` or
   `if-no-files-found: warn`). Rejected: leaves dead YAML in the
   workflow that confuses future readers and triggers actionlint
   warnings. Cleaner to delete and reintroduce.
2. **Create a placeholder `mkdocs.yml` + minimal `app/docs/`
   structure** so the job passes in feature 003 and gets deleted by
   feature 004. Rejected: pure throwaway work; the placeholder would
   confuse anyone exploring the repo between feature 003 merge and
   feature 004 merge.
3. **Adelantar VitePress to feature 003.** Rejected: violates the
   user-stated separation of concerns (003 = CI canonical only,
   004 = project docs). Mixes two large reviews into one PR.
4. **`continue-on-error: true` on the failing job.** Rejected:
   green-washing. The job remains visibly red in the PR check list,
   degrading signal-to-noise on real failures.

## Consequences

- **Positive**: feature 003 closes with all 12 remaining jobs green
  and a clean diff.
- **Positive**: feature 004 has full ownership of the docs-publishing
  story (tool choice, dual targets, preview channels) without
  inheriting MkDocs cruft.
- **Positive**: `dependency-graph` and `gource` artifacts remain
  produced by feature 003, ready for feature 004 to consume — no
  upstream rework needed.
- **Negative**: the canonical 11-job topology is reduced to 10 jobs
  in this service until feature 004 lands. Documented as a known
  deviation in M1 report.
- **Negative**: anyone diffing `c-integration.yml` against the
  upstream template will see the deletion. A NOTE block is left in
  place of the job pointing to this ADR and to feature 004.

## Compliance

- **RULE-018** (Ground in canonical sources): ✓ this ADR explicitly
  records a deliberate deviation from the canonical template, with
  explicit justification and a forward commitment.
- **Pairs with ADR-002** (defer Firebase Hosting): same pattern —
  scope discipline by deferring everything doc-publishing to
  feature 004, where the user-side pre-conditions (GCP, Firebase
  sites, service account, Pages enablement, label) are already in
  place per session confirmation 2026-05-10.

## Follow-up commitment for feature 004

- Reintroduce a job (or jobs) at the same DAG position
  (`needs: [ test, visualization ]`) that:
  - Installs Node + pnpm/npm.
  - Builds VitePress twice with different `base` values
    (`/lg5-loyalty-ledger/` for Pages, `/` for Firebase).
  - Includes the Swagger UI / AsyncAPI Studio HTML wrappers under
    `api/` inside the VitePress dist.
  - Embeds the `dependency-graph.png` and `gource.mp4` artifacts in
    the appropriate VitePress sections.
- Add `pages-deploy` job (gated `if: github.ref == 'refs/heads/main'`).
- Add `firebase-deploy` job (gated `if: github.ref == 'refs/heads/main'`)
  publishing to Firebase site `lglabs-loyalty-docs` (live channel).
- Add `firebase-deploy-allure` job (gated same) publishing the Allure
  artifact to Firebase site `lglabs-loyalty-allure` (live channel,
  overwrite each `main`).
- Add `firebase-preview` job gated by PR label `docs/preview`,
  deploying to channel `pr-<N>` (7-day TTL) on site
  `lglabs-loyalty-docs` only (no Pages preview, no Allure preview).
