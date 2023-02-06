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
import { Table } from 'antd';
import { get } from 'lodash';
import { goTo, qs } from 'common/utils';
import { getFormatter } from 'charts/utils/formatter';
import i18n from 'i18n';

interface IProps {
  data: object;
  valueTitle: string;
  unitType?: string;
  unit?: string;
  query: any;
}

export const topTable = ({ data, valueTitle, unitType, unit, query: { filter_host_ip, timestamp } }: IProps) => {
  const columns = [
    {
      title: `${i18n.t('cmp:process')} ID`,
      dataIndex: 'id',
    },
    {
      title: i18n.t('cmp:process name'),
      dataIndex: 'name',
    },
    {
      title: valueTitle,
      dataIndex: 'value',
      render: (value: number) => getFormatter(unitType, unit).format(value),
    },
  ];

  const handleRowClick = ({ id, name }: any) => {
    const queryMap = {
      ip: filter_host_ip,
      timestamp,
      name,
    };
    goTo(`./${id}?${qs.stringify(queryMap)}`);
  };

  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={get(data, 'list')}
      rowClassName={() => 'cursor-pointer'}
      onRowClick={handleRowClick}
      scroll={{ x: '100%' }}
    />
  );
};
