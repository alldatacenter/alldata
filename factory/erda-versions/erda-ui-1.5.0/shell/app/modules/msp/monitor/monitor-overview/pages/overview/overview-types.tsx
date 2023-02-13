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
import { map } from 'lodash';
import moment from 'moment';
import { Table, Spin } from 'antd';
import MonitorPanel from 'monitor-overview/common/components/monitor-panel';

import './overview-types.scss';

interface ITableProps {
  [pro: string]: any;
  isFetching: boolean;
  className?: string;
  title?: string;
  data: ITableData[];
  columns: any;
  scroll?: object;
}

interface ITableData {
  [pro: string]: any;
  time?: string;
}

const TablePanel = ({ isFetching, className, title, data, columns, scroll, ...otherProps }: ITableProps) => {
  // 转换time
  const newData = data.map((item) => {
    return { ...item, time: moment(item.time).format('YYYY-MM-DD HH:mm:ss') };
  });
  return (
    <Spin spinning={isFetching}>
      <MonitorPanel className={`${className}`} title={title} {...otherProps}>
        <Table
          columns={columns}
          dataSource={newData}
          rowKey="id"
          className="service-status"
          pagination={false}
          scroll={scroll}
        />
      </MonitorPanel>
    </Spin>
  );
};

interface IGridProps {
  [pro: string]: any;
  isFetching: boolean;
  className?: string;
  title?: string;
  data: any;
}

const GridPanel = ({ isFetching, className = '', title, data, ...otherProps }: IGridProps) => {
  return (
    <Spin spinning={isFetching}>
      <MonitorPanel className={`monitor-grid-panel ${className}`} title={title} {...otherProps}>
        <div className="data-line">
          {map(data, (it, key) => {
            return (
              <div className="data-unit" key={key}>
                <span className="title">{it.name}</span>
                <div className="value">
                  <span className={it.color}>{it.value || '无'}</span>
                </div>
              </div>
            );
          })}
        </div>
      </MonitorPanel>
    </Spin>
  );
};

export { TablePanel, GridPanel };
