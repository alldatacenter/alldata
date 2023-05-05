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

import moment from 'moment';
import React from 'react';
import i18n from 'i18n';
import MoreOperation from './more-operation';
import { ColumnProps } from 'core/common/interface';
import { UserInfo } from 'common';

export const commonColumns: Array<ColumnProps<TEST_CASE.CaseTableRecord>> = [
  {
    title: '',
    dataIndex: 'checkbox',
    key: 'checkbox',
    width: 64,
  },
  {
    title: 'ID',
    dataIndex: 'id',
    key: 'id',
    width: 72,
    sorter: true,
  },
  {
    title: i18n.t('dop:use case title'),
    dataIndex: 'name',
    key: 'name',
    render: (value: string, record: any) => renderContent((value) => value, value, record),
  },
  {
    title: i18n.t('dop:priority'),
    dataIndex: 'priority',
    key: 'priority',
    width: 96,
    sorter: true,
    render: (value: string, record: any) => renderContent((value) => value, value, record),
  },
  {
    title: i18n.t('dop:updater'),
    dataIndex: 'updaterID',
    key: 'updaterID',
    width: 96,
    ellipsis: true,
    sorter: true,
    render: (value: string, record: any) =>
      renderContent(
        (updatedID: string, record: any) =>
          updatedID && <UserInfo id={updatedID} render={(data) => data.nick || data.name} />,
        value,
        record,
      ),
  },
  {
    title: i18n.t('dop:updated'),
    dataIndex: 'updatedAt',
    key: 'updatedAt',
    width: 200,
    sorter: true,
    render: (value: string, record: any) =>
      renderContent(
        (updatedAt: string, record: any) => updatedAt && moment(updatedAt).format('YYYY-MM-DD HH:mm:ss'),
        value,
        record,
      ),
  },
];

const renderContent = (children, value: string, record: any, dataIndex?: string) => {
  const obj = {
    children: children(value, record),
    props: {},
  };
  if (!record.id && dataIndex !== 'operation') {
    obj.props.colSpan = 0;
  }
  return obj;
};

export const columns: Array<ColumnProps<TEST_CASE.CaseTableRecord>> = [
  ...commonColumns,
  {
    title: i18n.t('operation'),
    dataIndex: 'operation',
    key: 'operation',
    className: 'operation',
    width: 160,
    fixed: 'right',
    render: (value: string, record: any) =>
      renderContent(
        (value: string, record: any) => record.id && <MoreOperation record={record} />,
        value,
        record,
        'operation',
      ),
  },
];
