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
import { Table, Tooltip } from 'antd';
import { get } from 'lodash';
import moment from 'moment';
import { ColumnProps } from 'core/common/interface';
import i18n from 'i18n';

interface IData {
  [pro: string]: any;
  name: string;
  time?: string;
  max?: string;
  min?: string;
}

const TablePanel = ({ data }: { data: object }) => {
  const list = get(data, 'list') || [];
  const columns: Array<ColumnProps<IData>> = [
    {
      title: i18n.t('msp:page'),
      dataIndex: 'name',
      key: 'name',
      align: 'center',
      width: 320,
      render: (value: string) =>
        value.length > 30 ? <Tooltip title={value}>{`${value.substr(0, 30)}...`}</Tooltip> : value,
    },
    {
      title: i18n.t('time'),
      dataIndex: 'time',
      key: 'time',
      width: 280,
      render: (value: string) => moment(value).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: `${i18n.t('msp:maximum time consuming')}(ms)`,
      dataIndex: 'max',
      key: 'max',
      width: 140,
      render: (value: number) => value.toFixed(2),
    },
    {
      title: `${i18n.t('msp:minimum time consuming')}(ms)`,
      dataIndex: 'min',
      key: 'min',
      align: 'center',
      width: 140,
      render: (value: number) => value.toFixed(2),
    },
    {
      title: i18n.t('msp:number of occurrences'),
      dataIndex: 'count',
      key: 'count',
      width: 80,
    },
  ];
  return (
    <Table
      rowKey={(record: IData, i) => `${i}_${record.time}`}
      columns={columns}
      dataSource={list}
      scroll={{ x: '100%' }}
    />
  );
};

export default TablePanel;
