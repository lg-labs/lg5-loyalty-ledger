<script setup lang="ts">
declare const __COMMIT_SHA__: string;
declare const __BUILD_TIME__: string;
declare const __PR_NUMBER__: string;

const fullSha = __COMMIT_SHA__;
// REQ-020 mandates the *short* commit identifier; slice to 7 chars
// unless the build-time value is already short (e.g. local 'dev').
const sha = fullSha.length > 7 ? fullSha.slice(0, 7) : fullSha;
const buildTime = __BUILD_TIME__;
const prNumber = __PR_NUMBER__;
</script>

<template>
  <footer class="source-state-footer">
    Built from <code>{{ sha }}</code> · {{ buildTime }}
    <span v-if="prNumber"> · <em>(PR #{{ prNumber }})</em></span>
  </footer>
</template>

<style scoped>
.source-state-footer {
  padding: 1rem;
  text-align: center;
  font-size: 0.85em;
  color: var(--vp-c-text-2);
  border-top: 1px solid var(--vp-c-divider);
}
</style>
