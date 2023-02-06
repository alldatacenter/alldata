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

import { createStore, use } from '../cube';

const loadingStore = createStore({
  name: 'loading',
  state: {} as Record<string, Record<string, boolean>>,
  reducers: {
    setLoading(state, storeName: string, effectName, status: boolean) {
      state[storeName] = state[storeName] || {};
      state[storeName][effectName] = status;
    },
  },
});

export type ValueOf<T extends Record<string, any>, K> = K extends keyof T ? T[K] : never;
export type EffectKeys<T> = {
  [K in keyof T]: boolean;
};

export type EKs<T> = keyof EffectKeys<ValueOf<T, 'effects'> | ValueOf<T, '_effects'>>;
export function useLoading<T>(store: T & { name: string }, effectNames: Array<EKs<T>>): boolean[] {
  return loadingStore.useStore((s) => effectNames.map((n: EKs<T>) => (s[store.name] && s[store.name][n]) || false));
}

use({
  beforeEffect({ storeName, effectName }) {
    loadingStore.reducers.setLoading(storeName, effectName, true);
  },
  afterEffect({ storeName, effectName }) {
    loadingStore.reducers.setLoading(storeName, effectName, false);
  },
});

export default loadingStore;
