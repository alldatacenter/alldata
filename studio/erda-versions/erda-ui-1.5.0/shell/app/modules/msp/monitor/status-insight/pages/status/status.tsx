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
import { Button, Modal, Tooltip, Select } from 'antd';
import { goTo, cutStr, resolvePath } from 'common/utils';
import Table from 'common/components/table';
import { Badge, ErdaIcon } from 'common';
import { IActions } from 'common/components/table/interface';
import { reverse, map, filter, floor } from 'lodash';
import { useUpdate } from 'common/use-hooks';
import StatusChart from './status-chart';
import AddModal from './add-modal';
import monitorStatusStore from 'status-insight/stores/status';
import { useLoading } from 'core/stores/loading';
import routeInfoStore from 'core/stores/route';
import { useEffectOnce } from 'react-use';
import i18n from 'i18n';

import './status.scss';

const { Option } = Select;

const Status = () => {
  const query = routeInfoStore.useStore((s) => s.query);
  const dashboard = monitorStatusStore.useStore((s) => s.dashboard);
  const [isFetchingList] = useLoading(monitorStatusStore, ['getProjectDashboard']);
  const { getProjectDashboard, deleteMetric } = monitorStatusStore.effects;
  const { clearDashboardInfo } = monitorStatusStore.reducers;

  const { type = 'All' } = query || {};

  const [{ modalVisible, formData, filterType }, updater, update] = useUpdate({
    modalVisible: false,
    formData: null as MONITOR_STATUS.IMetricsBody | null,
    filterType: type,
  });

  useEffectOnce(() => {
    getProjectDashboard();
    return () => {
      clearDashboardInfo();
    };
  });

  const toggleModal = (_data?: MONITOR_STATUS.IMetricsBody) => {
    update({
      modalVisible: !modalVisible,
      formData: _data,
    });
  };

  const handleEdit = (_data: MONITOR_STATUS.IMetricsBody) => {
    toggleModal(_data);
  };

  const handleDelete = (id: string) => {
    Modal.confirm({
      title: i18n.t('msp:are you sure to delete this monitor?'),
      onOk: () => {
        deleteMetric(id).then(() => {
          getProjectDashboard();
        });
      },
    });
  };

  const handleDetailClick = (e: any, path: string) => {
    e.stopPropagation();
    goTo(path);
  };

  const changeType = (_type: any) => {
    updater.filterType(_type);
  };

  let data = [] as any[];
  if (dashboard && dashboard.metrics) {
    const curMetrics = dashboard.metrics;
    data = reverse(Object.keys(curMetrics)).map((id) => {
      return {
        id,
        ...(curMetrics[id] || {}),
      };
    });
  }
  let filterData = data;
  if (filterType !== 'All') {
    filterData = filter(filterData, (item) => item.status === filterType);
  }

  const typeMap = {
    All: {
      text: i18n.t('msp:all'),
      status: 'success',
      color: 'green',
    },
    Operational: {
      text: i18n.t('msp:normal'),
      status: 'success',
      color: 'green',
    },
    'Major Outage': {
      text: i18n.t('msp:downtime'),
      status: 'error',
      color: 'red',
    },
    Miss: {
      text: i18n.t('msp:no data'),
      status: 'default',
      color: 'grey',
    },
  };

  const defaultMap = {
    text: i18n.t('msp:no data'),
    color: 'grey',
  };

  const columns = [
    {
      title: i18n.t('msp:index'),
      dataIndex: 'name',
      render: (text: string) => (
        <span className="name-link">
          <Tooltip title={text}>{cutStr(text, 25)}</Tooltip>
        </span>
      ),
    },
    {
      title: i18n.t('status'),
      dataIndex: 'status',
      render: (status: string) => (
        <>
          <Badge status={(typeMap[status] || defaultMap).status} text={(typeMap[status] || defaultMap).text} />
        </>
      ),
    },
    {
      title: i18n.t('msp:online rate'),
      dataIndex: 'uptime',
    },
    {
      title: `${i18n.t('msp:downtime')}(${i18n.t('msp:nearly 1 hour')})`,
      dataIndex: 'downDuration',
    },
    {
      title: 'Apdex',
      dataIndex: 'apdex',
      render: (text: string) => text && floor(+text, 2),
    },
    {
      title: i18n.t('msp:average response time'),
      dataIndex: 'latency',
      render: (text: string) => <span>{text} ms</span>,
    },
    {
      title: `${i18n.t('msp:response map')}(${i18n.t('msp:nearly 1 hour')})`,
      dataIndex: 'chart',
      width: 160,
      render: (_text: string, record: MONITOR_STATUS.IMetricsBody) => {
        const { chart } = record;
        if (!chart) {
          return <div style={{ height: '58px' }} />;
        }
        return (
          <div>
            <StatusChart
              xAxisData={chart.time}
              data={chart.latency}
              style={{ width: '120px', height: '40px', minHeight: 0 }}
            />
            <ul className="status-list">
              {chart.status.map((item: string, i: number) => (
                <li key={String(i)} className={typeMap[item].color} />
              ))}
            </ul>
          </div>
        );
      },
    },
    {
      title: i18n.t('msp:root cause analysis'),
      dataIndex: 'requestId',
      width: 90,
      hidden: true,
      render: (requestId: string) =>
        !!requestId && (
          <span
            className="reason-analysis-span"
            onClick={(e) => {
              handleDetailClick(e, resolvePath(`../error/request-detail/${requestId}`));
            }}
          >
            {i18n.t('msp:details')}
          </span>
        ),
    },
  ];

  const actions: IActions<MONITOR_STATUS.IMetricsBody> = {
    render: (record: MONITOR_STATUS.IMetricsBody) => renderMenu(record),
  };

  const renderMenu = (record: MONITOR_STATUS.IMetricsBody) => {
    const { editMonitor, deleteMonitor } = {
      editMonitor: {
        title: i18n.t('edit'),
        onClick: () => handleEdit(record),
      },
      deleteMonitor: {
        title: i18n.t('delete'),
        onClick: () => handleDelete(record.id),
      },
    };

    return [editMonitor, deleteMonitor];
  };

  const filterSlot = (
    <div className="status-button-group-left">
      <Select onChange={changeType} value={filterType} className="type-filter">
        {map(typeMap, (item, key) => {
          return (
            <Option key={key} value={key}>
              {item.text}
            </Option>
          );
        })}
      </Select>
    </div>
  );

  let hasDown = {
    text: 'no Data',
    color: '',
  };
  if (dashboard.downCount !== undefined) {
    hasDown =
      dashboard.downCount > 0
        ? { text: `${dashboard.downCount} Down`, color: 'red' }
        : { text: 'All up', color: 'green' };
  }

  return (
    <div className="project-status-page">
      <div className="status-button-group">
        {/* <Button className="account-button" type="primary" ghost onClick={() => goTo('./account')}>{i18n.t('msp:authentication management')}</Button> */}
        <Button className="add-button" type="primary" onClick={() => toggleModal()}>
          {i18n.t('msp:add monitoring')}
        </Button>
      </div>
      <div className="top-bar">
        <span className={`summary-down-count ${hasDown.color}`}>
          <span className="flex items-center justify-center">
            <ErdaIcon type="info" className="mr-2.5" size="16" /> {hasDown.text}{' '}
          </span>
        </span>
      </div>
      <AddModal
        modalVisible={modalVisible}
        toggleModal={toggleModal}
        formData={formData}
        afterSubmit={getProjectDashboard}
      />
      <Table
        rowKey="id"
        rowClassName={() => 'row-click'}
        onRow={(record) => {
          return {
            onClick: () => goTo(`./${record.id}`),
          };
        }}
        actions={actions}
        loading={isFetchingList}
        columns={columns}
        dataSource={filterData}
        slot={filterSlot}
        onChange={() => getProjectDashboard()}
      />
    </div>
  );
};

export default Status;
