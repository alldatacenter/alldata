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
import { Select, Table, Tooltip } from 'antd';
import moment from 'moment';
import HealthPoint, { statusMap } from 'project/common/components/health-point';
import { ColumnProps } from 'core/common/interface';
import { isEmpty } from 'lodash';
import { IInstance, IServiceIns } from '../../pages/overview/components/service-card';
import i18n from 'i18n';

import './instance-table.scss';

const { Option } = Select;
const insStatusMap = statusMap.task;

interface IProps {
  [prop: string]: any;
  instances: IServiceIns;
  isFetching?: boolean;
  withHeader?: boolean;
  opsCol?: ColumnProps<{ [prop: string]: any }>;
}

const InstanceTable = ({ instances, isFetching, withHeader = true, opsCol, ...tableProps }: IProps) => {
  const typeMap = {
    running: 'running',
    stopped: 'stopped',
  };
  const paginationMap = {
    limited: {
      size: 'small',
      pageSize: 8,
      // hideOnSinglePage: true,
    },
    unLimited: {
      size: 'small',
      pageSize: 8,
      // hideOnSinglePage: true,
    },
  };
  const [dataSource, setDataSource] = React.useState([] as IInstance[]);
  const [defaultValue, setDefaultValue] = React.useState(typeMap.running);
  const [pagingType, setPagingType] = React.useState('unLimited');

  React.useEffect(() => {
    const { runs = [], completedRuns = [] } = instances;
    if (!withHeader) {
      setDataSource(instances.runs.concat(instances.completedRuns));
    } else {
      if (!isEmpty(runs)) {
        setDataSource(runs);
      }
      if (isEmpty(runs) && !isEmpty(completedRuns)) {
        setDataSource(completedRuns);
        setDefaultValue(typeMap.stopped);
        setPagingType('limited');
      }
    }
  }, [instances, typeMap.stopped, withHeader]);

  const onFilterChange = (value: string) => {
    if (value === typeMap.running) {
      setDataSource(instances.runs);
      setPagingType('unLimited');
    } else {
      setDataSource(instances.completedRuns);
      setPagingType('limited');
    }
  };

  const columns: Array<ColumnProps<{ [prop: string]: any }>> = [
    {
      title: i18n.t('runtime:instance ip'),
      dataIndex: 'ipAddress',
      width: 120,
      render: (text: string, record: { [prop: string]: any }) => {
        const { status } = record;
        return withHeader ? (
          text
        ) : (
          <div>
            <span className="status-pointer">
              {status === 'Healthy' ? null : <HealthPoint type="task" status={status} />}
            </span>
            <span className="nowrap">{text}</span>
          </div>
        );
      },
    },
    {
      title: i18n.t('runtime:host address'),
      width: 120,
      dataIndex: 'host',
    },
    {
      title: i18n.t('status'),
      dataIndex: 'status',
      className: 'th-status',
      render: (text: string, record: any) => {
        const { message } = record;
        return (
          <span className="nowrap">
            {insStatusMap[text].text}
            {message ? <Tooltip title={message}> ({message})</Tooltip> : null}
          </span>
        );
      },
    },
    {
      title: i18n.t('create time'),
      width: 176,
      dataIndex: 'startedAt',
      className: 'th-time nowrap',
      render: (text: string) => moment(text).format('YYYY-MM-DD HH:mm:ss'),
    },
  ];

  opsCol && columns.push(opsCol);

  return (
    <div className="instance-table">
      <div className={`header ${withHeader ? '' : 'hidden'}`}>
        <span className="font-medium">{i18n.t('runtime:service details')}</span>
        <Select
          key={defaultValue}
          defaultValue={defaultValue}
          size="small"
          style={{ width: '30%' }}
          onChange={(value: string) => onFilterChange(value)}
        >
          <Option key={typeMap.running} value={typeMap.running}>
            {i18n.t('running')}
          </Option>
          <Option key={typeMap.stopped} value={typeMap.stopped}>
            {i18n.t('stopped')}
          </Option>
        </Select>
      </div>
      <Table
        key={pagingType}
        columns={columns}
        dataSource={dataSource}
        rowKey={(record, i) => {
          const { id, containerId } = record;
          return `${i}${id || containerId}`;
        }}
        loading={isFetching}
        pagination={paginationMap[pagingType]}
        scroll={{ x: 800 }}
        {...tableProps}
      />
    </div>
  );
};

export default InstanceTable;
