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

import { ChartDataSectionType } from 'app/constants';
import { ChartDataConfigSectionProps } from 'app/types/ChartDataConfigSection';
import produce from 'immer';
export function dataConfigSectionComparer(
  prevProps: ChartDataConfigSectionProps,
  nextProps: ChartDataConfigSectionProps,
) {
  if (
    prevProps.translate !== nextProps.translate ||
    prevProps.config !== nextProps.config ||
    prevProps.aggregation !== nextProps.aggregation ||
    prevProps.expensiveQuery !== nextProps.expensiveQuery
  ) {
    return false;
  }
  return true;
}

export function handleDefaultConfig(defaultConfig, configType): any {
  const nextConfig = produce(defaultConfig, draft => {
    let _actions = {};

    draft.rows?.forEach((row, i) => {
      draft.rows[i].aggregate = undefined;
    });

    if (configType === ChartDataSectionType.Aggregate) {
      delete draft.actions.STRING;
    }

    if (configType === ChartDataSectionType.Group) {
      delete draft.actions.NUMERIC;
    }

    for (let key in draft.actions) {
      _actions[key] = draft.actions[key].filter(
        v => v !== 'aggregate' && v !== 'aggregateLimit',
      );
    }

    draft.actions = _actions;
  });
  return nextConfig;
}
