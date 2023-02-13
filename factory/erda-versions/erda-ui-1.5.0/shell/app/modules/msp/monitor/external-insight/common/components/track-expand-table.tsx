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
import { Table } from 'antd';
import moment from 'moment';
import { map, get, pick } from 'lodash';
import i18n from 'i18n';

interface IProps {
  recordKey: string;
  dataSource: Obj<MONITOR_EI.ISubTableData[]>;
  columns?: any[];
  applicationId: string;
  emptyText: string;
  viewLog: (params: any) => void;
  fetchTraceContent: (params: any) => void;
}

export const TraceExpandTable = ({
  recordKey,
  dataSource,
  columns,
  viewLog,
  fetchTraceContent,
  emptyText,
  applicationId,
}: IProps) => {
  let subDataSource = get(dataSource, recordKey);

  subDataSource = map(subDataSource, (item) => {
    const { '@timestamp': time, tags, fields } = item;
    const { elapsed_max: timeCost } = fields;
    const { request_id: requestId, trace_sampled: traced } = tags || {};
    return { time, requestId, traced: traced === 'true', timeCost };
  });

  const openLog = (record: any) => {
    const { requestId } = record;
    viewLog({ requestId, applicationId });
  };

  const openQuerier = (record: any) => {
    const { requestId } = record;
    fetchTraceContent({ requestId });
  };

  const subColumns = [
    {
      title: i18n.t('request ID'),
      dataIndex: 'requestId',
      key: 'requestId',
      ellipsis: true,
      width: 100,
    },
    {
      title: i18n.t('time'),
      dataIndex: 'time',
      key: 'time',
      render: (value: string) => moment(value).format('YYYY-MM-DD HH:mm:ss'),
      // width: 100,
    },
    {
      title: i18n.t('msp:time-consuming(ms)'),
      dataIndex: 'timeCost',
      key: 'timeCost',
      render: (value: string) => Number(value || '0') / 1000000,
      // width: 100,
    },
    {
      title: i18n.t('operation'),
      // width: 100,
      render: (record: any) => {
        const { requestId, traced } = record;
        return (
          <div className="table-operations">
            {requestId ? (
              <span className="table-operations-btn" onClick={() => openLog(record)}>
                {i18n.t('msp:view log')}
              </span>
            ) : null}
            {traced ? (
              <span className="table-operations-btn" onClick={() => openQuerier(record)}>
                {i18n.t('view link')}
              </span>
            ) : null}
          </div>
        );
      },
      // fixed: 'right',
    },
  ];

  return (
    <Table
      columns={columns || subColumns}
      dataSource={subDataSource}
      pagination={false}
      locale={{ emptyText }}
      scroll={{ x: '100%' }}
    />
  );
};

export const onExpand = (getSubTableList: Function, query: any, getParams: any) => (isExpend: boolean, record: any) => {
  if (isExpend) {
    const keyParams = getParams(record);
    const queryParams = pick(query, ['filter_source_terminus_key', 'filter_host', 'limit']);
    getSubTableList({ ...queryParams, ...keyParams });
  }
};
