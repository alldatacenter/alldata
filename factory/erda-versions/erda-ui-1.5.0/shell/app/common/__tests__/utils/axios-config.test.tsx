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

import { initAxios } from 'common/utils/axios-config';
import { setGlobal } from 'app/global-space';
import axios from 'axios';

describe('initAxios', () => {
  beforeEach(() => {
    initAxios();
    setGlobal('service-provider', 'ERDA');
  });
  afterEach(() => {
    setGlobal('service-provider', undefined);
  });
  it('request interceptors should work well', () => {
    const request = axios.interceptors.request.handlers || [];
    const { fulfilled, rejected } = request[0];
    const data = fulfilled({
      headers: {},
      method: 'POST',
      url: '/api/spot/aaa',
    });
    expect(data).toStrictEqual({
      headers: {
        Accept: 'application/vnd.dice+json;version=1.0',
        Lang: 'en-US',
        'OPENAPI-CSRF-TOKEN': 'OPENAPI-CSRF-TOKEN',
        org: 'erda',
        'service-provider': 'ERDA',
      },
      method: 'POST',
      url: '/api/erda/spot/aaa',
    });
    expect(rejected('err')).rejects.toBe('err');
  });
  it('request interceptors should work well', async () => {
    global.URL.createObjectURL = jest.fn();
    global.URL.revokeObjectURL = jest.fn();
    axios.get = jest.fn().mockResolvedValue({
      data: {
        url: '/login',
      },
    });
    jest.useFakeTimers();
    const response = axios.interceptors.response.handlers || [];
    const { fulfilled, rejected } = response[0];
    const res = fulfilled({
      config: { responseType: 'blob' },
      headers: {
        'content-disposition': '',
      },
      data: {
        data: {
          list: null,
          total: 0,
        },
      },
    });
    jest.runAllTimers();
    expect(res).toStrictEqual({
      config: { responseType: 'blob' },
      headers: { 'content-disposition': '' },
      data: { data: { list: [], total: 0 } },
    });
    const error = await rejected({ response: { status: 401 } }).catch((e) => e);
    expect(location.href).toBe('/login');
    expect(error).toStrictEqual({ response: { status: 401 } });
  });
});
