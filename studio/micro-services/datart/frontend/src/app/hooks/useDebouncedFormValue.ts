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

import debounce from 'lodash/debounce';
import { useMemo, useState } from 'react';

export default function useDebouncedFormValue<T>(
  initValue: T,
  options: {
    ancestors: number[];
    needRefresh?: boolean;
    delay: number;
  },
  onChange?: (ancestors: number[], value: T, needRefresh?: boolean) => void,
) {
  const [cachedValue, setCachedValue] = useState(initValue);

  const valueChange = newValue => {
    setCachedValue(newValue);
    debouncedValueChange(newValue);
  };

  const debouncedValueChange = useMemo(
    () =>
      debounce(newValue => {
        onChange?.(options.ancestors, newValue, options?.needRefresh);
      }, options.delay),
    [options.ancestors, options.delay, onChange, options?.needRefresh],
  );

  return [cachedValue, valueChange];
}
