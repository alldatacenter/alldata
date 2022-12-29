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
import { Form, FormItemProps, InputNumber } from 'antd';
import { valueType } from 'antd/lib/statistic/utils';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { memo, useEffect, useState } from 'react';
import { ControllerValuesName } from '../..';
import { rangeNumberValidator } from '../../../utils';

export const RangeNumberSetter: React.FC<{}> = memo(() => {
  const tc = useI18NPrefix(`viz.control`);
  const itemProps: FormItemProps<any> = {
    preserve: true,
    name: ControllerValuesName,
    label: tc('defaultValue'),
    required: false,
  };
  return <RangeNumberSetForm {...itemProps} />;
});
export interface RangeNumberSetFormProps {
  onChange?: () => any;
  value?: any[];
}
export const RangeNumberSetForm: React.FC<RangeNumberSetFormProps> = memo(
  ({ onChange, value, ...rest }) => {
    return (
      <Form.Item
        rules={[{ required: true, validator: rangeNumberValidator }]}
        {...rest}
      >
        <RangeNumberSet />
      </Form.Item>
    );
  },
);
export interface RangeNumberSetProps {
  onChange?: (value) => any;
  value?: any[];
}
export const RangeNumberSet: React.FC<RangeNumberSetProps> = memo(
  ({ onChange, value }) => {
    const [startVal, setStartVal] = useState<valueType | undefined>();
    const [endVal, setEndVal] = useState<valueType | undefined>();
    const onStartChange = start => {
      onChange?.([start, endVal]);
    };
    const onEndChange = end => {
      onChange?.([startVal, end]);
    };
    useEffect(() => {
      setStartVal(value?.[0]);
      setEndVal(value?.[1]);
    }, [value]);
    return (
      <>
        <InputNumber value={startVal} onChange={onStartChange} />
        <span> - </span>
        <InputNumber value={endVal} onChange={onEndChange} />
      </>
    );
  },
);
