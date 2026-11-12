
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const accessToken = env.CLEARPATH_AI_ACCESS_TOKEN

  return {
    plugins: [react()],

    server: {
      port: 5174,
      strictPort: true,

      proxy: {
        '/api': {
          target: 'http://127.0.0.1:8080',
          changeOrigin: true,

          configure(proxy) {
            proxy.on('proxyReq', (proxyReq) => {
              if (accessToken) {
                proxyReq.setHeader(
                    'X-ClearPath-Token',
                    accessToken
                )
              }
            })
          }
        }
      }
    }
  }
})
