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

import { ChartDataViewFieldCategory } from 'app/constants';
import { DATE_LEVEL_DELIMITER } from 'globalConstants';
import { APP_VERSION_RC_0, APP_VERSION_RC_2 } from '../constants';
import MigrationEvent from '../MigrationEvent';
import MigrationEventDispatcher from '../MigrationEventDispatcher';

export const RC0 = config => {
  if (!config) {
    return config;
  }

  try {
    if (config?.computedFields) {
      config.computedFields = config.computedFields.map(v => {
        if (!v.name) {
          return {
            ...v,
            name: v.id,
          };
        }
        return v;
      });
    }

    return config;
  } catch (error) {
    console.error('Migration config Errors | RC.0 | ', error);
    return config;
  }
};

export const RC2 = config => {
  if (!config) {
    return config;
  }
  try {
    if (config?.chartConfig) {
      config.chartConfig?.datas.forEach(data => {
        data.rows?.forEach(row => {
          if (
            row.category ===
              ChartDataViewFieldCategory.DateLevelComputedField &&
            !row.colName.includes(DATE_LEVEL_DELIMITER)
          ) {
            const field = row.field || row.id?.split('（')?.[0];
            row.colName =
              field + DATE_LEVEL_DELIMITER + row.expression.split('(')[0];
          }
        });
      });
    }
    if (config?.computedFields) {
      const allRows = config.chartConfig?.datas.flatMap(v => v.rows || []);
      config.computedFields = config.computedFields.map(computedField => {
        if (
          computedField.category ===
            ChartDataViewFieldCategory.DateLevelComputedField &&
          !computedField.name.includes(DATE_LEVEL_DELIMITER)
        ) {
          const currenrRowFiledForComputed = allRows.find(
            row => row.expression === computedField.expression,
          );
          const field =
            currenrRowFiledForComputed?.field ||
            currenrRowFiledForComputed?.id?.split('（')?.[0];
          return {
            ...computedField,
            name:
              field +
              DATE_LEVEL_DELIMITER +
              computedField.expression.split('(')[0],
          };
        }
        return computedField;
      });
    }

    return config;
  } catch (error) {
    console.error('Migration config Errors | RC.2 | ', error);
    return config;
  }
};

const migrationChartConfig = (config: string): string => {
  if (!config) {
    return config;
  }
  const chartConfig = JSON.parse(config);
  const event = new MigrationEvent(APP_VERSION_RC_0, RC0);
  const event2 = new MigrationEvent(APP_VERSION_RC_2, RC2);
  const dispatcher = new MigrationEventDispatcher(event, event2);
  const result = dispatcher.process(chartConfig);

  return JSON.stringify(result);
};

export default migrationChartConfig;
