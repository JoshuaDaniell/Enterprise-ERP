import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api/inventory': {
        target: 'http://localhost:18082',
        rewrite: (path) => path.replace(/^\/api\/inventory/, '/api'),
      },
      '/api/sales': {
        target: 'http://localhost:18081',
        rewrite: (path) => path.replace(/^\/api\/sales/, '/api'),
      },
      '/api/finance': {
        target: 'http://localhost:18083',
      },
    },
  },
})
