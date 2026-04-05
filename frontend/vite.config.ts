import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig(({ command }) => ({
  plugins: [vue()],
  // GitHub Pages repo site path
  base: command === 'build' ? '/ai-rag-platform/' : '/',
  server: {
    // 5173은 다른 Vite 프로젝트와 겹치기 쉬움 — 이 저장소 전용 포트
    port: 5188,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
}))
