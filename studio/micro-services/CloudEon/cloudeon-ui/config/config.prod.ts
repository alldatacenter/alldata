import { defineConfig } from 'umi';
import { proHost } from './apiConfig'
export default defineConfig({
  define: {
    "process.env.UMI_ENV": process.env.UMI_ENV,
    "process.env.API_HOST": proHost,
  },
});