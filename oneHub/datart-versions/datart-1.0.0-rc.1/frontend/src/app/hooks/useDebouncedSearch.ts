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

import { DEFAULT_DEBOUNCE_WAIT } from 'globalConstants';
import debounce from 'lodash/debounce';
import { useMemo, useState } from 'react';
import { filterListOrTree } from 'utils/utils';

export function useDebouncedSearch<T>(
  dataSource: T[] | undefined,
  filterFunc: (keywords: string, data: T) => boolean,
  wait: number = DEFAULT_DEBOUNCE_WAIT,
  filterLeaf: boolean = false,
) {
  const [keywords, setKeywords] = useState('');
  const filteredData = useMemo(
    () =>
      dataSource && keywords.trim()
        ? filterListOrTree(dataSource, keywords, filterFunc, filterLeaf)
        : dataSource,
    [dataSource, keywords, filterFunc, filterLeaf],
  );

  const debouncedSearch = useMemo(() => {
    const search = e => {
      setKeywords(e.target.value);
    };
    return debounce(search, wait);
  }, [wait]);

  return {
    keywords,
    filteredData,
    debouncedSearch,
  };
}
