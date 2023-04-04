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
import { Form, FormItemProps, Slider } from 'antd';
import React, { memo, useEffect, useState } from 'react';
import { ControllerValuesName } from '../..';

export const SliderSetter: React.FC<SliderSetFormProps> = memo(props => {
  const itemProps: FormItemProps<any> = {
    preserve: true,
    name: ControllerValuesName,
    label: props.label,
    required: true,
  };
  return <SliderSetForm {...itemProps} {...props} />;
});

export const RangeSliderSetter: React.FC<SliderSetFormProps> = memo(props => {
  const itemProps: FormItemProps<any> = {
    preserve: true,
    name: ControllerValuesName,
    label: props.label,
    required: false,
  };
  return <RangeSliderSetForm {...itemProps} {...props} />;
});

export type SliderSetFormProps = { label: string } & FormItemProps<any> &
  SliderSetProps;

export const RangeSliderSetForm: React.FC<SliderSetFormProps> = memo(
  ({ ...rest }) => {
    return (
      <Form.Item rules={[{ required: true }]}>
        <SliderSet {...rest} />
      </Form.Item>
    );
  },
);

export const SliderSetForm: React.FC<SliderSetFormProps> = memo(
  ({ ...rest }) => {
    return (
      <Form.Item rules={[{ required: true }]} {...rest}>
        <SliderSet {...rest} />
      </Form.Item>
    );
  },
);

export interface SliderSetProps {
  onChange?: (value) => any;
  value?: any[];
  maxValue: number;
  minValue: number;
  step: number;
  showMarks: boolean;
}
export const SliderSet: React.FC<SliderSetProps> = memo(
  ({ onChange, value, maxValue, minValue, step, showMarks }) => {
    const [val, setVal] = useState<any>();
    const _onChange = _val => {
      onChange?.([_val]);
    };
    const marks = {
      [minValue]: minValue,
      [maxValue]: maxValue,
      [val]: val,
    };
    useEffect(() => {
      setVal(value?.[0]);
    }, [value]);
    return (
      <Slider
        max={maxValue}
        min={minValue}
        step={step}
        value={val}
        {...(showMarks && { marks: marks })}
        onChange={_onChange}
      />
    );
  },
);

export const RangeSliderSet: React.FC<SliderSetProps> = memo(
  ({ onChange, value }) => {
    const [val, setVal] = useState<any>();
    const _onChange = _val => {
      onChange?.(_val);
    };
    useEffect(() => {
      setVal([value?.[0], value?.[1]]);
    }, [value]);
    return <Slider range value={val} onChange={_onChange} />;
  },
);
