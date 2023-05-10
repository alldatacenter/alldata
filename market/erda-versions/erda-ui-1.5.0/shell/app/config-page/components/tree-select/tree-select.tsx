// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import React from 'react';
import { TreeSelect } from 'antd';
import { debounce, get } from 'lodash';
import { useUpdate } from 'common/use-hooks';
import { useUpdateEffect } from 'react-use';

const CP_TREE_SELECT = (props: CP_TREE_SELECT.Props) => {
  const { execOperation, operations = {}, props: configProps, data, state: pState = {} } = props;
  const { treeData: propTreeData } = data || {};
  const { visible = true, placeholder, title, ...rest } = configProps || {};

  const [{ value, treeData, searchValue }, updater, update] = useUpdate({
    value: pState?.value,
    treeData: propTreeData || [],
    searchValue: undefined as undefined | string,
  });

  useUpdateEffect(() => {
    updater.value(pState.value);
  }, [pState.value]);

  useUpdateEffect(() => {
    const folders = (propTreeData || []).filter((node) => !node.isLeaf);
    const files = (propTreeData || []).filter((node) => node.isLeaf);
    // const collator = new Intl.Collator(undefined, { numeric: true, sensitivity: 'base' });
    // folders.sort((x, y) => collator.compare(x.title, y.title));
    // files.sort((x, y) => collator.compare(x.title, y.title));
    updater.treeData([...folders, ...files]);
  }, [propTreeData]);

  const onChange = (val: string) => {
    updater.value(val);
    if (operations.onChange) {
      const _opData = operations.onChange.fillMeta ? val : { value: val };
      execOperation(operations?.onChange, _opData);
    }
  };

  const onSearch = (val: string) => {
    updater.searchValue(val);
  };

  useUpdateEffect(() => {
    search(searchValue);
  }, [searchValue]);

  const search = React.useCallback(
    debounce((q?: string) => {
      if (q) {
        operations.onSearch && execOperation(operations.onSearch, q);
      } else {
        updater.treeData([]);
        onLoadData();
      }
    }, 400),
    [],
  );

  const onLoadData = (treeNode?: CP_TREE_SELECT.INode) => {
    return new Promise((resolve) => {
      const id = get(treeNode, 'props.id');
      if (operations.onLoadData) {
        execOperation(operations.onLoadData, id);
      }
      resolve();
    });
  };

  if (!visible) return null;
  const showSearch = !!operations.onSearch;
  return (
    <div className="mb-5">
      {title ? <h4> {title} </h4> : null}
      <TreeSelect
        showSearch={showSearch}
        className={'w-full'}
        treeDataSimpleMode
        value={value}
        listHeight={500}
        placeholder={placeholder}
        allowClear
        onSearch={onSearch}
        onChange={onChange}
        loadData={onLoadData}
        filterTreeNode={false}
        labelInValue
        autoClearSearchValue={false}
        treeData={treeData as any[]}
        dropdownMatchSelectWidth
        {...rest}
      />
    </div>
  );
};

export default CP_TREE_SELECT;
