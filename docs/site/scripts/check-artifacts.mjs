// docs/site/scripts/check-artifacts.mjs
//
// Pre-build artifact-presence guard for the loyalty-ledger docs surface.
// Implements REQ-019 per design.md §7.8 and tasks.md TASK-003.
//
// Behavior:
//   - For each of the 4 expected upstream artifacts (relative to docs/site/),
//     check if the file exists on disk.
//   - For each MISSING artifact, emit one `::warning file=<path>::<copy>` line
//     (GitHub Actions workflow command) on stdout, and accumulate the blockquote
//     fragment for the artifact's section.
//   - For each section, write the joined fragments (blank-line separated) to
//     `<section>/_placeholder.md`. Sections with zero missing artifacts get
//     an empty `_placeholder.md` (clears stale copy when CI produces all
//     artifacts).
//   - Sections not in the table (`adr/`, `runbook/`) are NEVER touched.
//   - Always exit 0. Only fatal I/O errors (writeFileSync throws) cause
//     non-zero exit via Node's default uncaught-exception handler.
//
// Stakeholder-confirmed disambiguations:
//   - A1: architecture/ has two artifacts; their fragments are concatenated
//     (separated by a blank line) into a single architecture/_placeholder.md.
//   - Allure HEAD probe (design.md §7.8 row 5, optional, no on-disk artifact)
//     is intentionally NOT implemented here — TASK-011 covers it.

import { existsSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const docsSiteRoot = resolve(__dirname, '..');

// Verbatim from design.md §7.8 (4 on-disk artifact rows).
const ARTIFACTS = [
  {
    artifact: 'public/dependency-graph.png',
    section: 'architecture',
    copy: 'The dependency graph was not produced in the most recent CI run.',
  },
  {
    artifact: 'public/gource.mp4',
    section: 'architecture',
    copy: 'The repository activity visualization was not produced in the most recent CI run.',
  },
  {
    artifact: 'api/swagger-ui.html',
    section: 'api',
    copy: 'The synchronous service contract (Swagger UI) was not produced in the most recent CI run.',
  },
  {
    artifact: 'events/asyncapi.html',
    section: 'events',
    copy: 'The asynchronous service contract (AsyncAPI) was not produced in the most recent CI run.',
  },
];

// Sections this script owns (subset of ARTIFACTS sections, deduplicated).
// Each owned section gets its _placeholder.md rewritten on every run, even
// if no artifact is missing (empty file = cleared stale copy).
const ownedSections = [...new Set(ARTIFACTS.map((a) => a.section))];

// Accumulator: section -> array of blockquote fragments for missing artifacts.
const fragmentsBySection = new Map(ownedSections.map((s) => [s, []]));

for (const { artifact, section, copy } of ARTIFACTS) {
  const artifactPath = resolve(docsSiteRoot, artifact);
  if (existsSync(artifactPath)) continue;
  // Missing — emit GitHub Actions warning command and accumulate fragment.
  console.log(`::warning file=${artifact}::${copy}`);
  fragmentsBySection.get(section).push(`> ${copy}\n`);
}

// Write _placeholder.md for each owned section. Joined with a blank line
// between fragments; empty file when no artifacts are missing for that section.
for (const section of ownedSections) {
  const placeholderPath = resolve(docsSiteRoot, section, '_placeholder.md');
  const fragments = fragmentsBySection.get(section);
  const body = fragments.length === 0 ? '' : fragments.join('\n');
  writeFileSync(placeholderPath, body);
}
