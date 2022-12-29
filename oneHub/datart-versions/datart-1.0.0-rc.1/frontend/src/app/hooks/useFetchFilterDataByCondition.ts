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

import { FilterConditionType } from 'app/constants';
import { ExecuteToken } from 'app/pages/SharePage/slice/types';
import { FilterCondition, RelationFilterValue } from 'app/types/ChartConfig';
import { ChartDTO } from 'app/types/ChartDTO';
import { getDistinctFields } from 'app/utils/fetch';
import useMount from './useMount';

export const useFetchFilterDataByCondition = (
  viewId?: string,
  condition?: FilterCondition,
  onFinish?: (datas: RelationFilterValue[]) => void,
  view?: ChartDTO['view'],
  executeToken?: Record<string, ExecuteToken>,
) => {
  useMount(() => {
    if (!viewId || condition?.type !== FilterConditionType.List) {
      return;
    }

    getDistinctFields?.(viewId, [condition?.name!], view, executeToken)?.then(
      dataset => {
        const _convertToList = collection => {
          const items: string[] = (collection || []).flatMap(c => c);
          const uniqueKeys = Array.from(new Set(items));
          return uniqueKeys.map(item => ({
            key: item,
            label: item,
          }));
        };
        onFinish?.(_convertToList(dataset?.rows));
      },
    );
  });
};

export default useFetchFilterDataByCondition;
