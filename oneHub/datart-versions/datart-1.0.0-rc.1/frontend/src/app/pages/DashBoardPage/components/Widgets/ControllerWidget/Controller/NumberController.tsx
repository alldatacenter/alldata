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
import { Form, InputNumber } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { memo, useEffect, useState } from 'react';
import styled from 'styled-components/macro';

export interface TextControllerProps {
  value?: any;
  placeholder?: string;
  onChange: (values) => void;
  label?: React.ReactNode;
  name?: string;
  required?: boolean;
}

export const NumberControllerForm: React.FC<TextControllerProps> = memo(
  ({ label, name, required, ...rest }) => {
    return (
      <Form.Item
        name={name}
        label={label}
        validateTrigger={['onChange', 'onBlur']}
        rules={[{ required: false }]}
      >
        <NumberController {...rest} />
      </Form.Item>
    );
  },
);
export const NumberController: React.FC<TextControllerProps> = memo(
  ({ onChange, value }) => {
    const [val, setVal] = useState();
    const _onChange = numberVal => {
      setVal(numberVal);
    };
    const _onChangeEnter = e => {
      onChange(e.target.value);
    };
    const _onBlur = () => {
      if (val !== value) {
        let _val = val === 0 ? '0' : val;
        onChange(_val);
      }
    };
    useEffect(() => {
      setVal(value);
    }, [value]);
    const t = useI18NPrefix(`viz.common.enum.controllerPlaceHolders`);
    return (
      <StyledWrap>
        <InputNumber
          style={{ width: '100%' }}
          className="control-number-input"
          value={val}
          placeholder={t('numberController')}
          onChange={_onChange}
          onPressEnter={_onChangeEnter}
          onBlur={_onBlur}
          bordered={false}
        />
      </StyledWrap>
    );
  },
);
const StyledWrap = styled.div`
  display: flex;

  justify-content: space-around;
  width: 100%;

  & .control-input-input {
    width: 100%;
  }
  .control-select {
    display: flex;
    flex: 1;
    width: 40%;
  }
  .control-input {
    display: flex;
    flex: 1;
  }
  &.ant-select .ant-select-selector {
    background-color: transparent;
  }
  & .ant-input-number {
    background-color: transparent;
  }
`;
