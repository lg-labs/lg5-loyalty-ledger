// docs/site/scripts/linkinator-to-annotations.mjs
//
// Post-build broken-link checker for the loyalty-ledger docs surface.
// Implements REQ-018 per design.md §7.7 and tasks.md TASK-004.
//
// Behavior:
//   - Crawls docs/site/.vitepress/dist/ recursively using linkinator's
//     programmatic API (LinkChecker class) instead of the CLI form
//     `pnpm exec linkinator … --silent`. Rationale: the AC's --silent
//     flag suppresses parseable stdout from the CLI; the Node API
//     returns a structured result that maps directly to the required
//     ::warning:: format.
//   - For each link with state === BROKEN, emits one
//     `::warning file=<page>,line=<n>::Broken link <url> -> <status>`
//     line (GitHub Actions workflow command) on stdout.
//   - Linkinator's API does NOT expose source line numbers per link;
//     <n> is therefore always emitted as `1` (placeholder).
//   - If `.vitepress/dist/` does not exist, emits one ::warning:: line
//     and exits 0 (does not throw).
//   - Always exits 0 (per spec: "exits 0 unconditionally"). Uncaught
//     exceptions are caught and reported as a single ::warning:: line.
//
// Allow-list pattern is verbatim from design.md §7.7 (post-fix in
// commit b56f420 which added `localhost(:\d+)?(/|$)` to the negative
// lookahead so linkinator's local-server URL rewrites are crawled).

import { existsSync } from 'node:fs';
import { dirname, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { LinkChecker, LinkState } from 'linkinator';

const __dirname = dirname(fileURLToPath(import.meta.url));
const docsSiteRoot = resolve(__dirname, '..');
const distPath = resolve(docsSiteRoot, '.vitepress/dist');

// Verbatim from design.md §7.7 (post-fix). Double-escaped backslashes
// here yield a regex string with single backslashes for linkinator.
const SKIP_PATTERN =
  '^https?://(?!localhost(:\\d+)?(/|$)|(lglabs-loyalty-docs|lglabs-loyalty-allure)\\.web\\.app|.+\\.github\\.io/lg5-loyalty-ledger)';

async function main() {
  if (!existsSync(distPath)) {
    console.log(
      '::warning file=docs/site/.vitepress/dist::dist directory missing — run vitepress build first',
    );
    return;
  }

  const checker = new LinkChecker();
  const result = await checker.check({
    path: distPath,
    recurse: true,
    linksToSkip: [SKIP_PATTERN],
  });

  for (const link of result.links) {
    if (link.state !== LinkState.BROKEN) continue;
    // Normalize parent (the page that contained the broken link) to a
    // dist-relative path. When linkinator reports the dist root itself
    // (its synthetic crawl entry), surface it as `index.html` — the
    // file actually being parsed. Defensive fallback: `unknown`.
    let page = 'unknown';
    if (link.parent) {
      const rel = relative(distPath, link.parent);
      page = rel === '' ? 'index.html' : rel === '..' || rel.startsWith('..') ? link.parent : rel;
      if (page.endsWith('/')) page += 'index.html';
    }
    // Strip distPath prefix from broken URLs so they read as
    // dist-relative paths instead of absolute filesystem paths.
    let url = link.url;
    if (url.startsWith(distPath)) {
      url = relative(distPath, url) || url;
    }
    const status = link.status ?? 'unknown';
    console.log(
      `::warning file=${page},line=1::Broken link ${url} -> ${status}`,
    );
  }
}

try {
  await main();
} catch (err) {
  console.log(
    `::warning file=docs/site/scripts/linkinator-to-annotations.mjs::linkinator wrapper failed: ${err?.message ?? err}`,
  );
}
