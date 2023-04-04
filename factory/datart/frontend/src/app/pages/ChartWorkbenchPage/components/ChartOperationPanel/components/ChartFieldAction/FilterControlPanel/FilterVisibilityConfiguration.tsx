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

import { Input, Radio, Row, Select, Space } from 'antd';
import { ControllerVisibilityTypes } from 'app/constants';
import useI18NPrefix, { I18NComponentProps } from 'app/hooks/useI18NPrefix';
import { ChartDataSectionField, FilterVisibility } from 'app/types/ChartConfig';
import { FilterSqlOperator } from 'globalConstants';
import { FC, memo, useState } from 'react';
import styled from 'styled-components/macro';

const FilterVisibilityConfiguration: FC<
  {
    visibility?: FilterVisibility;
    otherFilters?: ChartDataSectionField[];
    onChange: (visibility: FilterVisibility) => void;
  } & I18NComponentProps
> = memo(({ otherFilters, visibility, onChange: onVisibilityChange }) => {
  const t = useI18NPrefix('viz.common.enum.controllerVisibilityTypes');
  const t2 = useI18NPrefix('viz.common.enum.filterOperator');
  const [fieldUid, setFieldUid] = useState(() => {
    if (typeof visibility === 'object') {
      return visibility?.fieldUid;
    }
  });
  const [relation, setRelation] = useState(() => {
    if (typeof visibility === 'object') {
      return visibility?.relation;
    }
  });
  const [value, setValue] = useState(() => {
    if (typeof visibility === 'object') {
      return visibility?.value;
    }
  });
  const [visibilityType, setVisibilityType] = useState(() => {
    if (visibility === null || visibility === undefined) {
      return ControllerVisibilityTypes.Hide;
    }
    if (typeof visibility === 'string') {
      return visibility;
    }
    if (typeof visibility === 'object') {
      return ControllerVisibilityTypes.Condition;
    }
  });

  const handleVisibilityTypeChange = type => {
    setVisibilityType(type);
    if (type === ControllerVisibilityTypes.Condition) {
      handleConditionChange(fieldUid, relation, value);
      return;
    }
    onVisibilityChange(type);
  };

  const handleConditionChange = (fieldUid, relation, value) => {
    setFieldUid(fieldUid);
    setRelation(relation);
    setValue(value);

    onVisibilityChange({
      visibility: ControllerVisibilityTypes.Condition,
      fieldUid,
      relation,
      value,
    });
  };

  return (
    <StyledFilterVisibilityConfiguration>
      <Row>
        <Radio.Group
          value={visibilityType}
          onChange={e => handleVisibilityTypeChange(e.target?.value)}
        >
          <Radio value={ControllerVisibilityTypes.Hide}>
            {t(ControllerVisibilityTypes.Hide)}
          </Radio>
          <Radio value={ControllerVisibilityTypes.Show}>
            {t(ControllerVisibilityTypes.Show)}
          </Radio>
          {!!otherFilters?.length && (
            <Radio value={ControllerVisibilityTypes.Condition}>
              {t(ControllerVisibilityTypes.Condition)}
            </Radio>
          )}
        </Radio.Group>
      </Row>
      {!!otherFilters?.length &&
        visibilityType === ControllerVisibilityTypes.Condition && (
          <Row>
            <Space>
              <Select
                value={fieldUid}
                onChange={uid => handleConditionChange(uid, relation, value)}
              >
                {(otherFilters || []).map(f => (
                  <Select.Option key={f.uid} value={f.uid || f.colName}>
                    {f.alias?.name || f.colName}
                  </Select.Option>
                ))}
              </Select>
              <Select
                value={relation}
                onChange={r => handleConditionChange(fieldUid, r, value)}
              >
                {[FilterSqlOperator.Equal, FilterSqlOperator.NotEqual].map(
                  f => (
                    <Select.Option key={f} value={f}>
                      {t2(f)}
                    </Select.Option>
                  ),
                )}
              </Select>
              <Input
                value={value}
                onChange={e =>
                  handleConditionChange(fieldUid, relation, e.target.value)
                }
              />
            </Space>
          </Row>
        )}
    </StyledFilterVisibilityConfiguration>
  );
});

export default FilterVisibilityConfiguration;

const StyledFilterVisibilityConfiguration = styled.div`
  .ant-select {
    width: 200px;
  }
`;
