import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'

export default defineConfig({
  plugins: [react(), tailwindcss()],
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
    },
  },
})
