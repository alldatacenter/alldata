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

import { Col, InputNumber, Row } from 'antd';
import { updateBy } from 'app/utils/mutation';
import { FC, memo, useState } from 'react';
import styled from 'styled-components/macro';
import { PresentControllerFilterProps } from '.';

const RangValueFilter: FC<PresentControllerFilterProps> = memo(
  ({ condition, onConditionChange }) => {
    const [valueRange, setValueRange] = useState<[number, number] | []>(() => {
      if (Array.isArray(condition?.value)) {
        return condition?.value as [number, number];
      }
      return [];
    });

    const handleValueChange = index => value => {
      const newCondition = updateBy(condition!, draft => {
        const newValue = draft?.value || [];
        newValue[index] = value;
        draft.value = newValue;
      });

      onConditionChange(newCondition);
      setValueRange(newCondition.value as [number, number]);
    };

    return (
      <StyledRangeValueFilter>
        <Col span={10}>
          <InputNumber
            value={valueRange?.[0]}
            onChange={handleValueChange(0)}
          />
        </Col>
        <Col span={4} className="text-center">
          -
        </Col>
        <Col span={10}>
          <InputNumber
            value={valueRange?.[1]}
            onChange={handleValueChange(1)}
          />
        </Col>
      </StyledRangeValueFilter>
    );
  },
);

export default RangValueFilter;

const StyledRangeValueFilter = styled(Row)`
  .text-center {
    line-height: 32px;
    text-align: center;
  }

  .ant-col .ant-input-number {
    width: 100%;
  }
`;
