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

const hub = {};
const init = {}; // preserve emitted data before register listener
const maxListeners = 20;

export function on(type: string, cb: Function, clearInit = true) {
  if (init[type] !== undefined) {
    cb(init[type]);
    clearInit && (init[type] = undefined);
  }
  hub[type] = (hub[type] || []).concat(cb);
  if (hub[type].length > maxListeners) {
    console.warn(`[eventHub warn] exceed maxListeners number: ${maxListeners}`);
  }
  return () => off(type, cb);
}

export function emit(type: string, data: any) {
  if (!hub[type]) {
    init[type] = data;
    return;
  }
  hub[type].forEach((cb: Function) => cb(data));
}

export function off(type: string, cb: Function) {
  hub[type] = hub[type].filter((item: Function) => item !== cb);
}
