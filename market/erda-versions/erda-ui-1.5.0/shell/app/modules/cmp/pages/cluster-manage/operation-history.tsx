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

import { ColumnProps } from 'core/common/interface';
import { useLoading } from 'core/stores/loading';
import { Icon as CustomIcon, LogRoller, FilterGroup } from 'common';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import moment from 'moment';
import { Table, Drawer, Badge, Tooltip, Switch } from 'antd';
import machineStore from 'app/modules/cmp/stores/machine';
import React from 'react';
import { useUserMap } from 'core/stores/userMap';
import routeInfoStore from 'core/stores/route';
import { cutStr } from 'common/utils';
import clusterStore from 'app/modules/cmp/stores/cluster';
import { useMount } from 'react-use';
import { map, isEmpty } from 'lodash';
import { ClusterLog } from './cluster-log';
import orgStore from 'app/org-home/stores/org';

export const OperationHistory = () => {
  const currentOrg = orgStore.useStore((s) => s.currentOrg);
  const clusterList = clusterStore.useStore((s) => s.list);
  const [operationList, operationPaging, operationTypes] = machineStore.useStore((s) => [
    s.operationList,
    s.operationPaging,
    s.operationTypes,
  ]);
  const { getClusterOperationHistory } = machineStore.effects;
  const [loading] = useLoading(machineStore, ['getClusterOperationHistory']);
  const { clusterName: queryCluster, scope, recordType: recordTypeQuery } = routeInfoStore.getState((s) => s.query);

  const userMap = useUserMap();

  const [{ curRow, filters }, updater] = useUpdate({
    curRow: null,
    filters: { clusterName: queryCluster, recordType: recordTypeQuery ? [recordTypeQuery] : undefined },
  });

  useMount(() => {
    machineStore.effects.getClusterOperationTypes();

    if (!clusterList.length) {
      clusterStore.effects.getClusterList({ sys: true });
    }
  });

  const getList = React.useCallback(
    (extra = {}) => {
      const { recordType, clusterName, ...rest } = extra;
      getClusterOperationHistory({
        clusterName: !isEmpty(clusterName) ? clusterName : undefined,
        orgID: currentOrg.id,
        pageSize: operationPaging.pageSize,
        recordType: recordType ? recordType.join() : undefined,
        scope: scope || undefined,
        ...rest,
      });
    },
    [getClusterOperationHistory, currentOrg.id, operationPaging.pageSize, scope],
  );

  React.useEffect(() => {
    if (clusterList.length) {
      getList({ pageNo: 1, ...filters });
    }
  }, [clusterList, filters, getList]);

  const columns: Array<ColumnProps<any>> = [
    {
      title: 'ID',
      dataIndex: 'recordID',
      width: 96,
    },
    {
      title: i18n.t('cluster name'),
      dataIndex: 'clusterName',
      width: 120,
    },
    {
      title: `${i18n.t('operation')}${i18n.t('name')}`,
      dataIndex: 'recordType',
      width: 160,
      render: (val: string) => {
        return <Tooltip title={val}>{val}</Tooltip>;
      },
    },
    {
      title: i18n.t('status'),
      dataIndex: 'status',
      width: 80,
      render: (status: string) => {
        const statusMap = {
          success: 'success',
          failed: 'error',
          processing: 'processing',
        };
        return <Badge status={statusMap[status] || 'default'} />;
      },
    },
    {
      title: i18n.t('time'),
      dataIndex: 'createTime',
      width: 200,
      render: (createTime: string) => {
        return <span>{moment(createTime).format('YYYY-MM-DD HH:mm:ss')}</span>;
      },
    },
    {
      title: i18n.t('user'),
      dataIndex: 'userID',
      width: 120,
      render: (id: string) =>
        userMap[id] ? <span>{cutStr(userMap[id].nick || userMap[id].name, 8, { showTip: true })}</span> : null,
    },
    {
      title: i18n.t('detail'),
      dataIndex: 'detail',
      render: (detail: string) => (
        <Tooltip title={detail}>
          <div className="nowrap">{detail}</div>
        </Tooltip>
      ),
    },
    {
      title: i18n.t('operation'),
      width: 120,
      render: (_, record) => {
        const hasLog = !isEmpty(record.pipelineDetail);
        return hasLog ? (
          <CustomIcon
            type="log"
            className="cursor-pointer"
            onClick={() => {
              updater.curRow(record);
            }}
          />
        ) : null;
      },
    },
  ];

  const changeFilter = (filtersObj: any) => {
    const _recordType = filtersObj.recordType;
    if (_recordType && !Array.isArray(_recordType)) {
      updater.filters({ ...filtersObj, recordType: [_recordType] });
    } else {
      updater.filters(filtersObj);
    }
  };

  return (
    <div>
      <FilterGroup
        list={[
          {
            name: 'clusterName',
            type: 'select',
            placeholder: i18n.t('cmp:please select cluster'),
            options: map(clusterList, (c) => ({ name: c.name, value: c.name })),
            style: { width: '260px' },
            mode: 'multiple',
            value: filters ? filters.clusterName : undefined,
          },
          {
            name: 'recordType',
            type: 'select',
            placeholder: i18n.t('cmp:please select operation type'),
            allowClear: true,
            mode: 'multiple',
            options: map(operationTypes, (o) => ({ name: o.recordType, value: o.rawRecordType })),
          },
        ]}
        onChange={changeFilter}
      />
      <Table
        rowKey="recordID"
        columns={columns}
        loading={loading}
        dataSource={operationList}
        pagination={{
          current: operationPaging.pageNo,
          pageSize: operationPaging.pageSize,
          total: operationPaging.total,
          onChange: (no: number) => getList({ pageNo: no, ...filters }),
        }}
        scroll={{ x: 1100 }}
      />
      <ClusterLog recordID={curRow && curRow.recordID} onClose={() => updater.curRow(null)} />
    </div>
  );
};

export const OperationLog = ({
  recordID,
  onClose,
  StepList,
}: {
  recordID?: string;
  StepList?: any;
  onClose: () => void;
}) => {
  const [state, updater] = useUpdate({
    isStdErr: false,
  });

  const switchLog = (
    <Switch
      checkedChildren={i18n.t('error')}
      unCheckedChildren={i18n.t('standard')}
      checked={state.isStdErr}
      onChange={updater.isStdErr}
    />
  );
  const stream = state.isStdErr ? 'stderr' : 'stdout';
  return (
    <Drawer title={i18n.t('operation log')} visible={!!recordID} onClose={onClose} width="60%" destroyOnClose>
      {StepList}
      <LogRoller
        key={stream}
        query={{
          fetchApi: '/api/node-logs',
          recordID,
          stream,
        }}
        extraButton={switchLog}
        logKey={`cluster-op-${recordID}`}
      />
    </Drawer>
  );
};
