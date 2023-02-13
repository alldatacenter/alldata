// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

/* eslint-disable */
import legacyPlugin from '@vitejs/plugin-legacy';
import react from '@vitejs/plugin-react';
import fs from 'fs';
import * as path from 'path';
import tailwindcss from 'tailwindcss';
import autoprefixer from 'autoprefixer';
import { getLessTheme, getScssTheme } from './config/theme';

const babelConfig = require('./babel.config.js');

const packageJson = require('./package.json');

const mainVersion = packageJson.version.slice(0, -2);
const dotenv = require('dotenv');

const { parsed: envConfig } = dotenv.config({ path: path.resolve(__dirname, '../.env') });

// @see https://cn.vitejs.dev/config/
export default ({ command, mode }) => {
  let alias = {
    'core/index': path.resolve(__dirname, '../core/src/index'),
    'core/config': path.resolve(__dirname, '../core/src/config'),
    'core/i18n': path.resolve(__dirname, '../core/src/i18n'),
    'core/cube': path.resolve(__dirname, '../core/src/cube'),
    'core/service': path.resolve(__dirname, '../core/src/service'),
    'admin/entry': path.resolve(__dirname, '../../erda-ui-enterprise/admin/src'),
    'fdp/entry': path.resolve(__dirname, '../../erda-ui-enterprise/fdp/src'),
    'core/agent': path.resolve(__dirname, '../core/src/agent'),
    'core/stores/route': path.resolve(__dirname, '../core/src/stores/route'),
    'core/stores/userMap': path.resolve(__dirname, '../core/src/stores/user-map'),
    'core/utils/ws': path.resolve(__dirname, '../core/src/utils/ws.ts'),
    'core/stores/loading': path.resolve(__dirname, '../core/src/stores/loading.ts'),
    __mocks__: path.resolve(__dirname, './__mocks__'),
    app: path.resolve(__dirname, './app'),
    auto_test: path.resolve(__dirname, './auto_test'),
    bash: path.resolve(__dirname, './bash'),
    config: path.resolve(__dirname, './config'),
    node_modules: path.resolve(__dirname, './node_modules'),
    snippets: path.resolve(__dirname, './snippets'),
    test: path.resolve(__dirname, './test'),
    tools: path.resolve(__dirname, './tools'),
    common: path.resolve(__dirname, './app/common'),
    configForm: path.resolve(__dirname, './app/configForm'),
    'yml-chart': path.resolve(__dirname, './app/yml-chart'),
    'config-page': path.resolve(__dirname, './app/config-page'),
    layout: path.resolve(__dirname, './app/layout'),
    user: path.resolve(__dirname, './app/user'),
    charts: path.resolve(__dirname, './app/charts'),
    dcos: path.resolve(__dirname, './app/modules/dcos'),
    project: path.resolve(__dirname, './app/modules/project'),
    publisher: path.resolve(__dirname, './app/modules/publisher'),
    cmp: path.resolve(__dirname, './app/modules/cmp'),
    org: path.resolve(__dirname, './app/modules/org'),
    application: path.resolve(__dirname, './app/modules/application'),
    runtime: path.resolve(__dirname, './app/modules/runtime'),
    dop: path.resolve(__dirname, './app/modules/dop'),
    addonPlatform: path.resolve(__dirname, './app/modules/addonPlatform'),
    msp: path.resolve(__dirname, './app/modules/msp'),
    apiManagePlatform: path.resolve(__dirname, './app/modules/apiManagePlatform'),
    agent: path.resolve(__dirname, './app/agent.js'),
    i18n: path.resolve(__dirname, './app/i18n.ts'),
    'monitor-overview': path.resolve(__dirname, './app/modules/msp/monitor/monitor-overview'),
    'application-insight': path.resolve(__dirname, './app/modules/msp/monitor/application-insight'),
    'external-insight': path.resolve(__dirname, './app/modules/msp/monitor/external-insight'),
    'browser-insight': path.resolve(__dirname, './app/modules/msp/monitor/browser-insight'),
    'gateway-ingress': path.resolve(__dirname, './app/modules/msp/monitor/gateway-ingress'),
    'docker-container': path.resolve(__dirname, './app/modules/msp/monitor/docker-container'),
    'mobile-insight': path.resolve(__dirname, './app/modules/msp/monitor/mobile-insight'),
    'api-insight': path.resolve(__dirname, './app/modules/msp/monitor/api-insight'),
    'trace-insight': path.resolve(__dirname, './app/modules/msp/monitor/trace-insight'),
    'monitor-common': path.resolve(__dirname, './app/modules/msp/monitor/monitor-common'),
    topology: path.resolve(__dirname, './app/modules/msp/monitor/topology'),
    'status-insight': path.resolve(__dirname, './app/modules/msp/monitor/status-insight'),
    'error-insight': path.resolve(__dirname, './app/modules/msp/monitor/error-insight'),
    'monitor-alarm': path.resolve(__dirname, './app/modules/msp/monitor/monitor-alarm'),
  };

  const wsPathRegex = [
    /^\/api\/[^/]*\/websocket/,
    /^\/api\/[^/]*\/fdp-websocket/, // http-proxy-middleware can't handle multiple ws proxy https://github.com/chimurai/http-proxy-middleware/issues/463
    /^\/api\/[^/]*\/terminal/,
    /^\/api\/[^/]*\/apim-ws\/api-docs\/filetree/,
  ];

  return {
    base: './', // index.html文件所在位置
    root: './', // js导入的资源路径，src
    resolve: {
      alias,
    },
    publicDir: 'app/static',
    define: {
      'process.env.VITE': '"true"',
      'process.env.UI_ENV': JSON.stringify(process.env.ERDA_UI_ENV),
      'process.env.FOR_COMMUNITY': JSON.stringify(process.env.FOR_COMMUNITY),
      'process.env.mainVersion': JSON.stringify(mainVersion),
    },
    server: {
      host: envConfig.SCHEDULER_URL.replace('https://', ''),
      proxy: {
        // string shorthand
        // '/foo': 'http://localhost:4567',
        // with options
        '/api/': {
          target: envConfig.BACKEND_URL,
          changeOrigin: true,
          secure: false,
        },
      },
      https: {
        key: fs.readFileSync('../cert/dev/server.key'),
        cert: fs.readFileSync('../cert/dev/server.crt'),
      },
    },
    plugins: [
      legacyPlugin({
        targets: ['Chrome >= 80', 'Safari >= 10.1', 'iOS >= 10.3', 'Firefox >= 54', 'Edge >= 15'],
      }),
      react({
        babel: {
          plugins: babelConfig.plugins.slice(0, 2),
        },
      }),
    ],
    css: {
      preprocessorOptions: {
        less: {
          // 支持内联 JavaScript
          javascriptEnabled: true,
          modifyVars: getLessTheme(),
        },
        scss: {
          additionalData: `@import "app/styles/_color.scss";@import "app/styles/_variable.scss";@import "app/styles/_mixin.scss";${getScssTheme()}`,
        },
      },
      postcss: {
        plugins: [tailwindcss, autoprefixer],
      },
    },
  };
};
