<script setup lang="ts">
import { darkTheme, NConfigProvider, NGlobalStyle, NMessageProvider } from 'naive-ui'
import { computed } from 'vue'
import AppShell from './app/AppShell.vue'

const theme = computed(() => (window.matchMedia?.('(prefers-color-scheme: dark)')?.matches ? darkTheme : null))
</script>

<template>
  <NConfigProvider :theme="theme">
    <NGlobalStyle />
    <NMessageProvider>
      <AppShell>
        <RouterView />
      </AppShell>
    </NMessageProvider>
  </NConfigProvider>
</template>
