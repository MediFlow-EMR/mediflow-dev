import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  build: {
    rollupOptions: { output: { manualChunks: {
      framework: ['react', 'react-dom', 'react-router-dom', 'zustand'],
      charts: ['recharts'],
    } } },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:9005',
        changeOrigin: true,
      },
    },
  },
});