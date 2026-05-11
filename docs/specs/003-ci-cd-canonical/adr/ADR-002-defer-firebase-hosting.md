# ADR-002 — Defer Firebase Hosting of doc artifacts to feature 004

- **Status**: Proposed
- **Date**: 2026-05-10
- **Feature**: `003-ci-cd-canonical`
- **Decided by**: pending user approval

## Context

The canonical `c-integration.yml` template publishes documentation
sites (`openapi`, `asyncapi`, `allure`, `docs/mkdocs`) as **GitHub
Actions artifacts** — meaning they are accessible only by clicking
into a workflow run, retained 90 days, and not browsable as live
HTML.

The user requested public, durable hosting on **Firebase Hosting**
("publica cosas en firebase, entonces, me ayudas a configurar el
firebase, decicado para ello").

Investigation:

- Bundle v0.3.5 ships **no** skill for Firebase deploy. The
  `lg5-ci-cd-engineer` subagent explicitly lists "container delivery,
  Helm, GitOps, release automation, secrets, perf, quality-gates" as
  **out of scope** with named future skills (`lg5-container-delivery`
  etc.) — but Firebase is not even on that list.
- The canonical `build` job uploads a `firebase-json` artifact from a
  file `./firebase.json` that the template **assumes** exists at the
  repo root, but neither generates nor explains. This appears to be
  vestigial from an unfinished experiment in `blank-service`.
- `food-ordering-system` does ship a separate `c-delivery.yaml` that
  pushes Docker images to GHCR — but again, no Firebase.

## Decision

**Defer Firebase Hosting integration to a separate feature
`004-firebase-doc-hosting`.** Feature 003 ships the canonical
template intact, including the unmodified `firebase.json` upload
step (which will fail or upload an empty file — see Consequences).

Rationale:

1. **Scope discipline**. Feature 003 is "install canonical bundle".
   Firebase is a local extension. Mixing them in one PR makes the
   diff harder to review and contradicts the user's own decision
   ("1. Si dividir") in this thread.
2. **Pre-conditions for Firebase are non-trivial** and require
   user-side setup we don't have yet:
   - GCP / Firebase project creation.
   - Service account with `roles/firebasehosting.admin`.
   - JSON key → repo secret `FIREBASE_SERVICE_ACCOUNT`.
   - Multi-site config in `firebase.json` (one site per artifact:
     `/openapi`, `/asyncapi`, `/allure`, `/coverage`, `/mkdocs`).
   - Possibly billing-enabled GCP project (Hosting-only is free
     tier; Cloud Functions needed for some scenarios is not).
3. **RULE-018 compliance**. Building Firebase wiring inside
   feature 003 would be inventing a pattern not present in any
   canonical lg5-spring repo. ADR-explicit acknowledgement of "local
   extension beyond the bundle" is required, and that acknowledgement
   belongs in feature 004's own ADR — not buried in feature 003.

## Alternatives considered

1. **Inline Firebase deploy inside feature 003.**
   Rejected: violates separation; makes the PR review thread mix
   "did you correctly install the canonical bundle?" with "did you
   correctly configure Firebase Hosting for our artifacts?".
2. **Skip the `firebase-json` upload step from `build` entirely
   in feature 003.**
   Rejected: it is a literal byte from the canonical template;
   removing it would also count as a deviation requiring an ADR.
   Cheaper to leave it in (failing softly) and let feature 004 give
   it a real `firebase.json`.
3. **Use GitHub Pages instead of Firebase.**
   Rejected: user explicitly asked for Firebase. Pages also requires
   a single-site multiplexer (jekyll / mkdocs / hand-rolled) which
   pushes us into another design conversation.

## Consequences

- **Positive**: feature 003 has a clean, reviewable diff with one
  concern.
- **Positive**: feature 004 gets a focused thread to design the
  Firebase setup correctly (project naming, multi-site, deploy
  triggers, environment promotion).
- **Negative**: in feature 003 the `build` job's "Upload firebase.json
  as artifact" step will likely fail or upload nothing, since
  `./firebase.json` doesn't exist. We will use
  `if-no-files-found: warn` (or remove the step pre-merge if it
  fails the job) and document the workaround in the milestone
  report. Feature 004 will introduce the real file.
- **Negative**: the doc artifacts produced by feature 003 are not
  publicly browsable until feature 004 lands. Acceptable interim:
  download artifact + `python3 -m http.server 8765`.

## Follow-up commitments for feature 004

- Spec: `docs/specs/004-firebase-doc-hosting/`.
- Will include explicit ADR titled "ADR-001 — Local extension beyond
  bundle: justification and rollback plan".
- Will pre-list user-side setup steps (GCP project, service account,
  PAT) before delegating to any subagent.
- Will likely **not** depend on a future bundle skill — the user
  request is concrete and well-scoped, and waiting on upstream is
  not a good reason to defer indefinitely.

## Compliance

- RULE-018 — Ground in canonical sources: ✓ this ADR explicitly
  states Firebase is **not** in the bundle and defers rather than
  invents.
