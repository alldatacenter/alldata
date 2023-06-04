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

import { Tree } from 'antd';
import { FC, memo, useState } from 'react';
import { PresentControllerFilterProps } from '.';

const TreeFilter: FC<PresentControllerFilterProps> = memo(
  ({ condition, onConditionChange }) => {
    const getSelectedKeysFromTreeModel = (list, collector: string[]) => {
      list?.forEach(l => {
        if (l.isSelected) {
          collector.push(l.key);
        }
        if (l.children) {
          getSelectedKeysFromTreeModel(l.children, collector);
        }
      });
    };
    const [treeData] = useState(() => {
      if (Array.isArray(condition?.value)) {
        return condition?.value;
      }
      return [];
    });
    const [selectedKeys, setSelectedKeys] = useState(() => {
      if (!treeData) {
        return [];
      }
      const selectedKeys = [];
      getSelectedKeysFromTreeModel(treeData, selectedKeys);
      return selectedKeys;
    });

    const handleTreeNodeChange = keys => {
      setSelectedKeys(keys);
      setTreeCheckableState(treeData, keys);
      if (condition) {
        condition.value = treeData;
        onConditionChange && onConditionChange(condition);
      }
    };

    const isChecked = (selectedKeys, eventKey) =>
      selectedKeys.indexOf(eventKey) !== -1;

    const setTreeCheckableState = (treeList?, keys?: string[]) => {
      return (treeList || []).map(c => {
        c.isSelected = isChecked(keys, c.key);
        c.children = setTreeCheckableState(c.children, keys);
        return c;
      });
    };

    return (
      <Tree
        multiple
        checkable
        autoExpandParent
        treeData={treeData as any}
        checkedKeys={selectedKeys}
        onSelect={handleTreeNodeChange}
        onCheck={handleTreeNodeChange}
      />
    );
  },
);

export default TreeFilter;
