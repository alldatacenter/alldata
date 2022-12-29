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
import produce, { Draft } from 'immer';

export interface Action<T> {
  ancestors: number[];
  value: T;
}

export function updateBy<T>(base: T, updater: (draft: Draft<T>) => void) {
  return produce(base, draft => {
    updater(draft);
  });
}

export function addTo<T1>(base: T1[], value: T1): T1[] {
  return produce(base, draft => {
    draft.push(value as any);
  });
}

export function addByKey<T1, T2>(base: T1[], key: string, value: T2): T1[] {
  return produce(base, draft => {
    draft[key].push(value);
  });
}

export function updateByKey<T1, T2>(
  base: T1,
  key: string | number | symbol,
  value: T2,
): T1 {
  return produce(base, draft => {
    draft[key] = value;
  });
}

export function updateCollectionByAction<T extends ChartStyleConfig>(
  base: T[],
  action: Action<T>,
) {
  const value = action.value;
  const keys = (action?.ancestors && [...action.ancestors]) || [];
  const nextState = produce(base, draft => {
    const index = keys.shift() as number;
    if (index !== undefined) {
      if (keys.length > 0) {
        recursiveUpdateImpl(draft[index], keys, value as any);

        return;
      }
      draft[index] = value as any;
    }
  });
  return nextState;
}

export function updateByAction<T extends ChartStyleConfig>(
  base: T,
  action: Action<T>,
) {
  const value = action.value;
  const keys = (action?.ancestors && [...action.ancestors]) || [];

  const nextState = produce(base, draft => {
    recursiveUpdateImpl(draft, keys, value as any);
  });
  return nextState;
}

export function recursiveUpdateImpl<T extends ChartStyleConfig>(
  draft: T,
  keys: number[],
  value: T,
) {
  if (!keys || keys.length === 0) {
    draft = value;
    return;
  }
  if (draft.rows) {
    if (keys.length === 1) {
      const index = keys[0];
      draft.rows[index] = value;
      return;
    } else if (keys.length > 1) {
      const index = keys.shift() as number;
      recursiveUpdateImpl(draft.rows[index], keys, value);
      return;
    }
  }
}
