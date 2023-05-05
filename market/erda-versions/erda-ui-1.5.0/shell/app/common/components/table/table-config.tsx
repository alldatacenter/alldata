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
import { Checkbox, Popover } from 'antd';
import { ErdaIcon } from 'common';
import { TableConfigProps, ColumnProps } from './interface';

function TableConfig<T extends object = any>({
  slot,
  columns,
  setColumns,
  onReload,
  sortColumn = {},
}: TableConfigProps<T>) {
  const { column, order } = sortColumn;
  const onCheck = (checked: boolean, title: string) => {
    const newColumns = columns.map((item) => (item.title === title ? { ...item, hidden: !checked } : item));

    setColumns(newColumns);
  };

  const showLength = columns.filter((item) => !item.hidden).length;
  const columnsFilter = columns
    .filter((item) => item.title)
    .map((item: ColumnProps<T>) => (
      <div key={`${item.dataIndex}`}>
        <Checkbox
          className="whitespace-nowrap"
          checked={!item.hidden}
          onChange={(e) => onCheck(e.target.checked, item.title as string)}
          disabled={showLength === 1 && !item.hidden}
        >
          {typeof item.title === 'function' ? item.title({ sortColumn: column, sortOrder: order }) : item.title}
        </Checkbox>
      </div>
    ));

  return (
    <div className="erda-table-filter flex justify-between">
      <div className="erda-table-filter-content flex-1 flex items-center">
        <div className="flex-1">{slot}</div>
      </div>
      <div className="erda-table-filter-ops flex items-center">
        <ErdaIcon
          size="20"
          className={`icon-hover ml-3 bg-hover p-1`}
          type="refresh"
          color="currentColor"
          onClick={onReload}
        />
        <Popover
          content={columnsFilter}
          trigger="click"
          placement="bottomRight"
          overlayClassName="erda-table-columns-filter"
          getPopupContainer={(triggerNode) => triggerNode.parentElement as HTMLElement}
        >
          <ErdaIcon type="config1" size="20" className={`ml-3 icon-hover bg-hover p-1`} />
        </Popover>
      </div>
    </div>
  );
}

export default TableConfig;
