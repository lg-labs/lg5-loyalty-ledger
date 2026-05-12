# ADR-004: Build VitePress twice with different `base` paths for the two targets

- **Status:** Accepted
- **Date:** 2026-05-11
- **Deciders:** stakeholder (`lglabs`)
- **Consulted:** sdd-planner (this feature)
- **Informed:** loyalty-ledger team

## Context

ADR-002 commits to dual-deploying the VitePress site to GitHub Pages
**and** Firebase Hosting. The two targets serve at different URL
roots:

- **GitHub Pages**, for a non-`<user>.github.io` repository, publishes
  at `https://<org>.github.io/<repo>/` — a path prefix of
  `/lg5-loyalty-ledger/`.
- **Firebase Hosting**, for a project site, publishes at
  `https://<site>.web.app/` — root path `/`.

VitePress (like every static-site generator that emits absolute asset
paths) requires a build-time `base` configuration matching the serve
root. A single build with one `base` value produces broken asset URLs
on the other target.

REQ-001 requires "one stable, well-known location". REQ-008 requires
public reachability of every entry. Broken assets violate both.

## Decision

We will build VitePress **twice** in CI, with different `base` values
parameterized at build time:

- **Build A** (for GitHub Pages): `base: '/lg5-loyalty-ledger/'`.
- **Build B** (for Firebase): `base: '/'`.

Each build is uploaded as a distinct artifact and consumed by the
target-specific deploy job. The VitePress configuration reads `base`
from an environment variable (e.g. `DOCS_BASE`) so the two builds share
one source tree.

## Alternatives considered

- **Single build with `base: '/'`, served from a custom domain on
  Pages.**
  - Pros: one build artifact; same URL shape on both targets.
  - Cons: requires a custom domain (DNS work, name selection); the
    stakeholder explicitly deferred the domain decision.
  - Why not chosen: not actionable in this feature.

- **Single build with `base: '/lg5-loyalty-ledger/'`, rewrite paths on
  Firebase via hosting rewrites.**
  - Pros: one build artifact.
  - Cons: Firebase rewrites cover URL paths, not the absolute asset
    URLs that VitePress bakes into HTML and JS bundles. Asset URLs
    would still be wrong; some assets would load, others wouldn't.
  - Why not chosen: doesn't actually fix the breakage.

- **Accept broken relative links on Firebase** (or on Pages).
  - Pros: trivial — one build.
  - Cons: degrades UX on one target; contradicts REQ-008's "reach
    every entry" promise.
  - Why not chosen: violates the requirement.

- **Use only one target.**
  - Pros: trivial.
  - Cons: contradicts ADR-002.
  - Why not chosen: out of scope; the dual-target decision is upstream.

## Consequences

- **Positive:** every link and asset works on every target. REQ-001
  and REQ-008 satisfied uniformly.
- **Positive:** the `base` value is parameterized, so adding a third
  target later (or moving Pages to a custom domain — a future ADR)
  costs one extra build invocation, not a config rewrite.
- **Negative:** CI runs the VitePress build twice on every trunk
  advance and twice on every opt-in preview run. The cost is small
  (VitePress is fast), but it must be accounted for in the workflow
  topology — the docs-build job has two outputs, or there are two
  docs-build jobs.
- **Negative:** any local-preview command must pick one `base` (the
  Make target `make docs-preview` will default to `base: '/'` since
  that matches the local dev server's root).

## Constitutional impact

- **RULE-001 (stack-baseline)** — **N/A.**
- **RULE-004 (service-module-shape)** — **N/A.**
- **RULE-006 (rest-media-type)** — **N/A.**
- **RULE-014 (configuration-prefixes)** — **N/A** (no Spring config;
  the VitePress `base` is build-tool config, not application config).
- **RULE-017 (build-commands)** — **clarifies.** The two builds are
  wrapped under a single Make target that takes a parameter, or two
  sibling Make targets (`make docs-build-pages`,
  `make docs-build-firebase`). The plan picks the sibling-targets
  shape for clarity.

## Implementation notes

- PRD requirements covered (indirectly): REQ-001, REQ-008.
- Pre-condition: the GitHub repo is `lg-labs/lg5-loyalty-ledger` →
  Pages base path is `/lg5-loyalty-ledger/`. If the repo is ever
  renamed, this ADR must be revisited.
- A future ADR (out of scope here) may introduce a custom Pages domain
  and consolidate to a single `base: '/'` build. This ADR is
  superseded automatically if that happens.

## Related ADRs

- ADR-001 (site engine — VitePress, which has the `base` mechanism).
- ADR-002 (dual hosting — the cause of needing two builds).
- ADR-005 (preview channels — uses Build B (`base: '/'`) since previews
  are Firebase-only).

## Definition of Done (ADR)

- [x] Status is `Accepted`.
- [x] Decision stated in active voice.
- [x] More than one alternative documented.
- [x] Consequences cover positive AND negative.
- [x] Constitutional impact names every relevant `must` rule.
- [x] No `must` override → no time-box needed.

---

_Originally drafted: 2026-05-11 · Last reviewed: 2026-05-11._
