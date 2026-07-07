import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Dev server proxies keep the SPA same-origin and avoid CORS:
//   /api  -> notiflow-api  (no CORS config on the backend)
//   /prom -> Prometheus HTTP API (dashboard metrics), path prefix stripped
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/prom': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/prom/, ''),
      },
    },
  },
});
