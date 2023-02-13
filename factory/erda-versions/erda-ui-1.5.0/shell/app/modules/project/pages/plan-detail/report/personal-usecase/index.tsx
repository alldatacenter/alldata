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

import { map } from 'lodash';
import React from 'react';
import i18n from 'i18n';
import { ColumnProps } from 'core/common/interface';
import { Table } from 'antd';
import { UserInfo } from 'common';

import testPlanStore from 'project/stores/test-plan';

const PersonalUseCase = () => {
  const planReport = testPlanStore.useStore((s) => s.planReport);
  const { executorStatus } = planReport;
  const dataSource = map(executorStatus, (statusMap, userID) => ({ ...statusMap, userID }));

  const columns: Array<ColumnProps<object>> = [
    {
      title: i18n.t('dop:executor'),
      dataIndex: 'userID',
      key: 'userID',
      render: (text) => (text ? <UserInfo id={text} render={(data) => data.nick || data.name} /> : 'unallocated'),
    },
    {
      title: i18n.t('dop:not performed'),
      dataIndex: 'init',
      key: 'init',
      render: (value) => value || 0,
    },
    {
      title: i18n.t('passed'),
      dataIndex: 'succ',
      key: 'succ',
      render: (value) => value || 0,
    },
    {
      title: i18n.t('dop:not passed'),
      dataIndex: 'fail',
      key: 'fail',
      render: (value) => value || 0,
    },
    {
      title: i18n.t('dop:blocking'),
      dataIndex: 'block',
      key: 'block',
      render: (value) => value || 0,
    },
  ];

  return <Table rowKey="userID" columns={columns} dataSource={dataSource} scroll={{ x: '100%' }} />;
};

export default PersonalUseCase;
