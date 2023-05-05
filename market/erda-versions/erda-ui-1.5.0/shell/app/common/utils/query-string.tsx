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

import queryString from 'query-string';
import { goTo, IOptions as IGotoOptions } from './go-to';
import { pick } from 'lodash';

// 设置默认option，区分a=b和a[]=b的情况，也为了避免影响依赖中的queryString
const option: queryString.StringifyOptions = { arrayFormat: 'bracket' };

// 原有API

/**
 * Parse a query string into an object. Leading `?` or `#` are ignored, so you can pass `location.search` or `location.hash` directly.
 *
 * The returned object is created with [`Object.create(null)`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/create) and thus does not have a `prototype`.
 *
 * @param query - The query string to parse.
 */
function parse(queryStr: string, opt: object = {}) {
  return queryString.parse(queryStr, { ...option, ...opt });
}

/**
 * Stringify an object into a query string and sorting the keys.
 */
function stringify(obj: { [key: string]: unknown }, opt: object = {}) {
  return queryString.stringify(obj, { ...option, ...opt });
}

/**
 * Extract the URL and the query string as an object.
 *
 * @param url - The URL to parse.
 *
 * @example
 *
 * queryString.parseUrl('https://foo.bar?foo=bar');
 * //=> {url: 'https://foo.bar', query: {foo: 'bar'}}
 */
function parseUrl(url: string, opt: object = {}) {
  return queryString.parseUrl(url, { ...option, ...opt });
}

export const qs = {
  parse,
  stringify,
  parseUrl,
  extract: queryString.extract,
};

// 扩展API
/**
 * 返回和当前url中query合并后的query对象或字符串
 * @param obj 需合并的query对象
 * @param toString 是否返回string
 * @param ignoreOrigin 是否忽略原始的query参数
 */
export function mergeSearch(obj?: { [prop: string]: any }, toString?: boolean, ignoreOrigin?: boolean, opt?: Obj) {
  const res = { ...(opt?.ignoreOrigin || ignoreOrigin ? {} : parse(location.search)), ...obj };
  return toString ? stringify(res, opt) : res;
}

/**
 * 合并参数并更新到当前url上
 * @param obj 需合并的query对象
 */

interface IOpt {
  gotoOption?: IGotoOptions;
  [pro: string]: any;
}
export function updateSearch(search?: { [prop: string]: any }, opt?: IOpt) {
  const { gotoOption = {}, ...rest } = opt || {};
  goTo(`${location.pathname}?${mergeSearch(search, true, false, rest)}`, gotoOption);
}

/**
 * 设置参数到当前url上(非合并)
 * @param search query对象
 * @param keepParams 需要保留的query属性
 * @param replace 是否用replace跳转
 */
export function setSearch(search?: { [prop: string]: any }, keepParams: string[] = [], replace?: boolean) {
  const keepQuery = pick(parse(location.search), keepParams);
  goTo(`${location.pathname}?${mergeSearch({ ...keepQuery, ...search }, true, true)}`, { replace });
}
