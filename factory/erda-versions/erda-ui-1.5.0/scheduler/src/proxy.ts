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

import { createProxyMiddleware } from 'http-proxy-middleware';
import { Request } from 'express';
import { getEnv, logger } from './util';
import { INestApplication } from '@nestjs/common';
import qs from 'query-string';

const isProd = process.env.NODE_ENV === 'production';

const { envConfig } = getEnv();
const { BACKEND_URL, GITTAR_ADDR, UC_BACKEND_URL } = envConfig;

const API_URL = BACKEND_URL.startsWith('http') ? BACKEND_URL : `http://${BACKEND_URL}`;
const UC_API_URL = UC_BACKEND_URL.startsWith('http') ? UC_BACKEND_URL : `http://${UC_BACKEND_URL}`;

let gittarUrl = isProd ? GITTAR_ADDR : BACKEND_URL;
gittarUrl = gittarUrl.startsWith('http') ? gittarUrl : `http://${gittarUrl}`;

const wsPathRegex = [
  /^\/api\/[^/]*\/websocket/,
  /^\/api\/[^/]*\/fdp-websocket/, // http-proxy-middleware can't handle multiple ws proxy https://github.com/chimurai/http-proxy-middleware/issues/463
  /^\/api\/[^/]*\/terminal/,
  /^\/api\/[^/]*\/apim-ws\/api-docs\/filetree/,
];

export const createProxyService = (app: INestApplication) => {
  const wsProxy = createProxyMiddleware(
    (pathname: string, req: Request) => {
      return req.headers.upgrade === 'websocket' && wsPathRegex.some((regex) => regex.test(pathname));
    },
    {
      target: API_URL,
      ws: true,
      changeOrigin: !isProd,
      xfwd: true,
      secure: false,
      pathRewrite: replaceApiOrgPath,
      onProxyReqWs: (proxyReq, req: Request, socket) => {
        if (isProd) {
          const { query } = qs.parseUrl(req.url);
          logger.info(`get ws org: ${query?.wsOrg}`);
          proxyReq.setHeader('org', query?.wsOrg);
        }
        socket.on('error', (error) => {
          logger.warn('Websocket error:', error); // add error handler to prevent server crash https://github.com/chimurai/http-proxy-middleware/issues/463#issuecomment-676630189
        });
      },
    },
  );
  app.use(wsProxy);
  app.use(
    createProxyMiddleware(
      (pathname: string) => {
        return !!pathname.match('^/api/uc');
      },
      {
        target: UC_API_URL,
        changeOrigin: true,
        secure: false,
        pathRewrite: (api) => (isProd ? api.replace('/api/uc', '') : api),
      },
    ),
  );
  app.use(
    createProxyMiddleware(
      (pathname: string) => {
        return !!pathname.match('^/api');
      },
      {
        target: API_URL,
        changeOrigin: !isProd,
        xfwd: true,
        secure: false,
        pathRewrite: replaceApiOrgPath,
        onProxyReq: (proxyReq, req: Request) => {
          if (!isProd) {
            proxyReq.setHeader('referer', API_URL);
          } else {
            const org = extractOrg(req.originalUrl); // api/files not append org to path,org not exist in this condition
            if (org) {
              proxyReq.setHeader('org', org);
            }
          }
        },
      },
    ),
  );
  let dataServiceUIAddr = isProd ? process.env.FDP_UI_ADDR : API_URL;
  dataServiceUIAddr = dataServiceUIAddr.startsWith('http') ? dataServiceUIAddr : `http://${dataServiceUIAddr}`;
  app.use(
    '/fdp-app/',
    createProxyMiddleware({
      target: `${dataServiceUIAddr}/`,
      changeOrigin: !isProd,
      pathRewrite: (p: string, req: Request) => {
        if (p === `/fdp-app/static/menu.json`) {
          const lang = req.headers.lang || 'zh-CN';
          return `/fdp-app/static/menu-${lang === 'zh-CN' ? 'zh' : 'en'}.json`;
        }
        return p;
      },
    }),
  );
  app.use(
    createProxyMiddleware(
      (pathname: string, req: Request) => {
        if (pathname.startsWith('/wb/')) {
          return true;
        }
        const userAgent = req.headers['user-agent'];
        if (userAgent.toLowerCase().includes('git')) {
          // compatible with JGit
          return /[^/]*\/dop/.test(pathname);
        }
        return false;
      },
      {
        target: gittarUrl,
        changeOrigin: !isProd,
      },
    ),
  );
  app.use(
    '/metadata.json',
    createProxyMiddleware({
      target: API_URL,
      changeOrigin: !isProd,
    }),
  );
  return wsProxy;
};

const replaceApiOrgPath = (p: string) => {
  if (isProd) {
    const match = /\/api\/([^/]*)\/(.*)/.exec(p); // /api/orgName/path => /api/path
    if (match && !p.startsWith('/api/files')) {
      if (wsPathRegex.some((regex) => regex.test(p))) {
        if (Object.keys(qs.parseUrl(p).query).length) {
          return `/api/${match[2]}&wsOrg=${match[1]}`;
        }
        return `/api/${match[2]}?wsOrg=${match[1]}`;
      }
      return `/api/${match[2]}`;
    }
  }
  return p;
};

const extractOrg = (p: string) => {
  const match = /^\/[^/]*\/([^/]*)\/?/.exec(p);
  if (match && !p.startsWith('/api/files')) {
    return match[1];
  }
  return '';
};
