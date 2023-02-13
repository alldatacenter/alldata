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
import { Table, Tooltip } from 'antd';
import { get } from 'lodash';
import moment from 'moment';
import { Copy } from 'common';
import { TraceExpandTable, onExpand } from './track-expand-table';
import EIAffairsStore from '../../stores/affairs';
import i18n from 'i18n';

import './slow-track-panel.scss';

interface ISlowTrackProps {
  data: MONITOR_EI.ITableData[];
  query: object;
  timeSpan: ITimeSpan;
  viewLog: (params: any) => void;
  fetchTraceContent: (params: any) => void;
}

export const webSlowTrackPanel = ({ data, query, timeSpan, viewLog, fetchTraceContent }: ISlowTrackProps) => {
  const subSlowHttpList = EIAffairsStore.useStore((s) => s.subSlowHttpList);
  const { getSubSlowHttpList } = EIAffairsStore.effects;
  const list = get(data, 'list') || [];
  const { startTimeMs: start, endTimeMs: end } = timeSpan || {};

  const columns = [
    {
      title: i18n.t('service'),
      dataIndex: 'name',
      key: 'name',
      width: 280,
      render: (value: string) => (
        <div className="ei-table-adaption">
          <Tooltip title={value}>
            <div className="ei-table-value">
              <Copy copyText={value}>{`${value}`}</Copy>
            </div>
          </Tooltip>
        </div>
      ),
    },
    {
      title: i18n.t('time'),
      dataIndex: 'time',
      key: 'time',
      width: 200,
      render: (value: string) => moment(value).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: `${i18n.t('dop:maximum time consuming')}(ms)`,
      dataIndex: 'max',
      key: 'max',
      width: 140,
      render: (value: number) => (value / 1000000).toFixed(2),
    },
    {
      title: `${i18n.t('dop:minimum time consuming')}(ms)`,
      dataIndex: 'min',
      key: 'min',
      width: 140,
      render: (value: number) => (value / 1000000).toFixed(2),
    },
    {
      title: i18n.t('msp:number of occurrences'),
      dataIndex: 'count',
      key: 'count',
      width: 80,
    },
  ];

  const expandedRowRender = (record: MONITOR_EI.ITableData) => {
    const { name, applicationId } = record;
    return (
      <TraceExpandTable
        viewLog={viewLog}
        applicationId={applicationId}
        fetchTraceContent={fetchTraceContent}
        recordKey={name}
        dataSource={subSlowHttpList}
        emptyText={i18n.t('msp:No slow transaction data sampled yet.')}
      />
    );
  };

  const onRowExpand = onExpand(getSubSlowHttpList, query, (record: any) => {
    const { name } = record;
    return { start, end, filter_source_service_name: name, filter_trace_sampled: true };
  });
  return (
    <Table
      scroll={{ x: 710 }}
      rowKey={(record: MONITOR_EI.ITableData, i) => i + record.name}
      columns={columns}
      dataSource={list}
      onExpand={onRowExpand}
      expandedRowRender={expandedRowRender}
    />
  );
};
