<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NAlert, NButton, NCard, NForm, NFormItem, NInput, NText } from 'naive-ui'
import { http } from '../shared/http'
import { useAuthStore } from '../stores/auth'
import { useNotify } from '../shared/notify'

type RegisterResponse = {
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
  passwordConfirm: '',
})

async function submit() {
  error.value = null
  if (form.password !== form.passwordConfirm) {
    error.value = '비밀번호가 서로 일치하지 않습니다.'
    notify.error('회원가입 실패')
    return
  }
  loading.value = true
  try {
    const { data } = await http.post<RegisterResponse>('/api/auth/register', {
      email: form.email,
      password: form.password,
    })
    auth.setToken(data.accessToken)
    notify.success('가입 완료')
    router.push('/documents')
  } catch (e: any) {
    const message = e?.response?.data?.message || e?.message || '회원가입 실패'
    error.value = String(message)
    notify.error('회원가입 실패')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <NCard title="회원가입" size="medium" style="max-width: 480px; margin: 24px auto">
    <template #header-extra>
      <NText depth="3">비밀번호 8자 이상</NText>
    </template>
    <NForm label-placement="top" @submit.prevent="submit">
      <NFormItem label="이메일">
        <NInput v-model:value="form.email" placeholder="email@example.com" />
      </NFormItem>
      <NFormItem label="비밀번호">
        <NInput v-model:value="form.password" type="password" show-password-on="mousedown" />
      </NFormItem>
      <NFormItem label="비밀번호 확인">
        <NInput v-model:value="form.passwordConfirm" type="password" show-password-on="mousedown" />
      </NFormItem>
      <NButton type="primary" attr-type="submit" :loading="loading" block>가입하기</NButton>
      <NText depth="3" style="display: block; margin-top: 12px; text-align: center">
        이미 계정이 있나요?
        <router-link to="/login" style="margin-left: 4px">로그인</router-link>
      </NText>
      <NAlert v-if="error" style="margin-top: 12px" type="error" :show-icon="false">
        {{ error }}
      </NAlert>
    </NForm>
  </NCard>
</template>
