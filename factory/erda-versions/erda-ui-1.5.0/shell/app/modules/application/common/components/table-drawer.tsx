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
import { Drawer, Table, Spin } from 'antd';
import { get, isEmpty, findKey, reduce } from 'lodash';
import { SearchTable } from 'common';
import i18n from 'i18n';

import './table-drawer.scss';
import { PAGINATION } from 'app/constants';

interface IProps {
  tableAttrsList: any[];
  tableAttrsPaging: IPaging;
  centerRelatedGroup?: any;
  selectedItem: any;
  drawerVisible: boolean;
  isFetching: boolean;
  closeDrawer: (event: any) => void;
  getTableAttrs: (params: { filePath: string; searchKey?: string; pageNo: number }) => Promise<any>;
}

const columns = [
  {
    title: i18n.t('dop:attribute english name'),
    dataIndex: 'enName',
    width: 60,
    render: (enName: string) => (
      <div className="nowrap" title={enName}>
        {enName}
      </div>
    ),
  },
  {
    title: i18n.t('dop:attribute type'),
    dataIndex: 'type',
    width: 50,
    render: (type: string) => (
      <div className="nowrap" title={type}>
        {type}
      </div>
    ),
  },
  {
    title: i18n.t('dop:attribute chinese name'),
    dataIndex: 'cnName',
    width: 60,
    render: (cnName: string) => (
      <div className="nowrap" title={cnName}>
        {cnName}
      </div>
    ),
  },
  {
    title: i18n.t('dop:attribute description'),
    dataIndex: 'desc',
    width: 60,
    render: (desc: string) => (
      <div className="nowrap" title={desc}>
        {desc}
      </div>
    ),
  },
];

const TableDrawer = (props) => {
  const [tableRowClassNameMap, setTableRowClassNameMap] = React.useState({});

  const initTableRowClassNameMap = (_centerRelatedGroup: any) => {
    const COLOR_KEYS = ['purple', 'pink', 'green', 'purple-2', 'blue', 'red', 'green-2', 'orange'];
    const result = reduce(
      _centerRelatedGroup,
      (acc, value) => ({ ...acc, [COLOR_KEYS.pop() || '']: value.map((item: any) => item.relAttr) }),
      {},
    );
    setTableRowClassNameMap(result);
  };

  const {
    drawerVisible,
    closeDrawer,
    isFetching,
    tableAttrsList,
    tableAttrsPaging,
    centerRelatedGroup,
    getTableAttrs,
    selectedItem,
  } = props;
  const { pageNo: current, total } = tableAttrsPaging;

  React.useEffect(() => {
    centerRelatedGroup && initTableRowClassNameMap(centerRelatedGroup);
    !isEmpty(selectedItem) && getTableAttrs({ filePath: get(selectedItem, 'file'), pageNo: 1 });
  }, [centerRelatedGroup, getTableAttrs, selectedItem]);

  const getRelatedGroupClassName = (enName: string) => {
    return findKey(tableRowClassNameMap, (item: string[]) => item.includes(enName));
  };

  const onTableSearch = (searchKey: string) => {
    getTableAttrs({ filePath: get(selectedItem, 'file'), searchKey, pageNo: 1 });
  };

  const onPageChange = (pageNo: number) => {
    getTableAttrs({ filePath: get(selectedItem, 'file'), pageNo });
  };

  return (
    <Drawer
      destroyOnClose
      title={i18n.t('dop:class catalog')}
      width="50%"
      visible={drawerVisible}
      onClose={closeDrawer}
    >
      <Spin spinning={isFetching}>
        <SearchTable
          placeholder={i18n.t('dop:search by chinese/english name of attribute')}
          onSearch={onTableSearch}
          needDebounce
        >
          <Table
            columns={columns}
            rowKey="enName"
            rowClassName={({ enName }) =>
              centerRelatedGroup ? `with-group-tag group-tag-${getRelatedGroupClassName(enName)}` : ''
            }
            dataSource={tableAttrsList}
            pagination={{
              current,
              pageSize: PAGINATION.pageSize,
              total,
              onChange: onPageChange,
            }}
            scroll={{ x: '100%' }}
          />
        </SearchTable>
      </Spin>
    </Drawer>
  );
};

export { TableDrawer };
