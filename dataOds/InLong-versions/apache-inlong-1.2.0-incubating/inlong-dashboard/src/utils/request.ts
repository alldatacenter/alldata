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

import { message as Message } from 'antd';
import { extend } from 'umi-request';
import { RequestOptionsInit } from 'umi-request/types';
import nprogress from 'nprogress';
import requestConcurrentMiddleware from './requestConcurrentMiddleware';

export interface FetchOptions extends RequestOptionsInit {
  url: string;
  fetchType?: 'FETCH' | 'JSONP';
}

export interface RequestOptions extends FetchOptions {
  // Do not use global request error prompt (http request is successful, but the returned result has a backend error)
  noGlobalError?: boolean;
}

export const apiPrefix = '/api/inlong/manager';

const extendRequest = extend({
  prefix: apiPrefix,
  timeout: 60 * 1000,
});

extendRequest.use(requestConcurrentMiddleware);

const fetch = (options: FetchOptions) => {
  const { method = 'get', url, fetchType = 'FETCH' } = options;

  if (fetchType === 'JSONP') {
    // return new Promise((resolve, reject) => {
    //   fetchJsonp(url)
    //     .then((res) => res.json())
    //     .then((result) => {
    //       resolve({ statusText: 'OK', status: 200, data: result });
    //     })
    //     .catch((error: Error) => {
    //       reject(error);
    //     });
    // });
  }

  const config = { ...options };
  delete config.url;
  delete config.fetchType;

  return extendRequest(url, {
    method,
    ...config,
  });
};

export default function request(_options: RequestOptions | string) {
  const options = typeof _options === 'string' ? { url: _options } : _options;
  nprogress.start();
  return fetch(options)
    .then((response: any) => {
      const { success, data, errMsg: message } = response;

      // Request 200, but the result is wrong
      if (!success) {
        if (options.noGlobalError) {
          return Promise.resolve(response);
        }

        return Promise.reject(new Error(message));
      }

      return Promise.resolve(data);
    })
    .catch(error => {
      const { data, message } = error;
      let msg = message;

      if (data && data instanceof Object) {
        // 404, 500, ...
        const { status, message } = data;
        if (status === 403) {
          localStorage.removeItem('userName');
          window.location.href = '/';
        }
        msg = message || status;
      }

      if (msg) Message.error(msg);
      return Promise.reject(new Error(msg));
    })
    .finally(() => {
      nprogress.done();
    });
}
