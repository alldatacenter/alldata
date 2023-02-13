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

import agent from 'core/agent';
import { getCookies, setApiWithOrg, getOrgFromPath } from 'common/utils';
import { getGlobal } from 'app/global-space';
import { some } from 'lodash';
import errorHandler from './error-handler';

const isExcludeOrgHeaderApi = (url) => {
  const excludeApis = ['/api/files', '/api/uc'];
  return some(excludeApis, (api) => url.startsWith(api));
};

function handleUrl(req) {
  const header = getGlobal('service-provider');
  if (header) {
    req.set('service-provider', header);
  }
  const { url } = req;

  // 处理api/spot接口前缀
  if (url.startsWith('/api/spot/')) {
    req.url = url.replace('/api/spot/', '/api/');
  }

  const curOrg = getOrgFromPath();
  curOrg && req.set('org', curOrg);
  if (!isExcludeOrgHeaderApi(req.url)) {
    req.url = setApiWithOrg(req.url);
  }
}

function handleError(req) {
  req.retry(2, (err, response) => {
    // csrf token过期时进行重试
    if (err && err.status === 403 && (response.text || '').includes('empty csrf token')) {
      // 重试的肯定是需要带token的，不必再判断method
      const token = getCookies('OPENAPI-CSRF-TOKEN');
      if (token) {
        req.set('OPENAPI-CSRF-TOKEN', token);
      }
      return true;
    }
    return false;
  });
  req.on('error', errorHandler);
}

// if paging list is null, transform to array
function handelPagingNull(req) {
  req.on('response', (res) => {
    if (res.body && res.body.data) {
      const { data } = res.body;
      if (typeof data === 'object' && data !== null && 'list' in data && 'total' in data && data.list === null) {
        data.list = [];
      }
    }
  });
}

const reqMap = {};
const reqKeyList = [];
// const fetchingReqMap = {};
/**
 * mark & save request for later abort
 * multi same url requests only preserve one
 */
// function mark(req) {
// const key = null;

// req.on('request', () => {
//   key = reqKeyList.pop();
//   const url = req.url;
//   // if same url request is fetching, abort others just left one
//   if (fetchingReqMap[url] === undefined) {
//     fetchingReqMap[url] = req;
//   } else {
//     // it seems directly call req.abort() not work,so abort previous request
//     fetchingReqMap[url].abort();
//     fetchingReqMap[url] = req;
//   }
//   if (reqKeyList.length) {
//     console.warn('request key list should be empty, but left: ', [...reqKeyList]);
//     reqKeyList.length = 0;
//   }
//   reqMap[key] = req;
// });
// req.on('response', () => {
//   delete reqMap[key];
//   delete fetchingReqMap[req.url];
// });
// return req;
// }

// function markReq(key) {
//   reqKeyList.push(key);
// }

// function abortReq(key) {
//   if (Array.isArray(key)) {
//     key.forEach((k) => {
//       reqMap[k] && reqMap[k].abort();
//     });
//   } else {
//     reqMap[key] && reqMap[key].abort();
//   }
// }

// mf_share导出的模块内部会引用这里的agent，导致use方法被执行两次
agent.use((req) => {
  if (!req._marked) {
    req._marked = true;
    handleUrl(req);
    handelPagingNull(req);
    handleError(req);
  }
});
export default agent;
