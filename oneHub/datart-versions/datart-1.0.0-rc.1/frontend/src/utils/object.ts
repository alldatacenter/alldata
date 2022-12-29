/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { ChartStyleConfig } from 'app/types/ChartConfig';
import camelCase from 'lodash/camelCase';
import cloneDeep from 'lodash/cloneDeep';
import isEqual from 'lodash/isEqual';
import isFunction from 'lodash/isFunction';
import isObject from 'lodash/isObject';
import lowerCase from 'lodash/lowerCase';
import mean from 'lodash/mean';
import omit from 'lodash/omit';
import pick from 'lodash/pick';
import uniq from 'lodash/uniq';
import uniqWith from 'lodash/uniqWith';

type PipeFunction<TA, TO> = (accumulator?: TA, options?: TO) => TA | undefined;

export function pipe<T1, T2>(...fns: PipeFunction<T1, T2>[]) {
  return (v, o?) => (fns || []).reduce((y, f) => f(y, o), v);
}

export function curry(fn) {
  let _args: any[] = [];
  const collector = (...args) => {
    _args = _args.concat(args || []);
    if (_args.length < fn.length) {
      return collector.bind(Object.create(null));
    }
    return fn.apply(Object.create(null), _args);
  };
  return collector;
}

export function cond(...predicates) {
  return (value, defaultValue?) => {
    for (let i = 0; i < predicates.length; i++) {
      if (typeof predicates[i] === 'function' && predicates[i](value)) {
        return predicates[i](value);
      }
      if (
        isPairArray(predicates[i]) &&
        typeof predicates[i]?.[0] === 'function' &&
        predicates[i][0].call(Object.create(null), value)
      ) {
        if (typeof predicates[i]?.[1] === 'function') {
          return predicates[i][1].call(Object.create(null), value);
        }
        return predicates[i][1];
      }
    }
    return defaultValue;
  };
}

export function isPairArray(arr?: any[]) {
  return Array.isArray(arr) && arr?.length === 2;
}

export function isNumerical(n?: number | string) {
  return !isEmpty(n) && !isNaN(+n!);
}

export function isNumericEqual(a?: number | string, b?: number | string) {
  // eslint-disable-next-line eqeqeq
  return a == b;
}

export function isInPairArrayRange(
  count: number | string,
  pairArray: number[],
) {
  if (!isPairArray(pairArray)) {
    return false;
  }
  return pairArray[0] <= +count && +count <= pairArray[1];
}

export function PatchUpdate<T1, T2>(
  target: T1,
  collectionKey: string,
  unitMatcher: (c: T2) => boolean,
  item: T2,
): T1 {
  if (
    !target ||
    !target[collectionKey] ||
    !Array.isArray(target[collectionKey])
  ) {
    return target;
  }

  PatchUpdateCollection(target[collectionKey], unitMatcher, item);
  return target;
}

export function PatchUpdateCollection<T2>(
  target: T2[] = [],
  unitMatcher: (c: T2) => boolean,
  item: T2,
  updateByKey?: string,
): T2[] {
  const collection = target.find(unitMatcher);
  if (!collection) return target;

  if (updateByKey) {
    collection[updateByKey] = item[updateByKey];
  } else {
    Object.assign(collection, item);
  }

  return target;
}

export function shadowCopyCollection(collection) {
  return [...(collection || [])];
}

export function ArrayToObject(arr) {
  if (!arr) return {};

  return arr?.reduce((acc, cur) => {
    return Object.assign(cur, acc);
  }, {});
}

export function ObjectToArray(o) {
  if (!o) return [];

  return Object.values(o);
}

export function UniqArray<T>(arr: T[]) {
  return uniq(arr);
}

export function UniqWith<T>(arr: T[], compareFn: (a: T, b: T) => boolean) {
  return uniqWith(arr, compareFn);
}

export function Omit(object, keys: string[]) {
  return omit(object, keys);
}

export function ToCamelCase(str) {
  return camelCase(str);
}

export function ToLowerCase(str) {
  return lowerCase(str);
}

export function AssignDeep<T>(target: T, ...source: any[]) {
  return Object.assign({}, target, ...source);
}

export function mergeOptions<ChartStyleRowOption>(
  oldOptions?: ChartStyleRowOption,
  newOptions?: ChartStyleRowOption,
): ChartStyleRowOption {
  return Object.assign(oldOptions || {}, newOptions);
}

export function CloneValueDeep<T>(value: T): T {
  return cloneDeep(value);
}

export function isUndefined(o): boolean {
  return o === undefined;
}

export function isEmpty(o?: null | any): boolean {
  return o === null || isUndefined(o);
}

export function isEmptyString(o?: null | any): boolean {
  return isEmpty(o) || o === '';
}

export function isFunc(f) {
  return isFunction(f);
}

export function meanValue(value) {
  return mean(value);
}

export function isConfigRow(value) {
  return isObject(value) && 'comType' in value;
}

export function pickValues(o, values: string[]) {
  return pick(o, values);
}

export function StringInsert(source: string, value: string, index: number) {
  return source.slice(0, index) + value + source.slice(index);
}

export function IsKeyIn<T, K extends keyof T>(o: T, key: K): Boolean {
  return !!o && typeof o === 'object' && key in o;
}

export function mergeDefaultToValue(
  configs?: ChartStyleConfig[],
): ChartStyleConfig[] {
  return (configs || []).map(c => {
    if (c.comType !== 'group') {
      if (isEmpty(c.value) && !isEmpty(c.default)) {
        c.value = c.default;
      }
      // update(c, 'value', isEmpty(c.value) ? c.default : c.value);
    }
    // const newRows = initChartConfigValueByDefaultValue(c.rows);
    // update(c, 'rows', newRows);
    if (!!c?.rows?.length) {
      c.rows = mergeDefaultToValue(c.rows);
    }
    return c;
  });
}

export function cleanChartConfigValueByDefaultValue(
  configs?: ChartStyleConfig[],
): ChartStyleConfig[] {
  return (configs || []).filter(Boolean).map(c => {
    if (c.comType !== 'group') {
      c.value = c.default;
    }
    c.rows = cleanChartConfigValueByDefaultValue(c.rows);
    return c;
  });
}

export function resetValue(config: ChartStyleConfig): ChartStyleConfig {
  config.value = config.default;
  config.rows = config?.rows?.map(r => {
    return resetValue(r);
  });
  return config;
}

export function isTreeModel(data) {
  if (!Array.isArray(data)) {
    return false;
  }
  return data.some(d => d?.children?.length > 0);
}

export function isEmptyArray(value?) {
  if (isEmpty(value)) {
    return true;
  }

  return Array.isArray(value) && !value?.length;
}

export function isPromise(obj?) {
  if (isEmpty(obj)) {
    return false;
  }

  return (
    (typeof obj === 'object' || typeof obj === 'function') &&
    typeof obj.then === 'function'
  );
}

export function isEqualObject(obj, cObj) {
  return isEqual(obj, cObj);
}
