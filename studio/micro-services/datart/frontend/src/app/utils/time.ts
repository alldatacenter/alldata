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

import { DataViewFieldType } from 'app/constants';
import { ChartDataRequestFilter } from 'app/types/ChartDataRequest';
import {
  FilterSqlOperator,
  RECOMMEND_TIME,
  TIME_FORMATTER,
} from 'globalConstants';
import moment, { Moment, unitOfTime } from 'moment';

export function getTimeRange(
  amount?: [number, number],
  unit?,
): (unitTime, dateFormat?) => [string, string] {
  return (unitOfTime, dateFormat?) => {
    const startTime = moment().add(amount?.[0], unit).startOf(unitOfTime);
    const endTime = moment().add(amount?.[1], unit).endOf(unitOfTime);
    return [
      startTime.format(dateFormat || TIME_FORMATTER),
      endTime.format(dateFormat || TIME_FORMATTER),
    ];
  };
}

export function getTime(
  amount?: number | string,
  unit?: unitOfTime.DurationConstructor,
): (unitTime, isStart?: boolean) => Moment {
  return (unitOfTime: unitOfTime.StartOf, isStart?: boolean) => {
    if (!!isStart) {
      return moment().add(amount, unit).startOf(unitOfTime);
    }
    return moment().add(amount, unit).add(1, unit).startOf(unitOfTime);
  };
}

export function formatTime(time: string | Moment, format?): string {
  return moment(time).format(format || TIME_FORMATTER);
}

export function recommendTimeRangeConverter(relativeTimeRange, dateFormat?) {
  let timeRange = getTimeRange()('d', dateFormat);
  switch (relativeTimeRange) {
    case RECOMMEND_TIME.TODAY:
      break;
    case RECOMMEND_TIME.YESTERDAY:
      timeRange = getTimeRange([-1, 0], 'd')('d', dateFormat);
      break;
    case RECOMMEND_TIME.THIS_WEEK:
      timeRange = getTimeRange()('W', dateFormat);
      break;
    case RECOMMEND_TIME.LAST_7_DAYS:
      timeRange = getTimeRange([-7, 0], 'd')('d', dateFormat);
      break;
    case RECOMMEND_TIME.LAST_30_DAYS:
      timeRange = getTimeRange([-30, 0], 'd')('d', dateFormat);
      break;
    case RECOMMEND_TIME.LAST_90_DAYS:
      timeRange = getTimeRange([-90, 0], 'd')('d', dateFormat);
      break;
    case RECOMMEND_TIME.LAST_1_MONTH:
      timeRange = getTimeRange()('M', dateFormat);
      break;
    case RECOMMEND_TIME.LAST_1_YEAR:
      timeRange = getTimeRange()('y', dateFormat);
      break;
  }

  return timeRange;
}

export const splitRangerDateFilters = (filters: ChartDataRequestFilter[]) => {
  if (!Array.isArray(filters)) return [];
  const newFilter = [] as ChartDataRequestFilter[];
  filters.forEach(filter => {
    let isTargetFilter = false;
    if (
      filter.sqlOperator === FilterSqlOperator.Between &&
      filter.values?.[0].valueType === DataViewFieldType.DATE
    ) {
      isTargetFilter = true;
    }
    if (!isTargetFilter) {
      newFilter.push(filter);
      return;
    }
    // split date range filters
    if (filter.values?.[0] && filter.values?.[1]) {
      const start: ChartDataRequestFilter = {
        ...filter,
        sqlOperator: FilterSqlOperator.GreaterThanOrEqual,
        values: [filter.values?.[0]],
      };
      const end: ChartDataRequestFilter = {
        ...filter,
        sqlOperator: FilterSqlOperator.LessThan,
        values: [filter.values?.[1]],
      };
      newFilter.push(start, end);
    }
  });
  return newFilter;
};
