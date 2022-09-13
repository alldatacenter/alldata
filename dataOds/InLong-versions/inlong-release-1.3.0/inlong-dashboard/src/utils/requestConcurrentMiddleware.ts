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

/**
 * Concurrent request middleware
 *   Only for GET
 *   For the same request, the previous request did not get the result when the next request is sent,
 *   and the next request will directly use the return of the previous request
 */
import { stringify } from 'qs';

const concurrentData: Record<string, Function[]> = {};

const emit = (url, ...rest) => {
  concurrentData[url].forEach(handler => handler(...rest));
  delete concurrentData[url];
};

const on = url => {
  return new Promise(resolve => {
    concurrentData[url].push(data => {
      resolve(data);
    });
  });
};

// eslint-disable-next-line
export default async (ctx, next) => {
  const { url, options } = ctx.req;

  const urlKey = `${url}?${stringify(options?.params)}`;

  if (options?.method?.toUpperCase() !== 'GET') {
    await next();
    return;
  }

  if (concurrentData[urlKey]) {
    ctx.res = await on(urlKey);
  } else {
    concurrentData[urlKey] = [];
    await next();
    emit(urlKey, ctx.res);
  }
};
