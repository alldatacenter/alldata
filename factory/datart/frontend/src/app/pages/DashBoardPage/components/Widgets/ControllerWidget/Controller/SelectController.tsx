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
import { Form, Select } from 'antd';
import { SelectValue } from 'antd/lib/select';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { ControlOption } from 'app/pages/DashBoardPage/pages/BoardEditor/components/ControllerWidgetPanel/types';
import React, { memo } from 'react';
import styled from 'styled-components/macro';

export interface SelectControllerProps {
  defaultValue?: SelectValue;
  options?: ControlOption[];
  value?: SelectValue;
  placeholder?: string;
  onChange: (values) => void;
  label?: React.ReactNode;
  name?: string;
  required?: boolean;
}
const { Option } = Select;
export const SelectControllerForm: React.FC<SelectControllerProps> = memo(
  ({ label, name, required, value, ...rest }) => {
    return (
      <Form.Item
        shouldUpdate
        name={name}
        label={label}
        validateTrigger={['onChange', 'onBlur']}
        rules={[{ required: required }]}
      >
        <SelectController {...rest} value={value} />
      </Form.Item>
    );
  },
);
export const SelectController: React.FC<SelectControllerProps> = ({
  options,
  onChange,
  value,
  defaultValue,
}) => {
  const t = useI18NPrefix(`viz.common.enum.controllerPlaceHolders`);
  return (
    <StyledSelect
      showSearch
      allowClear
      value={value}
      style={{ width: '100%' }}
      placeholder={t('selectController')}
      onChange={onChange}
      optionFilterProp="label"
      bordered={false}
      filterOption={(input, option) =>
        String(option?.label).toLowerCase().indexOf(input.toLowerCase()) >= 0
      }
    >
      {(options || []).map(item => {
        //  ##659
        return (
          <Option
            key={item.value + item.label}
            label={item.label ?? item.value ?? 'none'}
            value={item.value}
          >
            <span>{item.label ?? item.value}</span>
          </Option>
        );
      })}
    </StyledSelect>
  );
};
const StyledSelect = styled(Select)`
  display: block;

  &.ant-select .ant-select-selector {
    background-color: transparent !important;
  }
`;
