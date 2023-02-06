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
import { ColumnProps } from 'core/common/interface';
import { get } from 'lodash';
import i18n from 'i18n';

interface IData {
  [pro: string]: any;
  name: string;
  logId?: string;
  traceId?: string;
}

const ExceptionTracePanel = ({ data }: { data: object }) => {
  const list = get(data, 'results') || [];
  const dataSource = list.map((item: any) => item.data[0]);
  const columns: Array<ColumnProps<IData>> = [
    {
      title: i18n.t('msp:error code'),
      dataIndex: 'name',
      key: 'name',
      align: 'center',
      // render: (value, record) => <Link to={{ pathname: resolvePath(`./${record.traceId}`), query: { logId: record.logId, traceId: record.traceId } }}>{value}</Link>,
    },
    {
      title: 'logId',
      dataIndex: 'logId',
      key: 'logId',
      align: 'center',
      render: (value: string) => (value ? <Tooltip title={value}>{`${value.substr(0, 24)}...`}</Tooltip> : ''),
    },
    {
      title: 'traceId',
      dataIndex: 'traceId',
      key: 'traceId',
      align: 'center',
      render: (value: string) => (value ? <Tooltip title={value}>{`${value.substr(0, 24)}...`}</Tooltip> : ''),
    },
  ];
  return (
    <Table
      rowKey={(record: IData, i) => i + record.name}
      columns={columns}
      dataSource={dataSource}
      scroll={{ x: '100%' }}
    />
  );
};

export default ExceptionTracePanel;
