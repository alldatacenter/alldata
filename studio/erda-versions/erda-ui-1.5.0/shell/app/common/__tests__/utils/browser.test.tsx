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

import { getBrowserInfo, getCookies, getLS, setLS, removeLS, clearLS, LSObserver } from 'common/utils';

describe('browser', () => {
  afterEach(() => {
    window.localStorage.clear();
  });
  it('getBrowserInfo', () => {
    expect(getBrowserInfo()).toStrictEqual({
      isChrome: true,
      isFirefox: false,
      isIE: false,
      isOpera: false,
      isSafari: false,
      version: '90.0.4430.85',
    });
  });
  it('getCookies', () => {
    expect(getCookies()).toStrictEqual({ ID: '123', 'OPENAPI-CSRF-TOKEN': 'OPENAPI-CSRF-TOKEN', ORG: 'erda' });
    expect(getCookies('ID')).toBe('123');
  });
  it('getLS', () => {
    window.localStorage.setItem('ORG', '"erda"');
    expect(getLS('ORG')).toBe('erda');
  });
  it('setLS', () => {
    setLS('ORG.list', [1, 2]);
    expect(window.localStorage.getItem('ORG')).toBe('{"list":[1,2]}');
  });
  it('removeLS', () => {
    setLS('ORG.list', [1, 2]);
    setLS('id', 1);
    removeLS('ORG.list');
    removeLS('id');
    expect(window.localStorage.getItem('ORG')).toBe('{}');
    expect(window.localStorage.getItem('id')).toBeNull();
  });
  it('should clearLS', () => {
    setLS('ORG.list', [1, 2]);
    setLS('id', 1);
    clearLS();
    expect(window.localStorage.getItem('ORG')).toBeNull;
    expect(window.localStorage.getItem('id')).toBeNull();
  });

  it('should LSObserver', () => {
    const fn = jest.fn();
    LSObserver.watch('test', fn);
    LSObserver.notify('test', 123);
    expect(fn).toHaveBeenLastCalledWith(123);
    expect(fn).toHaveBeenCalledTimes(1);
    LSObserver.remove('test');
    LSObserver.notify('test', 123);
    expect(fn).toHaveBeenCalledTimes(1);
  });
});
