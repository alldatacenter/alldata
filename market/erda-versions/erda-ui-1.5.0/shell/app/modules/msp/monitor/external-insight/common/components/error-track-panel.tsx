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

interface IErrorTrackProps {
  data: MONITOR_EI.ITableData[];
  query: object;
  timeSpan: ITimeSpan;
  viewLog: (params: any) => void;
  fetchTraceContent: (params: any) => void;
}

export const webErrorTrackPanel = ({ data, query, timeSpan, viewLog, fetchTraceContent }: IErrorTrackProps) => {
  const subErrorHttpList = EIAffairsStore.useStore((s) => s.subErrorHttpList);
  const { getSubErrorHttpList } = EIAffairsStore.effects;
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
            <div className="si-table-value">
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
      title: i18n.t('msp:error code'),
      dataIndex: 'httpCode',
      key: 'httpCode',
      width: 100,
    },
    {
      title: i18n.t('msp:number of occurrences'),
      dataIndex: 'count',
      key: 'count',
      width: 120,
    },
  ];

  const expandedRowRender = (record: MONITOR_EI.ITableData) => {
    const { name, httpCode, applicationId } = record;
    return (
      <TraceExpandTable
        viewLog={viewLog}
        applicationId={applicationId}
        fetchTraceContent={fetchTraceContent}
        recordKey={`${name}_${httpCode}`}
        dataSource={subErrorHttpList}
        emptyText={i18n.t('msp:No abnormal transaction data sampled yet.')}
      />
    );
  };

  const onRowExpand = onExpand(getSubErrorHttpList, query, (record: any) => {
    const { name, httpCode } = record;
    return { start, end, filter_source_service_name: name, filter_http_status_code: httpCode };
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
