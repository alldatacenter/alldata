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

import cube from 'cube-state';
import { getConfig } from './config';

const defaultPaging = {
  pageNo: 1,
  pageSize: 15,
  total: 0,
};

const { createStore, createFlatStore, use, storeMap } = cube({
  singleton: true,
  extendEffect({ update, select }) {
    return {
      async call(fn: Function, payload: any = {}, config = {} as any) {
        const { paging, successMsg, errorMsg, fullResult } = config;
        const pagingKey = paging && paging.key;
        const listKey = paging && paging.listKey;
        const pageNoKey = paging && paging.pageNoKey;

        // save as request key for later abort
        // markReq(prefixType(fn.name, model));
        let _payload = payload;
        if (pagingKey) {
          const curPaging = select((s) => s[pagingKey]); // 当前paging
          const pageNo = +(payload.pageNo || paging.pageNo || defaultPaging.pageNo);
          const pageSize = +(payload.pageSize || paging.pageSize || curPaging.pageSize || defaultPaging.pageSize);
          _payload = { ...payload, pageNo, pageSize };
        }
        const result = await fn(_payload);
        const keys = Object.keys(result || {});
        // 标准格式的返回结果
        if (keys.includes('success') && (keys.includes('err') || keys.includes('data'))) {
          const { success, data, err, userInfo } = result;
          if (storeMap.coreUserMap && userInfo) {
            storeMap.coreUserMap.reducers.setUserMap(userInfo);
          }
          if (success) {
            if (successMsg) {
              getConfig('onAPISuccess')?.(successMsg);
            }
            if (pagingKey && data && 'total' in data && ('list' in data || listKey in data)) {
              listKey && (data.list = data[listKey]); // 设置了listKey时，复制一份到list上
              data.list = data.list || [];
              const currentPageNo = pageNoKey ? _payload[pageNoKey] : _payload.pageNo;
              const hasMore = Math.ceil(data.total / _payload.pageSize) > currentPageNo;
              update({
                [pagingKey]: { pageSize: _payload.pageSize, total: data.total, hasMore, pageNo: currentPageNo },
              });
            }
          } else {
            getConfig('onAPIFail')?.('error', err.msg || errorMsg);
          }
          return fullResult ? result : data === undefined ? {} : data;
        } else {
          if (process.env.NODE_ENV !== 'production') {
            // eslint-disable-next-line no-console
            console.warn('[shell] Nonstandard response body', fn.name);
          }
          if (successMsg) {
            getConfig('onAPISuccess')?.(successMsg);
          }
        }
        return result;
      },
      getParams() {
        if (storeMap.routeInfo) {
          return storeMap.routeInfo.getState((s: any) => s.params);
        }
      },
      getQuery() {
        if (storeMap.routeInfo) {
          return storeMap.routeInfo.getState((s: any) => s.query);
        }
      },
    };
  },
});

export { createStore, createFlatStore, use, storeMap };
