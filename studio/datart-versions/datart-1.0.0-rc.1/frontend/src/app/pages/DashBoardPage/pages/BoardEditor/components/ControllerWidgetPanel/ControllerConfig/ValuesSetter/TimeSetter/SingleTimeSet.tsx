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
import { DatePicker, FormItemProps } from 'antd';
import moment from 'moment';
import React, { memo } from 'react';
import { PickerType } from '../../../types';
import { formatDateByPickType } from '../../../utils';
export interface SingleTimeSetProps extends FormItemProps<any> {
  pickerType: PickerType;
  value?: any;
  onChange?: any;
}
export const SingleTimeSet: React.FC<SingleTimeSetProps> = memo(
  ({ pickerType, value, onChange }) => {
    function _onChange(date, dateString) {
      if (!date) {
        onChange?.(date);
        return;
      }
      const value = formatDateByPickType(pickerType, date);

      onChange?.(moment(value));
    }
    return (
      <>
        {pickerType === 'dateTime' ? (
          <DatePicker
            value={value}
            allowClear={true}
            showTime
            onChange={_onChange}
          />
        ) : (
          <DatePicker
            value={value}
            allowClear={true}
            onChange={_onChange}
            picker={pickerType as any}
          />
        )}
      </>
    );
  },
);
