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
import moment from 'moment';
import { ColumnProps } from 'core/common/interface';
import { Avatar, Copy } from 'common';
import { Table, Tooltip } from 'antd';
import i18n from 'i18n';
import { useUserMap } from 'core/stores/userMap';

const typeMap = {
  BUILD: i18n.t('runtime:build'),
  ROLLBACK: i18n.t('runtime:rollback'),
  REDEPLOY: i18n.t('runtime:restart'),
  RELEASE: i18n.t('runtime:release deployment'),
};

interface IProps {
  opsCol?: ColumnProps<{ [prop: string]: any }>;
  dataSource: RUNTIME.DeployRecord[];
  paging: IPaging;
  loading: boolean;
  onChange?: (pageNo: number, pageSize?: number) => void;
}

const DeploymentTable = ({ dataSource, paging, loading, onChange, opsCol }: IProps) => {
  const userMap = useUserMap();
  const columns: Array<ColumnProps<{ [prop: string]: any }>> = [
    {
      title: i18n.t('runtime:deployment id'),
      dataIndex: 'id',
    },
    {
      title: i18n.t('operator'),
      dataIndex: 'operator',
      render: (operator: string) => {
        const { nick = '', avatar = '', name = '' } = userMap[operator] || {};
        return <Avatar className="mb-1" showName name={<Tooltip title={name}>{nick}</Tooltip>} url={avatar} />;
      },
    },
    {
      title: i18n.t('runtime:operation time'),
      dataIndex: 'createdAt',
      render: (text: string) => moment(text).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: i18n.t('runtime:deployment type'),
      dataIndex: 'type',
      render: (text: string) => typeMap[text],
    },
    {
      title: 'releaseID',
      dataIndex: 'releaseId',
      render: (text: string) => (
        <Copy selector=".cursor-copy">
          <span className="cursor-copy" data-clipboard-text={text}>
            {text}
          </span>
        </Copy>
      ),
    },
  ];

  opsCol && columns.push(opsCol);

  return (
    <Table
      columns={columns}
      dataSource={dataSource}
      rowKey="id"
      loading={loading}
      pagination={{
        // hideOnSinglePage: true,
        current: paging.pageNo,
        pageSize: paging.pageSize,
        total: paging.total,
        onChange,
      }}
      scroll={{ x: '100%' }}
    />
  );
};

export default DeploymentTable;
