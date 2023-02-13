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
import { get, round } from 'lodash';
import { Copy, IF } from 'common';
import i18n from 'i18n';

export const apiSizePanel = ({ data }: { data: object }) => {
  const list = get(data, 'list');
  const columns = [
    {
      title: 'API Path',
      dataIndex: 'name',
      key: 'name',
      render: (value: string) => (
        <IF check={value.length > 30}>
          <Tooltip title={value}>
            <Copy copyText={value}>{`${value.substr(0, 30)}...`}</Copy>
          </Tooltip>
          <IF.ELSE />
          <Copy>{value}</Copy>
        </IF>
      ),
    },
    {
      title: i18n.t('msp:transport bytes(KBytes)'),
      dataIndex: 'size',
      key: 'size',
      render: (size: number) => round(size, 3),
    },
  ];

  return <Table columns={columns} dataSource={list} scroll={{ x: '100%' }} />;
};
