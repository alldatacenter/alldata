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

import { useLayoutEffect, useState } from 'react';
import useResizeObserver from './useResizeObserver';

export const useCacheWidthHeight = (arg?: {
  initW?: number;
  initH?: number;
  refreshMode?: 'throttle' | 'debounce';
  refreshRate?: number;
}) => {
  const { initW, initH, refreshMode, refreshRate } = arg || {};
  const [cacheW, setCacheW] = useState(initW ?? 1);
  const [cacheH, setCacheH] = useState(initH ?? 1);
  const {
    ref,
    width = initW ?? 1,
    height = initH ?? 1,
  } = useResizeObserver<HTMLDivElement>({
    refreshMode: refreshMode ?? 'debounce',
    refreshRate: refreshRate ?? 20,
  });
  useLayoutEffect(() => {
    if (width > 0) {
      setCacheW(width);
      setCacheH(height);
    }
  }, [width, height]);
  return {
    cacheWhRef: ref,
    cacheW,
    cacheH,
  };
};
