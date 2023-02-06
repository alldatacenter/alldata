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
import { Drawer, Table, Breadcrumb, Popconfirm as PopConfirm } from 'antd';
import { map } from 'lodash';
import { TagsRow, TableActions, ErdaIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { ColumnProps } from 'core/common/interface';
import { useLoading } from 'core/stores/loading';
import machineManageStore from '../../stores/machine-manage';
import routeInfoStore from 'core/stores/route';
import MachineDetail from 'dcos/pages/cluster-dashboard/machine-detail';
import { DoubleProgressItem } from 'dcos/pages/machine-manager/machine-table';
import { useUnmount } from 'react-use';
import { goTo } from 'common/utils';
import 'dcos/pages/machine-manager/machine-table.scss';

const MachineManage = () => {
  const [{ drawerVisible, activeMachine }, updater, update] = useUpdate({
    drawerVisible: false,
    activeMachine: {} as ORG_MACHINE.IMachine,
  });

  const [{ siteName, clusterName }] = routeInfoStore.useStore((s) => [s.query]);
  const { id } = routeInfoStore.useStore((s) => s.params);

  const [isFetching] = useLoading(machineManageStore, ['getGroupInfos']);
  const { getGroupInfos, clearGroupInfos, offlineMachine } = machineManageStore;

  const [groupInfos] = machineManageStore.useStore((s) => [s.groupInfos]);

  useUnmount(() => {
    clearGroupInfos();
  });

  const getMachineList = React.useCallback(() => {
    getGroupInfos({
      groups: ['cluster'],
      clusters: [{ clusterName }],
      filters: [
        {
          key: 'edge_site',
          values: [siteName],
        },
      ],
    });
  }, [clusterName, getGroupInfos, siteName]);

  React.useEffect(() => {
    getMachineList();
  }, [getMachineList]);

  const tableList = React.useMemo(() => {
    const { machines } = groupInfos[0] || {};
    return map(machines, (m) => {
      return m;
    });
  }, [groupInfos]);

  const showMonitor = (record: ORG_MACHINE.IMachine) => {
    update({
      drawerVisible: true,
      activeMachine: record,
    });
  };

  const offlineHandle = (record: ORG_MACHINE.IMachine) => {
    offlineMachine({
      siteIP: record.ip,
      id: +id,
    }).then(() => {
      getMachineList();
    });
  };

  const columns: Array<ColumnProps<ORG_MACHINE.IMachine>> = [
    {
      title: 'IP',
      width: 160,
      dataIndex: 'ip',
    },
    {
      title: i18n.t('number of instance'),
      dataIndex: 'tasks',
      width: 176,
      sorter: (a: ORG_MACHINE.IMachine, b: ORG_MACHINE.IMachine) => Number(a.tasks) - Number(b.tasks),
    },
    {
      title: 'CPU',
      width: 120,
      dataIndex: 'cpuAllocatable',
      render: (_, data: ORG_MACHINE.IMachine) => {
        const { cpuAllocatable, cpuUsage, cpuRequest, cpuUsagePercent, cpuDispPercent } = data;
        return (
          <div className="percent-row">
            {DoubleProgressItem({
              usedPercent: Math.ceil(cpuUsagePercent),
              requestPercent: Math.ceil(cpuDispPercent),
              usage: cpuUsage,
              request: cpuRequest,
              total: cpuAllocatable,
              unit: i18n.t('core'),
            })}
          </div>
        );
      },
    },
    {
      title: i18n.t('memory'),
      width: 120,
      dataIndex: 'memProportion',
      render: (_, data: ORG_MACHINE.IMachine) => {
        const { memAllocatable, memUsage, memRequest, memUsagePercent, memDispPercent } = data;
        return (
          <div className="percent-row">
            {DoubleProgressItem({
              usedPercent: Math.ceil(memUsagePercent),
              requestPercent: Math.ceil(memDispPercent),
              usage: memUsage,
              request: memRequest,
              total: memAllocatable,
              unitType: 'STORAGE',
            })}
          </div>
        );
      },
    },
    {
      title: <span className="main-title">{i18n.t('tags')} </span>,
      dataIndex: 'labels',
      className: 'machine-labels',
      render: (value: string) => {
        const keyArray = value?.split(',') || [];
        return (
          <TagsRow
            labels={keyArray.map((label) => {
              return { label };
            })}
          />
        );
      },
    },
    {
      title: i18n.t('operations'),
      dataIndex: 'id',
      key: 'operation',
      width: 180,
      fixed: 'right',
      render: (_id: string, record: ORG_MACHINE.IMachine) => {
        return (
          <TableActions>
            <span className="table-operations-btn" onClick={() => showMonitor(record)}>
              {i18n.t('machine overview')}
            </span>
            <PopConfirm title={`${i18n.t('confirm to go offline')}?`} onConfirm={() => offlineHandle(record)}>
              <span className="table-operations-btn">{i18n.t('msp:offline')}</span>
            </PopConfirm>
          </TableActions>
        );
      },
    },
  ];

  const onCloseDrawer = React.useCallback(() => {
    updater.drawerVisible(false);
  }, [updater]);
  return (
    <div className="machine-table">
      <Breadcrumb
        separator={<ErdaIcon className="align-middle" type="right" size="14px" />}
        className="path-breadcrumb mb-2"
      >
        <Breadcrumb.Item className="hover-active" onClick={() => goTo(goTo.pages.ecpResource)}>
          {siteName}
        </Breadcrumb.Item>
        <Breadcrumb.Item>{i18n.t('cmp:node list')}</Breadcrumb.Item>
      </Breadcrumb>

      <Table
        className="machine-list-table"
        loading={isFetching}
        rowKey="ip"
        pagination={false}
        bordered
        columns={columns}
        dataSource={tableList}
        scroll={{ x: 1300 }}
      />
      <Drawer
        width="80%"
        visible={drawerVisible}
        title={i18n.t('machine overview')}
        destroyOnClose
        onClose={onCloseDrawer}
      >
        <MachineDetail type="insight" machineDetail={activeMachine} />
      </Drawer>
    </div>
  );
};

export default MachineManage;
