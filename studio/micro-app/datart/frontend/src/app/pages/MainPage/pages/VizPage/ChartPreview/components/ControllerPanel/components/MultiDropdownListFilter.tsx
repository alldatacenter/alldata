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

import { TreeSelect } from 'antd';
import useFetchFilterDataByCondition from 'app/hooks/useFetchFilterDataByCondition';
import { RelationFilterValue } from 'app/types/ChartConfig';
import { updateBy } from 'app/utils/mutation';
import { FC, memo, useMemo, useState } from 'react';
import styled from 'styled-components/macro';
import { IsKeyIn } from 'utils/object';
import { PresentControllerFilterProps } from '.';

const MultiDropdownListFilter: FC<PresentControllerFilterProps> = memo(
  ({ viewId, view, condition, executeToken, onConditionChange }) => {
    const [originalNodes, setOriginalNodes] = useState<RelationFilterValue[]>(
      condition?.value as RelationFilterValue[],
    );

    useFetchFilterDataByCondition(
      viewId,
      condition,
      setOriginalNodes,
      view,
      executeToken,
    );

    const handleSelectedChange = (keys: any) => {
      const newCondition = updateBy(condition!, draft => {
        draft.value = keys;
      });
      onConditionChange(newCondition);
    };
    const selectedNodes = useMemo(() => {
      if (Array.isArray(condition?.value)) {
        const item = condition?.value?.[0];
        if (typeof item === 'object') {
          const firstValues =
            (condition?.value as [])?.filter(n => {
              if (IsKeyIn(n as RelationFilterValue, 'key')) {
                return (n as RelationFilterValue).isSelected;
              }
              return false;
            }) || [];
          return firstValues?.map((n: RelationFilterValue) => n.key);
        } else {
          return condition?.value || [];
        }
      }
      return [];
    }, [condition]);
    return (
      <StyledMultiDropdownListFilter
        showSearch
        allowClear
        multiple
        treeCheckable
        treeDefaultExpandAll
        value={selectedNodes as any}
        onChange={handleSelectedChange}
      >
        {originalNodes?.map((n: RelationFilterValue) => {
          return (
            <TreeSelect.TreeNode key={n.key} value={n.key} title={n.label} />
          );
        })}
      </StyledMultiDropdownListFilter>
    );
  },
);

export default MultiDropdownListFilter;

const StyledMultiDropdownListFilter = styled(TreeSelect)`
  width: 100%;
`;
