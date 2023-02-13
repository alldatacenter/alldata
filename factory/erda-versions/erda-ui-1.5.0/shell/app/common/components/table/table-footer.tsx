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
import { Dropdown, Button, Menu } from 'antd';
import Pagination from 'common/components/pagination';
import { ErdaIcon } from 'common';
import i18n from 'i18n';
import { IRowSelection, IRowActions, TablePaginationConfig } from './interface';

interface IProps {
  rowSelection?: IRowSelection;
  pagination: TablePaginationConfig;
  hidePagination: boolean;
  onTableChange: ([key]: any) => void;
}

const TableFooter = ({ rowSelection, pagination, hidePagination, onTableChange }: IProps) => {
  const batchMenu = () => {
    return (
      <Menu>
        {(rowSelection?.actions || []).map((item: IRowActions) => {
          return (
            <Menu.Item key={item.key} onClick={() => item.onClick()} disabled={item.disabled}>
              {item.name}
            </Menu.Item>
          );
        })}
      </Menu>
    );
  };

  return (
    <div className="erda-table-footer flex justify-between">
      {rowSelection?.actions ? (
        <div className="erda-table-batch-ops flex items-center">
          <Dropdown
            overlay={batchMenu}
            trigger={['click']}
            disabled={rowSelection.selectedRowKeys?.length === 0}
            getPopupContainer={(triggerNode) => triggerNode.parentElement as HTMLElement}
          >
            <Button type="default">
              {i18n.t('dop:batch processing')}
              <ErdaIcon
                type="caret-down"
                className="ml-0.5 relative top-0.5"
                fill={rowSelection.selectedRowKeys?.length === 0 ? 'disabled' : 'normal'}
                size="14"
              />
            </Button>
          </Dropdown>
        </div>
      ) : (
        <div />
      )}

      {!hidePagination && (
        <div className="flex items-center justify-end">
          <Pagination {...pagination} onChange={(page, size) => onTableChange({ pageNo: page, pageSize: size })} />
        </div>
      )}
    </div>
  );
};

export default TableFooter;
