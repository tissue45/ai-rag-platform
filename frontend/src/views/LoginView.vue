<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NAlert, NButton, NCard, NForm, NFormItem, NInput, NText } from 'naive-ui'
import { http } from '../shared/http'
import { useAuthStore } from '../stores/auth'
import { useNotify } from '../shared/notify'

type LoginResponse = {
  accessToken: string
  tokenType: string
}

const router = useRouter()
const auth = useAuthStore()
const notify = useNotify()

const loading = ref(false)
const error = ref<string | null>(null)
const form = reactive({
  email: '',
  password: '',
})

async function submit() {
  loading.value = true
  error.value = null
  try {
    const { data } = await http.post<LoginResponse>('/api/auth/login', form)
    auth.setToken(data.accessToken)
    notify.success('로그인 성공')
    router.push('/documents')
  } catch (e: any) {
    const message = e?.response?.data?.message || e?.message || '로그인 실패'
    error.value = String(message)
    notify.error('로그인 실패')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <NCard title="로그인" size="medium" style="max-width: 480px; margin: 24px auto">
    <template #header-extra>
      <NText depth="3">dev 기본 계정이 입력돼 있습니다.</NText>
    </template>
    <NForm label-placement="top" @submit.prevent="submit">
      <NFormItem label="이메일">
        <NInput v-model:value="form.email" placeholder="email@example.com" />
      </NFormItem>
      <NFormItem label="비밀번호">
        <NInput v-model:value="form.password" type="password" show-password-on="mousedown" />
      </NFormItem>
      <NButton type="primary" attr-type="submit" :loading="loading" block>로그인</NButton>
      <NText depth="3" style="display: block; margin-top: 12px; text-align: center">
        계정이 없나요?
        <router-link to="/register" style="margin-left: 4px">회원가입</router-link>
      </NText>
      <NAlert v-if="error" style="margin-top: 12px" type="error" :show-icon="false">
        {{ error }}
      </NAlert>
    </NForm>
  </NCard>
</template>

