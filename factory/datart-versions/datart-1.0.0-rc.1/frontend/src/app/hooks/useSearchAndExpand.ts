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

import { TreeDataNode } from 'antd';
import { DEFAULT_DEBOUNCE_WAIT } from 'globalConstants';
import { useCallback, useMemo, useState } from 'react';
import { getExpandedKeys } from 'utils/utils';
import { useDebouncedSearch } from './useDebouncedSearch';

export function useSearchAndExpand<T extends TreeDataNode>(
  dataSource: T[] | undefined,
  filterFunc: (keywords: string, data: T) => boolean,
  wait: number = DEFAULT_DEBOUNCE_WAIT,
  filterLeaf: boolean = false,
) {
  const [expandedRowKeys, setExpandedRowKeys] = useState<string[]>([]);

  const { keywords, filteredData, debouncedSearch } = useDebouncedSearch(
    dataSource,
    filterFunc,
    wait,
    filterLeaf,
  );

  const filteredExpandedRowKeys = useMemo(
    () =>
      filteredData && keywords.trim() ? getExpandedKeys(filteredData) : [],
    [keywords, filteredData],
  );

  const onExpand = useCallback(expandedRowKeys => {
    setExpandedRowKeys(expandedRowKeys);
  }, []);

  return {
    keywords,
    filteredData,
    expandedRowKeys:
      filteredExpandedRowKeys.length > 0
        ? filteredExpandedRowKeys
        : expandedRowKeys,
    onExpand,
    debouncedSearch,
    setExpandedRowKeys,
  };
}
