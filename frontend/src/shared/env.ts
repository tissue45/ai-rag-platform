export function getApiBaseUrl(): string {
  const raw = import.meta.env.VITE_API_BASE_URL
  if (typeof raw === 'string' && raw.trim().length > 0) {
    let base = raw.trim().replace(/\/+$/, '')
    if (base.endsWith('/api')) base = base.slice(0, -4).replace(/\/+$/, '')
    return base
  }
  return '/api'
}

