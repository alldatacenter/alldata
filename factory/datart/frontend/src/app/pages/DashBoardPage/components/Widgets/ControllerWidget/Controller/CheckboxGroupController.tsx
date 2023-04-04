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
import { Checkbox, Form } from 'antd';
import { CheckboxValueType } from 'antd/lib/checkbox/Group';
import { ControlOption } from 'app/pages/DashBoardPage/pages/BoardEditor/components/ControllerWidgetPanel/types';
import React, { memo, useCallback } from 'react';
import styled from 'styled-components/macro';

export interface CheckboxGroupControllerProps {
  options?: ControlOption[];
  value?: CheckboxValueType[];
  placeholder?: string;
  onChange: (values) => void;
  label?: React.ReactNode;
  name?: string;
  required?: boolean;
}

export const CheckboxGroupControllerForm: React.FC<CheckboxGroupControllerProps> =
  memo(({ label, name, required, ...rest }) => {
    return (
      <Form.Item
        name={name}
        label={label}
        validateTrigger={['onChange', 'onBlur']}
        rules={[{ required: false }]}
      >
        <CheckboxGroupSetter {...rest} />
      </Form.Item>
    );
  });
export const CheckboxGroupSetter: React.FC<CheckboxGroupControllerProps> = memo(
  ({ options, onChange, value }) => {
    const renderOptions = useCallback(() => {
      return (options || []).map(o => ({
        label: o.label ?? o.value,
        value: o.value || '',
        key: o.label + o.value,
      }));
    }, [options]);

    return (
      <Wrapper>
        <Checkbox.Group
          value={value}
          onChange={onChange}
          options={renderOptions()}
        />
      </Wrapper>
    );
  },
);
const Wrapper = styled.div`
  display: flex;
`;
