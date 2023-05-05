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
import { Table, Modal, Tooltip } from 'antd';
import i18n from 'i18n';
import moment from 'moment';
import runtimeServiceStore from 'runtime/stores/service';
import { useUpdate } from 'common/use-hooks';
import { ColumnProps } from 'core/common/interface';

interface IProps {
  runtimeID: number;
  service: string;
}

const PodTable = (props: IProps) => {
  const { runtimeID, service } = props;
  const podListMap = runtimeServiceStore.useStore((s) => s.podListMap);
  const podList = podListMap[service] || [];
  const [state, updater] = useUpdate({
    loading: false,
  });

  React.useEffect(() => {
    updater.loading(true);
    runtimeServiceStore
      .getServicePods({
        runtimeID,
        service,
      })
      .then(() => {
        updater.loading(false);
      });
  }, [service, runtimeID, updater]);

  const handleKill = (record: RUNTIME_SERVICE.Pod) => {
    const infoContent = (
      <div className="record-info">
        <div>{`${i18n.t('cmp:pod instance')}: ${record.podName}`}</div>
      </div>
    );

    const onOk = () =>
      runtimeServiceStore
        .killServicePod({
          runtimeID: +runtimeID,
          podName: record.podName,
        })
        .then(() => {
          runtimeServiceStore.getServicePods({
            runtimeID,
            service,
          });
        });

    Modal.confirm({
      title: i18n.t('runtime:confirm to delete the Pod'),
      content: infoContent,
      width: 500,
      onOk,
    });
  };

  const podTableColumn: Array<ColumnProps<RUNTIME_SERVICE.Pod>> = [
    {
      title: i18n.t('runtime:pod IP'),
      dataIndex: 'ipAddress',
      width: 120,
    },
    {
      title: i18n.t('cmp:pod instance'),
      dataIndex: 'podName',
      width: 160,
      render: (text: string) => <Tooltip title={text}>{text}</Tooltip>,
    },
    {
      title: i18n.t('status'),
      dataIndex: 'phase',
      width: 80,
    },
    {
      title: i18n.t('cmp:namespace'),
      dataIndex: 'k8sNamespace',
      width: 120,
      render: (text: string) => <Tooltip title={text}>{text}</Tooltip>,
    },
    {
      title: i18n.t('runtime:Host IP'),
      dataIndex: 'host',
      width: 120,
    },
    {
      title: i18n.t('runtime:message content'),
      dataIndex: 'message',
    },
    {
      title: i18n.t('create time'),
      width: 176,
      dataIndex: 'startedAt',
      className: 'th-time nowrap',
      render: (text: string) => (moment(text).isValid() ? moment(text).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: i18n.t('operation'),
      dataIndex: 'op',
      width: 96,
      fixed: 'right',
      render: (_v: any, record: RUNTIME_SERVICE.Pod) => {
        return (
          <div className="table-operations">
            <span className="table-operations-btn" onClick={() => handleKill(record)}>
              {i18n.t('stop')}
            </span>
          </div>
        );
      },
    },
  ];
  return (
    <Table
      scroll={{ x: 1100 }}
      loading={state.loading}
      columns={podTableColumn}
      dataSource={podList}
      rowKey="podName"
      pagination={{
        size: 'small',
        pageSize: 15,
      }}
    />
  );
};

export default PodTable;
