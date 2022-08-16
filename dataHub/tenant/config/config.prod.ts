import { defineConfig } from 'umi'

export default defineConfig({
  devServer: {
    host: '0.0.0.0',
    port: 3000,
  },

  define: {
    API_GRAPHQL_URL: 'http://localhost:9800/graphql/',
    MEDIA_URL: 'http://localhost:9800',
  },
})
