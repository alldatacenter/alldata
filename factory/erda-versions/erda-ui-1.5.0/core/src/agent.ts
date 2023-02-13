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

import agentUse from 'superagent-use';
import { getCurrentLocale } from './i18n';
import request from 'superagent';

const superagent = agentUse(request);

function getCookies(key: string) {
  const cookies = {};
  window.document.cookie.split(';').forEach((item) => {
    const [k, v] = item.split('=');
    cookies[k.trim()] = v && v.trim();
  });
  return key ? cookies[key] : cookies;
}

/**
 * set accept header
 */
function setHeader(req: request.SuperAgentRequest) {
  req.set('Accept', 'application/vnd.dice+json;version=1.0');
  req.set('Lang', getCurrentLocale().key === 'zh' ? 'zh-CN' : 'en-US');
  if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(req.method)) {
    const token = getCookies('OPENAPI-CSRF-TOKEN');
    if (token) {
      req.set('OPENAPI-CSRF-TOKEN', token);
    }
  }
  return req;
}

superagent.use(setHeader);

export default superagent;
