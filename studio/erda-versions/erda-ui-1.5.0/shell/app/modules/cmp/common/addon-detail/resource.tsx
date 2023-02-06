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
import i18n from 'i18n';
import { Table, Badge, Tooltip } from 'antd';
import moment from 'moment';
import { Copy } from 'common';
import { getFormatter } from 'app/charts/utils/formatter';
import { ColumnProps } from 'core/common/interface';
import { PAGINATION } from 'app/constants';

type IResource = MIDDLEWARE_DASHBOARD.IResource;

interface IProps {
  resourceList: MIDDLEWARE_DASHBOARD.IResource[];
  loading: boolean;
  drawerComp: JSX.Element;
  renderOp: (record: any) => JSX.Element;
}
export const PureResourceList = ({ renderOp, resourceList, loading, drawerComp }: IProps) => {
  const resourceCols: Array<ColumnProps<IResource>> = [
    {
      title: i18n.t('container IP'),
      dataIndex: 'containerIP',
      key: 'containerIP',
      width: 160,
      fixed: 'left',
    },
    {
      title: i18n.t('host IP'),
      dataIndex: 'hostIP',
      key: 'hostIP',
      width: 160,
    },
    {
      title: i18n.t('cpu limit'),
      dataIndex: 'cpuLimit',
      key: 'cpuLimit',
      width: 120,
      sorter: (a: IResource, b: IResource) => a.cpuLimit - b.cpuLimit,
    },
    {
      title: i18n.t('CPU allocation'),
      dataIndex: 'cpuRequest',
      key: 'cpuRequest',
      width: 120,
      sorter: (a: IResource, b: IResource) => a.cpuRequest - b.cpuRequest,
    },
    {
      title: i18n.t('memory limit'),
      dataIndex: 'memLimit',
      key: 'memLimit',
      width: 120,
      render: (v: number) => getFormatter('CAPACITY', 'MB').format(v),
      sorter: (a: IResource, b: IResource) => a.memLimit - b.memLimit,
    },
    {
      title: i18n.t('cmp:Memory allocation'),
      dataIndex: 'memRequest',
      key: 'memRequest',
      width: 120,
      render: (v: number) => getFormatter('CAPACITY', 'MB').format(v),
      sorter: (a: IResource, b: IResource) => a.memRequest - b.memRequest,
    },
    {
      title: i18n.t('image'),
      dataIndex: 'image',
      key: 'image',
      render: (image: string) => (
        <Tooltip title={image}>
          <Copy className="cursor-copy" data-clipboard-tip={i18n.t('image')} data-clipboard-text={image}>
            {image}
          </Copy>
        </Tooltip>
      ),
    },
    {
      title: i18n.t('start time'),
      dataIndex: 'startedAt',
      key: 'startedAt',
      width: 200,
      render: (v: string) => moment(v).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: i18n.t('status'),
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (v: string) =>
        v === 'Healthy' ? (
          <>
            <Badge status="success" />
            {i18n.t('healthy')}
          </>
        ) : v === 'UnHealthy' ? (
          <>
            <Badge status="warning" />
            {i18n.t('abnormal')}
          </>
        ) : (
          <>
            <Badge status="default" />
            {i18n.t('stopped')}
          </>
        ),
    },
    {
      title: i18n.t('operations'),
      key: 'operation',
      width: 176,
      fixed: 'right',
      render: renderOp,
    },
  ];

  return (
    <>
      <Table
        rowKey="containerId"
        columns={resourceCols}
        dataSource={resourceList}
        loading={loading}
        pagination={{ pageSize: PAGINATION.pageSize }}
        scroll={{ x: 1500 }}
      />
      {drawerComp}
    </>
  );
};
