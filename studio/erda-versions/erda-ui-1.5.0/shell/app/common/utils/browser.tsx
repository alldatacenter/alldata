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

export function getBrowserInfo() {
  const userAgent = navigator.userAgent.toLowerCase();
  const match = userAgent.match(/(?:firefox|opera|safari|chrome|msie)[/: ]([\d.]+)/);
  return {
    version: match ? match[1] : null,
    isSafari: /version.+safari/.test(userAgent),
    isChrome: /chrome/.test(userAgent),
    isFirefox: /firefox/.test(userAgent),
    isIE: /msie/.test(userAgent),
    isOpera: /opera/.test(userAgent),
  };
}

/**
 * 获取cookies对象，传入key时获取单个字段
 * @param 需要获取的cookie key
 */
export function getCookies(key?: string) {
  const cookies = {};
  window.document.cookie.split(';').forEach((item) => {
    const [k, v] = item.split('=');
    cookies[k.trim()] = v && v.trim();
  });
  return key ? cookies[key] : cookies;
}

// setLS('a.arr', [1,2]);
export function setLS(keyPath: string, value: any) {
  const LSPath = keyPath.split('.');
  const LSKey: string = LSPath.splice(0, 1)[0];
  const result = localStorage.getItem(LSKey);
  let json = result ? JSON.parse(result) : {};
  if (LSPath.length) {
    const key = LSPath.splice(-1)[0];
    const target = LSPath.reduce((pre, next) => pre[next], json);
    if (Array.isArray(target) && !Number.isInteger(+key)) {
      throw new TypeError(`can not set attr '${key}' to Array '${target}'`);
    }
    target[key] = value;
  } else {
    json = value;
  }
  localStorage.setItem(LSKey, JSON.stringify(json));
}

export function getLS(keyPath: string) {
  const LSPath = keyPath.split('.');
  const LSKey = LSPath.splice(0, 1)[0];
  const saved = localStorage.getItem(LSKey);
  const json = saved ? JSON.parse(saved) : {};
  const result = LSPath.reduce((pre, next) => pre[next], json);
  return result;
}

export function removeLS(keyPath: string) {
  const LSPath = keyPath.split('.');
  const LSKey = LSPath.splice(0, 1)[0];
  if (LSPath.length) {
    const saved = localStorage.getItem(LSKey);
    const json = saved ? JSON.parse(saved) : {};
    const key = LSPath.splice(-1)[0];
    const target = LSPath.reduce((pre, next) => pre[next], json);
    delete target[key];
    localStorage.setItem(LSKey, JSON.stringify(json));
  } else {
    localStorage.removeItem(LSKey);
  }
}

export function clearLS() {
  localStorage.clear();
}

type LSWatcher = (value: any) => void;
export const LSObserver = (() => {
  const obMap = {} as any;
  return {
    watch(name: string, fn: LSWatcher) {
      obMap[name] = fn;
    },
    remove(name: string) {
      obMap[name] = undefined;
    },
    notify(name: string | null, val: any) {
      if (name) {
        if (!name || !obMap[name]) return;
        const fun = obMap[name];
        typeof fun === 'function' && fun(val);
      }
    },
  };
})();

// 监听localstorage变化
window.addEventListener('storage', (event) => {
  LSObserver.notify(event.key, event.newValue);
});
