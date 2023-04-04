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

import { Input, Select, Space } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import ChartFilterCondition, {
  ConditionBuilder,
} from 'app/models/ChartFilterCondition';
import { FilterSqlOperator } from 'globalConstants';
import debounce from 'lodash/debounce';
import { FC, memo, useCallback, useState } from 'react';

const CategoryConditionRelationSelector: FC<{
  condition?: ChartFilterCondition;
  onConditionChange: (condition: ChartFilterCondition) => void;
}> = memo(({ condition, onConditionChange }) => {
  const t = useI18NPrefix('viz.common.filter.category');
  const t2 = useI18NPrefix('viz.common.enum.filterOperator');
  const [inputValue, setInputValue] = useState(() => condition?.value);

  const handleConditionFilterChange = useCallback(
    (relation, value) => {
      const filter = new ConditionBuilder(condition)
        .setOperator(relation)
        .setValue(value)
        .asCondition();
      onConditionChange(filter);
    },
    [onConditionChange, condition],
  );

  const debounceHandleFilterChange = debounce(handleConditionFilterChange, 200);

  const renderRelationSelector = () => {
    return (
      <Select
        value={condition?.operator}
        onChange={op => {
          setInputValue('');
          handleConditionFilterChange(op, null);
        }}
      >
        <Select.OptGroup label={t('include')}>
          {[
            FilterSqlOperator.Contain,
            FilterSqlOperator.PrefixContain,
            FilterSqlOperator.SuffixContain,
            FilterSqlOperator.Equal,
            FilterSqlOperator.Null,
          ].map(f => (
            <Select.Option key={f} value={f}>
              {t2(f)}
            </Select.Option>
          ))}
        </Select.OptGroup>
        <Select.OptGroup label={t('notInclude')}>
          {[
            FilterSqlOperator.NotContain,
            FilterSqlOperator.NotPrefixContain,
            FilterSqlOperator.NotSuffixContain,
            FilterSqlOperator.NotEqual,
            FilterSqlOperator.NotNull,
          ].map(f => (
            <Select.Option key={f} value={f}>
              {t2(f)}
            </Select.Option>
          ))}
        </Select.OptGroup>
      </Select>
    );
  };

  return (
    <Space>
      {condition?.operator !== FilterSqlOperator.Null &&
        condition?.operator !== FilterSqlOperator.NotNull && (
          <Input
            addonBefore={renderRelationSelector()}
            value={inputValue}
            onChange={e => {
              setInputValue(e.target.value);
              debounceHandleFilterChange(condition?.operator, e.target.value);
            }}
          />
        )}
      {(condition?.operator === FilterSqlOperator.Null ||
        condition?.operator === FilterSqlOperator.NotNull) &&
        renderRelationSelector()}
    </Space>
  );
});

export default CategoryConditionRelationSelector;
