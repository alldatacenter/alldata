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

import { DatePicker } from 'antd';
import { FilterConditionType } from 'app/constants';
import { ConditionBuilder } from 'app/models/ChartFilterCondition';
import {
  formatTime,
  getTime,
  recommendTimeRangeConverter,
} from 'app/utils/time';
import { TIME_FORMATTER } from 'globalConstants';
import moment from 'moment';
import { FC, memo, useMemo } from 'react';
import { PresentControllerFilterProps } from '.';
const { RangePicker } = DatePicker;

const toMoment = t => {
  if (!t) {
    return moment();
  }
  if (Boolean(t) && typeof t === 'object' && 'unit' in t) {
    const time = getTime(+(t.direction + t.amount), t.unit)(t.unit, t.isStart);
    return moment(formatTime(time, TIME_FORMATTER));
  }
  return moment(t);
};

const RangeTimePickerFilter: FC<PresentControllerFilterProps> = memo(
  ({ condition, onConditionChange }) => {
    const handleTimeChange = (moments, dateStrings) => {
      const filterRow = new ConditionBuilder(condition)
        .setValue(dateStrings)
        .asRangeTime();
      onConditionChange?.(filterRow);
    };

    const rangeTimes = useMemo(() => {
      if (condition?.type === FilterConditionType.RangeTime) {
        const startTime = toMoment(condition?.value?.[0]);
        const endTime = toMoment(condition?.value?.[1]);
        return [startTime, endTime];
      }
      if (condition?.type === FilterConditionType.RecommendTime) {
        return recommendTimeRangeConverter(condition?.value)?.map(toMoment);
      }
      return [moment(), moment()];
    }, [condition?.type, condition?.value]);

    return (
      <RangePicker
        showTime
        value={rangeTimes as any}
        onChange={handleTimeChange}
      />
    );
  },
);

export default RangeTimePickerFilter;
