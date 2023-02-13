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

import React, { useEffect } from 'react';
import i18n from 'i18n';
import { ceil, set } from 'lodash';
import { Table, Tooltip } from 'antd';
import { Copy } from 'common';
import { getBrowserInfo } from 'common/utils';
import { ColumnProps } from 'core/common/interface';
import { getFormatter } from 'charts/utils/formatter';
import clusterDashboardStore from '../../stores/dashboard';
import { useLoading } from 'core/stores/loading';
import { useInstanceOperation } from 'app/modules/cmp/common/components/instance-operation';
import './instance-list.scss';
import { PAGINATION } from 'app/constants';
import orgStore from 'app/org-home/stores/org';

const getImageText = (text: string) => {
  const headTxt = text.substr(0, 5);
  const tailTxt = text.substr(5);
  return (
    <div className="image-txt-container">
      <span className="head">{headTxt}</span>
      <span className={`tail nowrap ${getBrowserInfo().isSafari ? 'hack-safari' : ''}`}>{tailTxt}</span>
    </div>
  );
};

interface IProps {
  clusters: any[];
  instanceType: string;
  // onClickMachine(payload: any): void;
}

interface IInstance {
  [prop: string]: any;
  containerId: string;
  id: string;
  status: string;
  host: string;
}

const InstanceList = ({
  clusters,
  instanceType,
}: // onClickMachine,
IProps) => {
  const orgName = orgStore.useStore((s) => s.currentOrg.name);
  const [serviceList, jobList, instanceList] = clusterDashboardStore.useStore((s) => [
    s.serviceList,
    s.jobList,
    s.instanceList,
  ]);
  const [loading] = useLoading(clusterDashboardStore, ['getInstanceList']);
  const { getInstanceList } = clusterDashboardStore.effects;

  const [renderOp, drawer] = useInstanceOperation<IInstance>({
    log: true,
    console: true,
    monitor: true,
    getProps(type, record) {
      let fetchApi;
      if (instanceType === 'job') {
        set(record, 'id', record.jobId);
        fetchApi = '/api/orgCenter/job/logs';
      } else {
        set(record, 'id', record.instanceId || record.containerId);
        fetchApi = '/api/orgCenter/logs';
      }
      return {
        log: {
          fetchApi,
          extraQuery: { clusterName: record.clusterName },
          sourceType: instanceType === 'job' ? 'job' : 'container',
        },
        monitor: {
          api: '/api/orgCenter/metrics',
          extraQuery: { filter_cluster_name: record.clusterName },
        },
      }[type];
    },
  });

  useEffect(() => {
    const payload: Merge<ORG_DASHBOARD.IInstanceListQuery, { isWithoutOrg: boolean }> = {
      clusters,
      instanceType,
      isWithoutOrg: true,
    };
    if (instanceType !== 'all') {
      payload.filters = [
        {
          key: 'org_name',
          values: [orgName],
        },
      ];
    }
    getInstanceList(payload);
  }, [instanceType, clusters, getInstanceList, orgName]);

  const instanceMap = {
    service: serviceList,
    job: jobList,
    all: instanceList,
  };

  const instanceTypeColMap = {
    job: [
      {
        title: i18n.t('task ID'),
        dataIndex: 'jobId',
        render: (id: string) => <Tooltip title={id}>{id || '--'}</Tooltip>,
      },
    ],
    service: [
      {
        title: i18n.t('service name'),
        dataIndex: 'serviceName',
        render: (name: string) => <Tooltip title={name}>{name || '--'}</Tooltip>,
      },
      {
        title: i18n.t('service position'),
        dataIndex: 'projectName',
        render: (_: any, { projectName, applicationName, runtimeName }: any) => (
          <Tooltip
            title={`${i18n.t('project')}/${i18n.t('application')}/${i18n.t(
              'instance',
            )}：${projectName}/${applicationName}/${runtimeName}`}
          >
            {`${projectName}/${applicationName}/${runtimeName}`}
          </Tooltip>
        ),
      },
    ],
    all: [
      {
        title: i18n.t('image'),
        key: 'image',
        dataIndex: 'image',
        width: 400,
        className: 'item-image',
        render: (image: string) => {
          if (!image) return null;

          return (
            <Tooltip title={`${i18n.t('click to copy')}：${image}`} overlayClassName="tooltip-word-break">
              <span
                className="image-name for-copy-image"
                data-clipboard-tip={i18n.t('image name')}
                data-clipboard-text={image}
              >
                {getImageText(image)}
              </span>
            </Tooltip>
          );
        },
      },
    ],
  };

  const cols = [
    // {
    //   title: i18n.t('host IP'),
    //   dataIndex: 'hostIP',
    //   render: (ip: string, record: any) => (ip ? <span className="hover-text" onClick={() => { onClickMachine({ ip, clusterName: record.clusterName }); }}>{ ip }</span> : '--'),
    // },
    {
      title: i18n.t('cluster'),
      dataIndex: 'clusterName',
    },
    {
      title: i18n.t('cmp:CPU usage'),
      dataIndex: 'cpuUsage',
      sorter: (a: any, b: any) => a.cpuUsage - b.cpuUsage,
      render(cpuUsage: number, { cpuRequest }: any) {
        return (
          <Tooltip
            title={
              <div className="table-tooltip">
                {i18n.t('usage')}
                <span>
                  {ceil(cpuUsage, 2)} {i18n.t('core')}
                </span>{' '}
                <br />
                {`${i18n.t('allocated')}${ceil(cpuRequest, 2)}`} {i18n.t('core')}
              </div>
            }
          >
            {`${ceil(cpuUsage, 2)} ${i18n.t('core')}`}
          </Tooltip>
        );
      },
    },
    {
      title: i18n.t('cmp:Memory usage'),
      sorter: (a: any, b: any) => a.memUsage - b.memUsage,
      dataIndex: 'memRequest',
      render(_: any, { memRequest, memUsage }: any) {
        return (
          <Tooltip
            title={
              <div className="table-tooltip">
                {i18n.t('usage')}
                <span>{getFormatter('STORAGE').format(memUsage, 2)}</span> <br />
                {`${i18n.t('allocated')}${getFormatter('STORAGE').format(memRequest, 2)}`}
              </div>
            }
          >
            {getFormatter('STORAGE').format(memUsage, 2)}
          </Tooltip>
        );
      },
    },
    {
      title: i18n.t('cmp:Disk usage'),
      dataIndex: 'diskUsage',
      render: (diskUsage: number) => getFormatter('STORAGE').format(diskUsage),
    },
    {
      title: i18n.t('operations'),
      width: 250,
      render: renderOp,
    },
  ];

  return (
    <div className="table-wraper">
      <Table
        rowKey={(record: any, i: number) => `${i}${record.hostIP}`}
        columns={[...instanceTypeColMap[instanceType], ...cols] as Array<ColumnProps<any>>}
        dataSource={instanceMap[instanceType]}
        loading={loading}
        pagination={{
          pageSize: PAGINATION.pageSize,
        }}
        scroll={{ x: '100%' }}
      />
      <Copy selector=".for-copy-image" />
      {drawer}
    </div>
  );
};

export default InstanceList;
