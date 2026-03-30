import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

const TOKEN_KEY = 'airag.accessToken'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem(TOKEN_KEY))

  const isAuthenticated = computed(() => !!accessToken.value)

  function setToken(token: string) {
    accessToken.value = token
    localStorage.setItem(TOKEN_KEY, token)
  }

  function logout() {
    accessToken.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  return { accessToken, isAuthenticated, setToken, logout }
})

