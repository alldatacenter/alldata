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

import { getFormatter } from 'charts/utils/formatter';
import { Holder, MetricsMonitor } from 'common';
import i18n from 'i18n';
import { isEmpty, isFunction, map } from 'lodash';
import { Tag } from 'antd';
import React from 'react';
import './machine-detail.scss';

const itemConfigs = [
  {
    title: i18n.t('host name'),
    value: 'hostname',
  },
  {
    title: i18n.t('cluster'),
    value: 'clusterName',
  },
  {
    title: i18n.t('tags'),
    render: ({ labels }: any) => map((labels || '').split(','), (label) => <Tag key={label}>{label}</Tag>),
  },
  {
    title: 'CPU',
    value: 'cpuTotal',
    render: (cpus: number) => `${cpus} ${i18n.t('core')}`,
  },
  {
    title: i18n.t('memory'),
    value: 'memTotal',
    render: (memory: number) => `${getFormatter('STORAGE').format(memory)}`,
  },
  {
    title: i18n.t('disk'),
    value: 'diskTotal',
    render: (disk: number) => `${getFormatter('STORAGE').format(disk)}`,
  },
  {
    title: i18n.t('system version'),
    render: ({ os, kernelVersion }: any) => `${os} ${kernelVersion}`,
  },
];

export interface IProps {
  type: string;
  machineDetail: ORG_MACHINE.IMachine;
}

const MachineDetail = ({ type, machineDetail }: IProps) => {
  let Content = null;
  if (machineDetail) {
    switch (type) {
      case 'info':
        Content = map(itemConfigs, ({ title, value, render }) => (
          <div className="machine-detail-info-item mb-7" key={title}>
            <div className="label mb-2">{title}</div>
            <div className="value">
              {isFunction(render)
                ? render(value ? machineDetail[value] : machineDetail)
                : value
                ? machineDetail[value]
                : null}
            </div>
          </div>
        ));
        break;
      case 'insight':
        Content = (
          <MetricsMonitor
            resourceType="machine"
            resourceId={machineDetail.hostname}
            defaultTime={1}
            commonChartQuery={{
              filter_host_ip: machineDetail.ip,
              filter_cluster_name: machineDetail.clusterName,
              customAPIPrefix: '/api/orgCenter/metrics/',
            }}
          />
        );
        break;
      default:
        break;
    }
  }

  return <Holder when={isEmpty(machineDetail)}>{Content}</Holder>;
};

export default MachineDetail;
