import { defineConfig } from 'vitepress';

const base = process.env.DOCS_BASE ?? '/';

const commitSha = process.env.COMMIT_SHA ?? 'dev';
const buildTime = process.env.BUILD_TIME ?? new Date().toISOString();
const prNumber = process.env.PR_NUMBER ?? '';

export default defineConfig({
  title: 'loyalty-ledger',
  description: 'Reference documentation for the loyalty-ledger service.',
  base,
  srcDir: '.',
  cleanUrls: true,
  ignoreDeadLinks: true,
  themeConfig: {
    nav: [
      { text: 'Architecture', link: '/architecture/' },
      { text: 'API (sync)', link: '/api/' },
      { text: 'Events (async)', link: '/events/' },
      { text: 'ADRs', link: '/adr/' },
      { text: 'Acceptance Report', link: 'https://lglabs-loyalty-allure.web.app/' },
      { text: 'Runbook', link: '/runbook/' }
    ],
    sidebar: {
      '/architecture/': [{ text: 'Overview', link: '/architecture/' }],
      '/api/': [{ text: 'Synchronous contract', link: '/api/' }],
      '/events/': [{ text: 'Asynchronous contract', link: '/events/' }],
      '/adr/': [{ text: 'Decision records', link: '/adr/' }],
      '/runbook/': [{ text: 'Onboarding runbook', link: '/runbook/' }]
    },
    search: { provider: 'local' }
  },
  vite: {
    define: {
      __COMMIT_SHA__: JSON.stringify(commitSha),
      __BUILD_TIME__: JSON.stringify(buildTime),
      __PR_NUMBER__: JSON.stringify(prNumber)
    }
  }
});
