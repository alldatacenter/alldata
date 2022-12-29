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

import { BranchesOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { Col, Row, Select, Switch } from 'antd';
import { FilterConditionType, FilterRelationType } from 'app/constants';
import ChartFilterCondition, {
  ConditionBuilder,
} from 'app/models/ChartFilterCondition';
import { ChartDataSectionField } from 'app/types/ChartConfig';
import { getColumnRenderName } from 'app/utils/chartHelper';
import { FC, useState } from 'react';
import styled from 'styled-components/macro';

const RelationTypeFilter: FC<{
  condition: ChartFilterCondition;
  fields: ChartDataSectionField[];
  showOperator?: boolean;
  onRelationChange: (relation: ChartFilterCondition) => void;
}> = ({ condition, fields, showOperator = true, onRelationChange }) => {
  const handleChildRelationChange = index => childRelation => {
    if (!condition?.children) {
      return;
    }
    const children = condition.children;
    children[index] = childRelation || new ChartFilterCondition();
    onRelationChange(condition);
  };

  const handleOperatorChange = operator => {
    condition.operator = operator;
    onRelationChange(condition);
  };

  if (!condition?.children || condition?.children.length !== 2) {
    return null;
  }

  return (
    <StyledConditionRow gutter={24} justify="center">
      <ConditionCol
        condition={condition.children[0]}
        fields={fields}
        onRelationChange={handleChildRelationChange(0)}
      />
      {showOperator && (
        <OperatorCol
          operator={condition.operator}
          onOperatorChange={handleOperatorChange}
        />
      )}
      <ConditionCol
        condition={condition.children[1]}
        fields={fields}
        onRelationChange={handleChildRelationChange(1)}
      />
    </StyledConditionRow>
  );
};

const ConditionCol: FC<{
  condition: ChartFilterCondition;
  fields: ChartDataSectionField[];
  onRelationChange: (relation?: ChartFilterCondition) => void;
}> = ({ condition, fields, onRelationChange }) => {
  const onAddSubRelation = () => {
    const newCondition = new ConditionBuilder(condition)
      .setOperator(FilterRelationType.AND)
      .asRelation(condition.value, [
        new ChartFilterCondition(),
        new ChartFilterCondition(),
      ]);
    onRelationChange(newCondition);
  };

  const onDeleteRelation = () => {
    onRelationChange(undefined);
  };

  const handleOperatorChange = operator => {
    const newCondition = new ConditionBuilder(condition)
      .setOperator(operator)
      .asRelation();
    onRelationChange(newCondition);
  };

  const handleConditionChange = value => {
    const newCondition = new ConditionBuilder(condition)
      .setValue(value)
      .asRelation();
    onRelationChange(newCondition);
  };

  if (condition.type === FilterConditionType.Relation) {
    return (
      <StyledRelationCol>
        <Row>
          <OperatorCol
            operator={condition.operator}
            onOperatorChange={handleOperatorChange}
          />
          <MinusCircleOutlined onClick={onDeleteRelation} />
        </Row>
        <Row>
          <RelationTypeFilter
            condition={condition}
            fields={fields}
            showOperator={false}
            onRelationChange={onRelationChange}
          />
        </Row>
      </StyledRelationCol>
    );
  }

  return (
    <StyledRelationCol>
      <Row>
        <Select
          value={condition?.value as string}
          onChange={handleConditionChange}
        >
          {fields.map(c => (
            <Select.Option value={c.colName}>
              {getColumnRenderName(c)}
            </Select.Option>
          ))}
        </Select>
        <BranchesOutlined onClick={onAddSubRelation} />
      </Row>
    </StyledRelationCol>
  );
};

const OperatorCol: FC<{ operator; onOperatorChange }> = ({
  operator,
  onOperatorChange,
}) => {
  const [opValue, setOpValue] = useState(operator);

  return (
    <StyledOperatorCol>
      <Switch
        checkedChildren={FilterRelationType.AND}
        unCheckedChildren={FilterRelationType.OR}
        defaultChecked
        checked={opValue === FilterRelationType.AND}
        onChange={checked => {
          const value = checked
            ? FilterRelationType.AND
            : FilterRelationType.OR;
          setOpValue(value);
          onOperatorChange(value);
        }}
      />
    </StyledOperatorCol>
  );
};

export default RelationTypeFilter;

const StyledConditionRow = styled(Row)`
  border: 1px dashed green;

  .anticon {
    align-self: center;
    padding-left: 4px;
  }
`;

const StyledRelationCol = styled(Col)`
  align-self: center;

  .ant-row:first-child {
    justify-content: center;
  }

  .ant-select {
    width: 100px;
    text-align: center;
  }

  button {
    display: inline-block;
  }
`;

const StyledOperatorCol = styled(Col)`
  align-self: center;
  width: 100px;
  padding: 0 !important;
  text-align: center;
`;
