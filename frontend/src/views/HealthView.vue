<script setup lang="ts">
import { computed, ref } from 'vue'
import { NAlert, NButton, NCard, NFlex, NTag, NText } from 'naive-ui'
import { http } from '../shared/http'
import { getApiBaseUrl } from '../shared/env'
import { useNotify } from '../shared/notify'

type HealthResponse = {
  status?: string
}

const apiBaseUrl = getApiBaseUrl()
const loading = ref(false)
const error = ref<string | null>(null)
const status = ref<string | null>(null)
const notify = useNotify()

const statusLabel = computed(() => status.value ?? 'UNKNOWN')
const statusTone = computed(() => {
  if (status.value === 'UP') return 'ok'
  if (status.value === 'DOWN') return 'bad'
  return 'neutral'
})

async function checkHealth() {
  loading.value = true
  error.value = null
  try {
    const res = await http.get<HealthResponse>('/actuator/health')
    status.value = res.data?.status ?? null
    if (status.value === 'UP') notify.success('백엔드 연결 OK (UP)')
  } catch (e) {
    status.value = null
    error.value = e instanceof Error ? e.message : '요청 실패'
    notify.error('헬스체크 실패')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <NCard title="Health check" size="medium" style="max-width: 720px; margin: 24px auto">
    <template #header-extra>
      <NText depth="3">API: <code>{{ apiBaseUrl }}</code></NText>
    </template>

    <NFlex vertical :size="12">
      <NButton type="primary" :loading="loading" @click="checkHealth">
        Check /actuator/health
      </NButton>

      <NFlex justify="space-between" align="center">
        <NText depth="2">Status</NText>
        <NTag :type="statusTone === 'ok' ? 'success' : statusTone === 'bad' ? 'error' : 'default'" size="small">
          {{ statusLabel }}
        </NTag>
      </NFlex>

      <NAlert v-if="error" type="error" title="요청 실패" :show-icon="false">
        {{ error }}
      </NAlert>
    </NFlex>
  </NCard>
</template>

