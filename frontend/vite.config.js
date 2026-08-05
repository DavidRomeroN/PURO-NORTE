import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  // sockjs-client espera `global` (Node); en el navegador no existe y la app queda en blanco.
  define: {
    global: 'globalThis',
  },
  optimizeDeps: {
    include: ['sockjs-client', '@stomp/stompjs'],
  },
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  server: {
    port: 5173,
    // Permite abrir la app desde el celular usando la IP de la laptop en el local.
    host: true,
    // Atajo para probar en el celular: si pones VITE_API_URL=/api, las llamadas
    // salen por el mismo origen y no hay que tocar el CORS del backend.
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/ws': { target: 'http://localhost:8080', changeOrigin: true, ws: true },
    },
  },
})
