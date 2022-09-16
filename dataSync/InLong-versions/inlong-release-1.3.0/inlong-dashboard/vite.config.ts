/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import path from 'path';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tsConfigPaths from 'vite-tsconfig-paths';
import vitePluginImp from 'vite-plugin-imp';
import dynamicImport from 'vite-plugin-dynamic-import';
import svgr from 'vite-plugin-svgr';
import eslintPlugin from 'vite-plugin-eslint';

export default defineConfig({
  resolve: {
    alias: [
      { find: /^~/, replacement: '' },
      { find: '@', replacement: path.join(__dirname, 'src') },
    ],
  },
  server: {
    proxy: {
      '/inlong/manager/api': {
        target: 'http://127.0.0.1:8083',
        changeOrigin: true,
        secure: false,
      },
    },
  },
  plugins: [
    react(),
    tsConfigPaths(),
    vitePluginImp({
      libList: [
        {
          libName: 'antd',
          style: name => `antd/es/${name}/style`,
        },
      ],
    }),
    dynamicImport(),
    svgr(),
    eslintPlugin({
      include: ['src/**/*.js', 'src/**/*.jsx', 'src/**/*.ts', 'src/**/*.tsx'],
    }),
  ],
  css: {
    preprocessorOptions: {
      less: {
        javascriptEnabled: true,
        modifyVars: {
          hack: `true; @import "${path.resolve('src/themes/antd.var.less')}";`,
        },
      },
    },
  },
});
