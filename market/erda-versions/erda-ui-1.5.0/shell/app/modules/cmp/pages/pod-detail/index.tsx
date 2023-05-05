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
import { useMount } from 'react-use';
import { map, isEmpty } from 'lodash';
import { Spin, Table } from 'antd';
import { Holder } from 'common';
import i18n from 'i18n';
import { useLoading } from 'core/stores/loading';
import routeInfoStore from 'core/stores/route';
import podDetailStore from '../../stores/pod-detail';
import { useInstanceOperation } from '../../common/components/instance-operation';

import { IInstances } from '../../services/pod-detail';

import './index.scss';

const SUMMARY_KEY_MAP = {
  podName: i18n.t('cmp:pod instance'),
  clusterName: i18n.t('cluster'),
  nodeName: i18n.t('node'),
  namespace: i18n.t('cmp:namespace'),
  hostIP: i18n.t('runtime:Host IP'),
  restartTotal: i18n.t('cmp:restart times'),
  stateCode: i18n.t('cmp:state code'),
  terminatedReason: i18n.t('cmp:exit reasons'),
};

const PodDetail = () => {
  const podDetail = podDetailStore.useStore((s) => s.podDetail);
  const { getPodDetail } = podDetailStore.effects;
  const [loading] = useLoading(podDetailStore, ['getPodDetail']);
  const { cluster_name } = routeInfoStore.useStore((s) => s.query);

  useMount(() => {
    getPodDetail();
  });

  const [renderOp, drawer] = useInstanceOperation<IInstances>({
    log: true,
    console: true,
    monitor: true,
    getProps(type) {
      return {
        console: {
          clusterName: cluster_name,
        },
        log: {
          fetchApi: '/api/orgCenter/logs',
          extraQuery: { clusterName: cluster_name },
          sourceType: 'container',
        },
        monitor: {
          api: '/api/orgCenter/metrics',
          extraQuery: { filter_cluster_name: cluster_name },
        },
      }[type];
    },
  });

  const columns = [
    {
      title: i18n.t('container ID'),
      dataIndex: 'containerId',
    },
    {
      title: i18n.t('runtime:Host IP'),
      dataIndex: 'hostIP',
      width: 110,
    },
    {
      title: i18n.t('start time'),
      dataIndex: 'startedAt',
    },
    {
      title: i18n.t('exit time'),
      dataIndex: 'finishedAt',
      render: (value: string) => <span>{value || '--'}</span>,
    },
    {
      title: i18n.t('exit code'),
      dataIndex: 'exitCode',
      width: 100,
    },
    {
      title: i18n.t('operations'),
      width: 180,
      render: renderOp,
    },
  ];

  return (
    <Spin spinning={loading}>
      <Holder when={isEmpty(podDetail.instances) && isEmpty(podDetail.summary)}>
        <div className="pod-detail">
          <div className="base-info mb-8">
            <span className="title font-medium">{i18n.t('basic information')}</span>
            <div className="info-grid">
              {map(SUMMARY_KEY_MAP, (label, key) => (
                <div key={key}>
                  <div className="param-k nowrap">{label}</div>
                  <div className="param-v nowrap">{podDetail.summary ? podDetail.summary[key] : '--'}</div>
                </div>
              ))}
            </div>
          </div>
          <div className="instance mb-8">
            <span className="title font-medium">{i18n.t('cmp:instance list')} TOP10</span>
            <Table
              rowKey="containerId"
              pagination={false}
              columns={columns}
              dataSource={podDetail.instances || []}
              scroll={{ x: '100%' }}
            />
            {drawer}
          </div>
        </div>
      </Holder>
    </Spin>
  );
};

export default PodDetail;
