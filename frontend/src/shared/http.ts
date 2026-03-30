import axios from 'axios'
import { getApiBaseUrl } from './env'
import { useAuthStore } from '../stores/auth'

export const http = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 10_000,
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  const token = auth.accessToken
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

