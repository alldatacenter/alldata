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
import { CloseOutlined } from '@ant-design/icons';
import { InputNumber, Select, Space } from 'antd';
import { DEVICE_LIST } from 'app/pages/DashBoardPage/constants';
import React, { memo, useEffect, useState } from 'react';
import styled from 'styled-components/macro';
const { Option } = Select;

const ListKeys = Object.keys(DEVICE_LIST);

const initValues = DEVICE_LIST[ListKeys[0]];

export const DeviceList: React.FC<{
  updateCurWH: (values: number[]) => void;
}> = memo(({ updateCurWH }) => {
  useEffect(() => {
    updateCurWH(initValues);
  }, [updateCurWH]);

  const [curW, setCurW] = useState<number>(initValues[0]);
  const [curH, setCurH] = useState<number>(initValues[1]);
  const [disabled, setDisabled] = useState(true);
  const onChangeW = value => {
    setCurW(Math.min(value, 768));
  };

  const onChangeH = value => {
    setCurH(value);
  };

  const onBlur = () => {
    updateCurWH([curW, curH]);
  };
  const changeDeviceKey = deviceKey => {
    const values = DEVICE_LIST[deviceKey] || [curW, curH];

    const isCustom = deviceKey === 'custom';
    setDisabled(!isCustom);
    setCurW(values[0]);
    setCurH(values[1]);
    updateCurWH(values);
  };

  return (
    <StyledWrap>
      <Space>
        <Select
          defaultValue={ListKeys[0]}
          style={{ width: 180 }}
          onChange={changeDeviceKey}
          size="small"
        >
          {ListKeys.map(item => {
            return (
              <Option key={item} value={item}>
                {item}
              </Option>
            );
          })}
        </Select>
        <div>
          <InputNumber
            size="small"
            value={curW}
            disabled={disabled}
            onChange={onChangeW}
            onBlur={onBlur}
          />
          <CloseOutlined />
          <InputNumber
            size="small"
            disabled={disabled}
            value={curH}
            onChange={onChangeH}
            onBlur={onBlur}
          />
        </div>
      </Space>
    </StyledWrap>
  );
});

const StyledWrap = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 36px;
  background-color: ${p => p.theme.componentBackground};
`;

export default DeviceList;
