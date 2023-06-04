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

import { Switch } from 'antd';
import { FilterConditionType, FilterRelationType } from 'app/constants';
import ChartFilterCondition from 'app/models/ChartFilterCondition';
import { FC, memo } from 'react';
import SingleFilterRow from './SingleFilterRow';

const MultiFilterRow: FC<{
  rowName: string;
  condition: ChartFilterCondition;
  onConditionChange: (condition: ChartFilterCondition) => void;
}> = memo(({ onConditionChange, condition, rowName }) => {
  const handleFilterChanged = index => childFilter => {
    condition.updateChild(index, childFilter);
    onConditionChange(condition);
  };

  const handleAddBrotherFilter = index => () => {
    condition.appendChild(index);
    onConditionChange(condition);
  };

  const handleDeleteSelfFilter = index => () => {
    condition.removeChild(index);
    onConditionChange(condition);
  };

  const handleFilterRelationChange = checked => {
    condition.setValue(
      checked ? FilterRelationType.AND : FilterRelationType.OR,
    );
    onConditionChange(condition);
  };

  return (
    <div>
      <div
        style={{
          display: 'flex',
          border: 'solid 1px gray',
          padding: '5px 10px',
        }}
      >
        <Switch
          style={{ alignSelf: 'center' }}
          checkedChildren={FilterRelationType.AND.toUpperCase()}
          unCheckedChildren={FilterRelationType.OR.toUpperCase()}
          defaultChecked={condition.value !== FilterRelationType.OR}
          onChange={handleFilterRelationChange}
        />
        <div style={{ display: 'flex', flexFlow: 'column' }}>
          {condition.children?.map((r, index) => {
            if (r.type === FilterConditionType.Relation) {
              return (
                <MultiFilterRow
                  key={index}
                  rowName={rowName + index}
                  condition={r}
                  onConditionChange={handleFilterChanged(index)}
                />
              );
            }

            return (
              <SingleFilterRow
                key={index}
                rowName={rowName + index}
                condition={r}
                onAddBrotherFilter={handleAddBrotherFilter(index)}
                onDeleteSelfFilter={handleDeleteSelfFilter(index)}
                onConditionChange={handleFilterChanged(index)}
              />
            );
          })}
        </div>
      </div>
    </div>
  );
});

export default MultiFilterRow;
