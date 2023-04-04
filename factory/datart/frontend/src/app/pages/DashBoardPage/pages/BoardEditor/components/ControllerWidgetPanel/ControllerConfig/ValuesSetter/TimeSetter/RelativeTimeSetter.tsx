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
import { Form, InputNumber, Select } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { TIME_DIRECTION, TIME_UNIT_OPTIONS } from 'globalConstants';
import { NamePath } from 'rc-field-form/lib/interface';
import React, { memo, useEffect, useState } from 'react';
import { RelativeDate } from '../../../types';
export interface RelativeTimeSetProps {
  relativeName: NamePath;
  value?: any;
  onChange?: (data: RelativeDate) => any;
}
export const RelativeTimeSetter: React.FC<RelativeTimeSetProps> = memo(
  ({ relativeName, ...props }) => {
    return (
      <>
        <Form.Item
          noStyle
          name={relativeName}
          shouldUpdate
          validateTrigger={['onChange', 'onBlur']}
          rules={[{ required: true, message: '' }]}
        >
          <RelateTimeInput relativeName={relativeName} {...props} />
        </Form.Item>
      </>
    );
  },
);

export const RelateTimeInput: React.FC<RelativeTimeSetProps> = ({
  value,
  onChange,
}) => {
  const filterDataT = useI18NPrefix('viz.common.filter.date');

  const [dateVal, setDateVal] = useState<RelativeDate>({
    direction: '+',
    amount: 1,
    unit: 'd',
  } as RelativeDate);

  useEffect(() => {
    setDateVal({ ...value });
  }, [value]);

  const onChangeDirection = (_val: RelativeDate['direction']) => {
    if (_val === '+0') {
      onChange?.({ ...dateVal, direction: _val, amount: 0 });
      return;
    }
    onChange?.({ ...dateVal, direction: _val });
  };
  const onChangeAmount = _val => {
    onChange?.({ ...dateVal, amount: _val });
  };
  const onChangeUnit = _val => {
    onChange?.({ ...dateVal, unit: _val });
  };
  return (
    <>
      <Select
        style={{ width: '80px' }}
        value={dateVal.direction}
        onChange={onChangeDirection}
      >
        {TIME_DIRECTION.map(item => {
          return (
            <Select.Option key={item.name} value={item.value}>
              {filterDataT(item.name)}
            </Select.Option>
          );
        })}
      </Select>

      {dateVal.direction !== '+0' && (
        <InputNumber
          step={1}
          min={0}
          value={dateVal.amount}
          onChange={onChangeAmount}
        />
      )}

      <Select
        style={{ width: '80px' }}
        value={dateVal.unit}
        onChange={onChangeUnit}
      >
        {TIME_UNIT_OPTIONS.map(item => (
          <Select.Option key={item.value} value={item.value}>
            {filterDataT(item.name)}
          </Select.Option>
        ))}
      </Select>
    </>
  );
};
