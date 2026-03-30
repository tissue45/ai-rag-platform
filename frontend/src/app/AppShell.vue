<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NLayout, NLayoutHeader, NLayoutContent, NSpace } from 'naive-ui'
import PageContainer from './PageContainer.vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isAuthed = computed(() => auth.isAuthenticated)
const isLoginPage = computed(() => route.path === '/login')

function goHealth() {
  router.push('/health')
}

function goDocuments() {
  router.push('/documents')
}

function goLogin() {
  router.push('/login')
}

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <NLayout embedded>
    <NLayoutHeader bordered class="shell__header">
      <PageContainer>
        <div class="shell__headerRow">
          <div class="shell__brand">AI RAG Platform</div>
          <NSpace>
            <NButton v-if="isAuthed" size="small" quaternary @click="goDocuments">Documents</NButton>
            <NButton v-if="!isLoginPage" size="small" quaternary @click="goHealth">Health</NButton>
            <NButton v-if="!isAuthed" size="small" type="primary" @click="goLogin">로그인</NButton>
            <NButton v-else size="small" @click="logout">로그아웃</NButton>
          </NSpace>
        </div>
      </PageContainer>
    </NLayoutHeader>

    <NLayoutContent class="shell__content">
      <PageContainer>
        <slot />
      </PageContainer>
    </NLayoutContent>
  </NLayout>
</template>

<style scoped>
.shell__header {
  height: 56px;
  display: flex;
  align-items: center;
}

.shell__headerRow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.shell__brand {
  font-weight: 800;
  letter-spacing: 0.2px;
}

.shell__content {
  padding: 20px 0;
}
</style>

