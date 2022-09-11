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

export function isDevelopEnv() {
  if (process.env.NODE_ENV === 'development') {
    return true;
  }
  return false;
}

export function hyphenToHump(str: string, flag = '-'): string {
  return str.replace(new RegExp(`${flag}(\\w)`, 'g'), (...args) => args[1].toUpperCase());
}

export function humpToHyphen(str: string, flag = '-'): string {
  return str.replace(/([A-Z])/g, `${flag}$1`).toLowerCase();
}

export function toLowerCaseFirst(str: string): string {
  return `${str[0].toLowerCase()}${str.slice(1)}`;
}

export function dateFormat(date: Date, format = 'yyyy-MM-dd hh:mm:ss'): string {
  const o: {
    [key: string]: any;
  } = {
    'M+': date.getMonth() + 1,
    'd+': date.getDate(),
    'h+': date.getHours(),
    'H+': date.getHours(),
    'm+': date.getMinutes(),
    's+': date.getSeconds(),
    'q+': Math.floor((date.getMonth() + 3) / 3),
    S: date.getMilliseconds(),
  };
  if (/(y+)/.test(format)) {
    format = format.replace(RegExp.$1, `${date.getFullYear()}`.substr(4 - RegExp.$1.length));
  }
  for (const k in o) {
    if (new RegExp(`(${k})`).test(format)) {
      format = format.replace(
        RegExp.$1,
        RegExp.$1.length === 1 ? o[k] : `00${o[k]}`.substr(`${o[k]}`.length),
      );
    }
  }
  return format;
}

export function timestampFormat(timestamp: number, format = 'yyyy-MM-dd hh:mm:ss') {
  const date = new Date(timestamp);
  return dateFormat(date, format);
}

/**
 * Get the value of the object's deep chained property
 * @param {object} obj
 * @param {string} keyChain example:'user.name' (another:'users[0].name')
 * @return {any} example: return obj.user.name
 */
export function getDeepObjectAttr(obj: Record<string, any>, keyChain: string): unknown {
  if (keyChain[0] === '[') keyChain = keyChain.slice(1);
  return keyChain.split(/\.|\[|\]\./).reduce((nestedObj, key) => {
    if (nestedObj && nestedObj[key]) {
      return nestedObj[key];
    }
    return undefined;
  }, obj);
}

/**
 * Trim plainData
 * Suppose object array|object|string, removing leading and trailing spaces
 * @param {plain} plainData Simple data, non-function/recursion, etc.
 */
export function trim(plainData: any): unknown {
  if (Array.isArray(plainData)) {
    return plainData.map(trim);
  }
  if (Object.prototype.toString.call(plainData) === '[object Object]') {
    return Object.keys(plainData).reduce(
      (acc, key) => ({
        ...acc,
        [key]: trim(plainData[key]),
      }),
      {},
    );
  }
  if (typeof plainData === 'string') {
    return plainData.trim();
  }
  return plainData;
}

/**
 * Filter Object, example: {a, b, c} => {a, b}
 */
export function filterObj(obj: Record<string, any>, keyList: string[]) {
  return keyList.reduce((acc, key) => {
    if (obj[key] !== undefined) {
      return {
        ...acc,
        [key]: obj[key],
      };
    }
    return acc;
  }, {});
}

/**
 * Filter parts from the object array to form a new array
 * @param keys
 * @param sourceArr
 * @param key Flag
 */
export function pickObjectArray(keys = [], sourceArr = [], key = 'name') {
  const map = new Map(sourceArr.map(item => [item[key], item]));
  if (isDevelopEnv()) {
    // Increase the log in the development environment to facilitate debugging
    return keys.reduce((acc, k) => {
      if (typeof k !== 'string') {
        return acc.concat(k);
      } else if (map.has(k)) {
        return acc.concat(map.get(k));
      }
      console.warn(`[Utils] pickObjectArray lost: ${k}`);
      return acc;
    }, []);
  } else {
    return keys.map(k => (typeof k !== 'string' ? k : map.get(k)));
  }
}

/**
 * Filter the object key and return a new object composed of the specified keys
 */
export function pickObject(keys = [], sourceObj) {
  const set = new Set(keys);
  return Object.entries(sourceObj).reduce((acc, [key, value]) => {
    if (set.has(key)) {
      return {
        ...acc,
        [key]: value,
      };
    }
    return acc;
  }, {});
}

export function excludeObject(keys = [], sourceObj: Record<string, unknown>) {
  const set = new Set(keys);
  return Object.entries(sourceObj).reduce((acc, [key, value]) => {
    if (!set.has(key)) {
      return {
        ...acc,
        [key]: value,
      };
    }
    return acc;
  }, {});
}

/**
 * Exclude parts from the object array to form a new array
 * @param keys
 * @param sourceArr
 * @param key Flag
 */
export function excludeObjectArray(keys = [], sourceArr = [], key = 'name') {
  const set = new Set(keys);
  return sourceArr.filter(item => !set.has(item[key]));
}

/**
 * Get string byte length
 * @param {string} str
 * @return {number}
 */
export function getStrByteLen(str: string): number {
  let len = 0;
  for (let i = 0; i < str.length; i++) {
    const c = str.charAt(i);
    if (/^[\u4e00-\u9fa5]$/.test(c)) {
      len += 2;
    } else {
      len += 1;
    }
  }
  return len;
}
