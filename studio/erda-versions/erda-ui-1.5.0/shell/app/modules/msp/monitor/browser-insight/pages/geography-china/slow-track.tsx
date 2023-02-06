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
import { get } from 'lodash';
import { Table } from 'antd';
import moment from 'moment';
import i18n from 'i18n';

interface IData {
  [pro: string]: any;
  name: string;
  time?: string;
  max?: string;
  min?: string;
}

const SlowTrack = ({ data }: { data: object }) => {
  const list = get(data, 'list') || [];
  const columns = [
    {
      title: i18n.t('msp:domain name'),
      dataIndex: 'name',
      key: 'name',
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
    },
    {
      title: `${i18n.t('msp:minimum time consuming')}(ms)`,
      dataIndex: 'min',
      key: 'min',
    },
  ];
  return (
    <Table rowKey={(record: IData, i) => i + record.name} columns={columns} dataSource={list} scroll={{ x: '100%' }} />
  );
};

export default SlowTrack;
