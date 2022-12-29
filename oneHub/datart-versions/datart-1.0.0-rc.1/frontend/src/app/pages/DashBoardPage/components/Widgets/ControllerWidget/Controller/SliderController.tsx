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
import { Form, Slider } from 'antd';
import React, { memo, useEffect, useState } from 'react';
import styled from 'styled-components/macro';

export interface SelectControllerProps {
  showMarks: boolean;
  step: number;
  minValue: number;
  maxValue: number;
  value?: any;
  placeholder?: string;
  onChange: (values) => void;
  label?: React.ReactNode;
  name?: string;
  required?: boolean;
}
export const SlideControllerForm: React.FC<SelectControllerProps> = memo(
  ({ label, name, required, ...rest }) => {
    return (
      <Form.Item
        name={name}
        label={label}
        validateTrigger={['onChange', 'onBlur']}
        rules={[{ required: false }]}
      >
        <SlideController {...rest} />
      </Form.Item>
    );
  },
);
export const SlideController: React.FC<SelectControllerProps> = memo(
  ({ onChange, value, minValue, maxValue, step, showMarks }) => {
    const [val, setVal] = useState<any>();

    useEffect(() => {
      setVal(value);
    }, [value]);

    const marks = {
      [minValue]: minValue,
      [maxValue]: maxValue,
      [val]: val,
    };
    return (
      <StyledWrap>
        <Slider
          value={val}
          onChange={setVal}
          onAfterChange={onChange}
          min={minValue}
          max={maxValue}
          step={step}
          {...(showMarks && { marks: marks })}
        />
      </StyledWrap>
    );
  },
);
const StyledWrap = styled.div`
  /* display: flex;

  justify-content: space-around;
  width: 100%; */
`;
