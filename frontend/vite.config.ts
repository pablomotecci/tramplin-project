import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Проксирование запросов к Backend
    proxy: {
      '/api/v1': {
        target: 'http://localhost:8080', /*Заменить потом на айпи сервера от Данилы Палыча*/
        changeOrigin: true,
      },

      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
