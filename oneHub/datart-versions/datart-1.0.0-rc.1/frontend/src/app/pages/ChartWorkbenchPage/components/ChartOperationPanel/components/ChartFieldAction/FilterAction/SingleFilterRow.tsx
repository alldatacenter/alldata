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

import { InputNumber, Select, Space } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import ChartFilterCondition, {
  ConditionBuilder,
} from 'app/models/ChartFilterCondition';
import { FilterSqlOperator } from 'globalConstants';
import { FC, memo, useState } from 'react';

const SingleFilter: FC<{
  rowName: string;
  condition: ChartFilterCondition;
  onAddBrotherFilter?: () => void;
  onDeleteSelfFilter?: () => void;
  onConditionChange: (condition: ChartFilterCondition) => void;
}> = memo(
  ({
    rowName,
    condition,
    onAddBrotherFilter,
    onDeleteSelfFilter,
    onConditionChange,
  }) => {
    const t = useI18NPrefix('viz.common.enum.filterOperator');
    const operators = [
      FilterSqlOperator.Between,
      FilterSqlOperator.NotBetween,
      FilterSqlOperator.Equal,
      FilterSqlOperator.NotEqual,
      FilterSqlOperator.GreaterThanOrEqual,
      FilterSqlOperator.LessThanOrEqual,
      FilterSqlOperator.LessThan,
      FilterSqlOperator.GreaterThan,
      FilterSqlOperator.Null,
      FilterSqlOperator.NotNull,
    ];
    const [operator, setOperator] = useState<string | undefined>(
      condition?.operator || operators[0],
    );
    const [inputValue, setInputValue] = useState<number[]>(condition?.value);

    const handleOnChange = (operator, inputValue) => {
      setOperator(operator);
      setInputValue(inputValue);
      const filter = new ConditionBuilder(condition)
        .setOperator(operator)
        .setValue(inputValue)
        .asSingleOrRangeValue();
      onConditionChange(filter);
    };

    const renderInputNumber = op => {
      if (op === FilterSqlOperator.Null || op === FilterSqlOperator.NotNull) {
        return null;
      }
      if (
        op === FilterSqlOperator.Between ||
        op === FilterSqlOperator.NotBetween
      ) {
        return (
          <>
            <InputNumber
              value={inputValue?.[0]}
              onChange={value => {
                handleOnChange(op, [value, inputValue?.[1]]);
              }}
            />
            {' - '}
            <InputNumber
              value={inputValue?.[1]}
              onChange={value => {
                handleOnChange(op, [inputValue?.[0], value]);
              }}
            />
          </>
        );
      }
      return (
        <InputNumber
          value={inputValue?.[0]}
          onChange={value => {
            handleOnChange(op, [value]);
          }}
        />
      );
    };

    return (
      <div style={{ display: 'flex', alignItems: 'center' }}>
        <Space>
          <Select
            value={operator}
            onChange={value => {
              handleOnChange(value, []);
            }}
          >
            {operators.map(o => (
              <Select.Option key={o} value={o}>
                {t(o)}
              </Select.Option>
            ))}
          </Select>
          {renderInputNumber(operator)}
          {/* <PlusCircleOutlined onClick={() => onAddBrotherFilter()} />
          <BranchesOutlined onClick={handleChangeFromSingleRowToMultiRow} />
          <MinusCircleOutlined onClick={() => onDeleteSelfFilter()} /> */}
        </Space>
      </div>
    );
  },
);

export default SingleFilter;
