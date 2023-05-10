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

/* eslint-disable @typescript-eslint/consistent-type-assertions */
/* eslint-disable react-hooks/rules-of-hooks */
import React from 'react';
import { createStore } from '../cube';
import { isObject } from 'lodash';
import axios from 'axios';
import { Key, pathToRegexp, compile } from 'path-to-regexp';
import qs from 'query-string';
import { IUserInfo, setUserMap } from '../stores/user-map';
import { getConfig } from '../config';

const DEFAULT_PAGESIZE = 15;

/**
 * Fill in the actual value for the path with parameters by path-to-regexp
 * @param path Paths that may contain parameters, such as /fdp/:id/detail, path can not include `?`
 * @param params The incoming parameters may be query or params
 * @returns
 */
const generatePath = (path: string, params?: Obj) => {
  try {
    const toPathRepeated = compile(path);
    return toPathRepeated(params);
  } catch (error) {
    // eslint-disable-next-line no-console
    console.error('path:', path, 'Error parsing url parameters');
    throw error;
  }
};

/**
 * Use path to match the incoming parameters, extract query and params
 * @param path Paths that may contain parameters, such as /fdp/:id/detail, path can not include `?`
 * @param params The incoming parameters may be query or params
 */
const extractPathParams = (path: string, params?: Obj<any>) => {
  const keys: Key[] = [];
  pathToRegexp(path, keys);
  const pathParams = {} as Obj<string>;
  const bodyOrQuery = { ...params };
  if (keys.length > 0) {
    keys.forEach(({ name }) => {
      pathParams[name] = bodyOrQuery[name];
      delete bodyOrQuery[name];
    });
  }
  return {
    pathParams,
    bodyOrQuery,
  };
};

interface $options {
  isDownload?: boolean; // whether its download api
  uploadFileKey?: string; // upload formData attribute
  autoMessage?: boolean; // eject message automatically
  successMsg?: string; // eject message when success to override default message
  errorMsg?: string; // eject message when failed to override default message
}
type $headers = Obj<string>;
type $body = Obj<unknown>;
export interface CallParams {
  $options?: $options;
  $headers?: $headers;
  $body?: $body;
}

type Merge<A, B> = { [K in keyof A]: K extends keyof B ? B[K] : A[K] } & B extends infer O
  ? { [K in keyof O]: O[K] }
  : never;

/**
 * generate api request function by config
 * @param apiConfig
 * @returns callable api function
 */
export const genRequest = function <T extends FN>(apiConfig: APIConfig) {
  const { api, headers } = apiConfig;
  let [method, path] = api.split('@');
  if (!path) {
    path = method;
    method = 'get';
  }

  // use Merge to extract inner properties
  return (params?: CallParams & Merge<Parameters<T>[0], {}>) => {
    const { $options, $headers, $body, ...rest } = params || {};
    const { bodyOrQuery, pathParams } = extractPathParams(path, rest);
    const { isDownload, uploadFileKey } = $options || {};
    let getParams = bodyOrQuery;
    if ('pageNo' in bodyOrQuery && !('pageSize' in bodyOrQuery)) {
      bodyOrQuery.pageSize = DEFAULT_PAGESIZE;
    }
    let bodyData;
    if (['post', 'put'].includes(method)) {
      if (Object.keys(bodyOrQuery).length) {
        bodyData = uploadFileKey ? bodyOrQuery[uploadFileKey] : bodyOrQuery;
      }
      getParams = {};
    } else if (method === 'delete') {
      bodyData = $body;
    }
    return axios({
      method: method as any,
      url: generatePath(path, pathParams),
      headers: headers ?? $headers,
      params: getParams,
      paramsSerializer: (p: Obj<string>) => qs.stringify(p),
      responseType: isDownload ? 'blob' : 'json',
      data: bodyData,
    }).then((res) => res.data) as unknown as Promise<RES_BODY<ReturnType<T>>>;
  };
};

export const apiDataStore = createStore({
  name: 'apiData',
  state: {
    body: {} as Obj,
    data: {} as Obj,
    loading: {} as Obj<boolean>,
  },
  reducers: {
    setBody(state, path: string, data: any) {
      state.body[path] = data;
    },
    setData(state, path: string, data: any) {
      state.data[path] = data;
    },
    setLoading(state, path: string, isLoading: boolean) {
      state.loading[path] = isLoading;
    },
  },
});

type FN = (...args: any) => any;
type NormalOrPagingData<D> = D extends PagingData ? Merge<D, ExtraPagingData> : D;
type RES<D> = Promise<RES_BODY<D>>;
type PICK_DATA<T extends FN> = ReturnType<T> extends RES<infer D> ? NormalOrPagingData<D> : never;
type PICK_BODY<T extends FN> = RES_BODY<PICK_DATA<T>>;
interface PagingData {
  list: any[];
  total: number;
  [k: string]: any;
}
interface ExtraPagingData {
  paging: {
    pageNo: number;
    pageSize: number;
    total: number;
    hasMore: boolean;
  };
}
export interface RES_BODY<D> {
  data: NormalOrPagingData<D> | null;
  success: boolean;
  err: {
    msg: string;
    code: string;
    ctx: null | object;
  };
  userInfo?: Obj<IUserInfo>;
}
export interface APIConfig<T extends FN> {
  api: string;
  successMsg?: string;
  errorMsg?: string;
  globalKey?: string;
  headers?: Obj<string>;
  mock?: T;
}

export function enhanceAPI<T extends FN>(_apiFn: T, config?: APIConfig<T>) {
  const { globalKey, mock } = config || {};

  let _toggleLoading: undefined | ((p: boolean) => void);
  let _setData: undefined | Function;

  const onResponse = (body: PICK_BODY<T>, params?: Parameters<T>[0]) => {
    // standard response
    if ('success' in body && ('err' in body || 'data' in body)) {
      const { data, success, err, userInfo } = body;
      if (userInfo) {
        setUserMap(userInfo);
      }

      if (isObject(data) && Object.keys(params ?? {}).includes('pageNo')) {
        if ('list' in data && 'total' in data) {
          if (data.list === null) {
            data.list = [];
          }
          const { total } = data;
          const { pageNo, pageSize } = params ?? ({} as Parameters<T>[0]);
          const hasMore = Math.ceil(total / +pageSize) > +pageNo;
          (data as any).paging = { pageNo, pageSize, total, hasMore };
        }
      }

      if (success) {
        const successMsg = params?.$options?.successMsg || config?.successMsg;
        successMsg && getConfig('onAPISuccess')?.(successMsg);
      } else if (err) {
        const errorMsg = err?.msg || params?.$options?.errorMsg || config?.errorMsg;
        errorMsg && getConfig('onAPIFail')?.('error', errorMsg);
      }
    }
  };

  let apiFn = _apiFn;
  if (mock) {
    apiFn = ((params?: Parameters<T>[0]) => {
      return new Promise((resolve) =>
        setTimeout(() => resolve({ success: true, data: mock(params), err: null }), Math.floor(Math.random() * 300)),
      );
    }) as any;
  }

  const service = (params?: Parameters<T>[0]): ReturnType<T> =>
    apiFn(params).then((body: PICK_BODY<T>) => {
      onResponse(body, params);
      return body;
    });

  return Object.assign(service, {
    fetch: (params?: Parameters<T>[0]): ReturnType<T> => {
      _toggleLoading?.(true);
      return apiFn(params)
        .then((body: PICK_BODY<T>) => {
          onResponse(body, params);
          _setData?.(body?.data);
          return body;
        })
        .finally(() => {
          _toggleLoading?.(false);
        });
    },
    useData: (): PICK_DATA<T> | null => {
      const [data, setData] = React.useState(null);

      if (globalKey) {
        _setData = (d: PICK_DATA<T>) => apiDataStore.reducers.setData(globalKey, d);
        return apiDataStore.useStore((s) => s.data[globalKey]) as PICK_DATA<T>;
      }
      _setData = setData;

      return data;
    },
    useLoading: (): boolean => {
      const [loading, setLoading] = React.useState(false);

      if (globalKey) {
        _toggleLoading = (isLoading: boolean) => apiDataStore.reducers.setLoading(globalKey, isLoading);
        return apiDataStore.useStore((s) => !!s.loading[globalKey]);
      }
      _toggleLoading = setLoading;

      return loading;
    },
    useState: (): [PICK_DATA<T> | null, boolean] => {
      const [loading, setLoading] = React.useState(false);
      const [data, setData] = React.useState(null);

      if (globalKey) {
        _toggleLoading = (isLoading: boolean) => apiDataStore.reducers.setLoading(globalKey, isLoading);
        _setData = (d: PICK_DATA<T>) => apiDataStore.reducers.setData(globalKey, d);
        return apiDataStore.useStore((s) => [s.data[globalKey], !!s.loading[globalKey]]) as [PICK_DATA<T>, boolean];
      }
      _toggleLoading = setLoading;
      _setData = setData;

      return [data, loading];
    },
    getData: () => {
      return globalKey ? apiDataStore.getState((s) => s.data[globalKey]) : undefined;
    },
    clearData: () => {
      return globalKey ? apiDataStore.reducers.setData(globalKey, undefined) : undefined;
    },
  });
}

/**
 * get load function which can call directly with partial query
 * @param service api function
 * @param required required params, service will not be called if any of these is null or undefined
 * @param initial initial params
 * @returns if required params is all valid, return service result, otherwise return void;
 */
export function usePaging<T extends FN>({
  service,
  required,
  initial,
}: {
  service: T;
  required: Partial<Parameters<T>[0]>;
  initial?: Partial<Parameters<T>[0]>;
}) {
  const staticQuery = React.useRef({ pageNo: 1, pageSize: DEFAULT_PAGESIZE, ...initial });

  return React.useCallback(
    (query?: Partial<Parameters<T>[0]>) => {
      staticQuery.current = { ...staticQuery.current, ...query };
      const full = { ...required, ...staticQuery.current };
      const isReady = Object.keys(required).every((k) => full[k] !== null && full[k] !== undefined);
      if (isReady) {
        return service(full);
      }
    },
    [required, service],
  );
}

/**
 * Create a service function which have some hooks as the function property, these hooks can use in component directly
 * @example
  const apis = {
    addOrg: {
      api: 'post@/api/org/:orgId',
      errorMsg: 'call api failed',
      mock(params) { return { orgName: 'erda' } },
      successMsg: 'call api success',
      globalKey: 'globalOrg',
    },
  };

  export const addOrg = apiCreator<(p: ORG.CreateOrgBody) => ORG.Detail>(apis.addOrg);

  // in component
  React.useEffect(() => {
    addOrg.fetch();
  }, [])
  const [data, loading] = addOrg.useState(); // useState = useData + useLoading

  @tip
 * After set globalKey, use `getData` to get global state out of component.
 * use `clearData` to clear global state.
 */
export function apiCreator<T extends FN>(apiConfig: APIConfig<T>) {
  const apiFn = genRequest<T>(apiConfig);
  return enhanceAPI<typeof apiFn>(apiFn, apiConfig);
}

export { axios };
