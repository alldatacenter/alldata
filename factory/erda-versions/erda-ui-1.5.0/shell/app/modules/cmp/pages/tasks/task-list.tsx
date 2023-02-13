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
import moment from 'moment';
import { Table, Badge, message } from 'antd';
import { goTo } from 'common/utils';
import { TASKS_STATUS_MAP, WORKSPACE_MAP } from './config';
import { ClusterSelector } from 'app/modules/cmp/common/components/cluster-selector';
import clusterStore from 'cmp/stores/cluster';
import { useLoading } from 'core/stores/loading';
import clusterTaskStore from 'app/modules/cmp/stores/task';
import { useEffectOnce } from 'react-use';
import { useUserMap } from 'core/stores/userMap';
import orgStore from 'app/org-home/stores/org';

const getClusterTasksCols = (userMap: object) => {
  return [
    {
      title: i18n.t('environment'),
      dataIndex: 'env',
      key: 'env',
      render: (env: string) => WORKSPACE_MAP[env],
    },
    {
      title: i18n.t('cluster name'),
      dataIndex: 'clusterName',
      key: 'clusterName',
    },
    {
      title: i18n.t('project'),
      dataIndex: 'projectName',
      key: 'projectName',
    },
    {
      title: i18n.t('application'),
      dataIndex: 'applicationName',
      key: 'applicationName',
    },
    {
      title: i18n.t('creator'),
      dataIndex: 'userID',
      key: 'userID',
      render: (id: string) => {
        return id ? userMap[id]?.nick || userMap[id]?.name || '--' : '--';
      },
    },
    {
      title: i18n.t('created at'),
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 200,
      render: (createdAt: string) => `${moment(createdAt).format('YYYY-MM-DD HH:mm:ss')}`,
    },
    {
      title: i18n.t('status'),
      dataIndex: 'status',
      key: 'status',
      width: 120,
      fixed: 'right',
      render: (status: string) => (
        <Badge status={TASKS_STATUS_MAP[status].state} text={TASKS_STATUS_MAP[status].name} />
      ),
    },
  ];
};

interface IProps {
  taskType: string;
}

const ServicesList = ({ taskType }: IProps) => {
  const orgClusterList = clusterStore.useStore((s) => s.list);
  const currentOrg = orgStore.useStore((s) => s.currentOrg);
  const [list, { pageNo, pageSize, total }] = clusterTaskStore.useStore((s) => [s.list, s.paging]);
  const { getTaskList } = clusterTaskStore.effects;
  const { resetState } = clusterTaskStore.reducers;
  const userMap = useUserMap();
  const [loading] = useLoading(clusterTaskStore, ['getTaskList']);
  const [cluster, setCluster] = React.useState();

  useEffectOnce(() => {
    clusterStore.effects.getClusterList({ orgId: currentOrg.id });
    return () => resetState();
  });

  React.useEffect(() => {
    getTaskList({ type: taskType, pageNo: 1, pageSize });
  }, [getTaskList, pageSize, taskType]);

  const handleClusterChange = (val: string) => {
    setCluster(val || undefined);
    getTaskList({ type: taskType, pageNo: 1, pageSize, cluster: val || undefined });
  };

  const handlePageChange = (value: number) => {
    getTaskList({ type: taskType, pageNo: value, pageSize, cluster });
  };

  let extraTableAttr = {};
  if (taskType === 'deployment') {
    extraTableAttr = {
      rowClassName: 'cursor-pointer',
      onRow: ({ projectID, applicationID, runtimeID }: any) => {
        return {
          onClick: () => {
            if (!runtimeID) {
              message.warning(i18n.t('no running runtime'));
              return;
            }
            runtimeID &&
              goTo(goTo.pages.runtimeDetail, {
                jumpOut: true,
                projectId: projectID,
                appId: applicationID,
                runtimeId: runtimeID,
              });
          },
        };
      },
    };
  }

  return (
    <>
      <div className="mb-4">
        <ClusterSelector clusterList={orgClusterList} onChange={handleClusterChange} />
      </div>
      <Table
        rowKey="taskID"
        loading={loading}
        columns={getClusterTasksCols(userMap)}
        dataSource={list}
        pagination={{
          pageSize,
          current: pageNo,
          total,
          onChange: handlePageChange,
        }}
        scroll={{ x: 1200 }}
        {...extraTableAttr}
      />
    </>
  );
};

export default ServicesList;
