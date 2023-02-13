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

import { map } from 'lodash';
import React from 'react';
import i18n from 'i18n';
import moment from 'moment';
import { ColumnProps } from 'core/common/interface';
import { Badge, Table } from 'antd';

import { Icon as CustomIcon } from 'common';
import { useLoading } from 'core/stores/loading';
import testPlanStore from 'project/stores/test-plan';
import { ciStatusMap } from 'application/pages/build-detail/config';
import './build-history.scss';

interface IProps {
  activeItem: any;
  onClickRow: (record: object) => any;
}

type TableRecord = Merge<TEST_PLAN.Pipeline, { runIndex: string }>;

export const BuildHistory = ({ activeItem, onClickRow }: IProps) => {
  const { getExecuteRecords } = testPlanStore.effects;
  const [isFetchingRecords] = useLoading(testPlanStore, ['getExecuteRecords']);
  const [recordPaging, executeRecords] = testPlanStore.useStore((s) => [s.executeRecordsPaging, s.executeRecords]);

  const columns: Array<ColumnProps<TableRecord>> = [
    {
      title: i18n.t('version'),
      dataIndex: 'runIndex',
      width: 80,
      align: 'center',
      render: (runIndex: any, record: TableRecord) => (
        <span className="run-index">
          {record.triggerMode === 'cron' && <CustomIcon type="clock" />}
          {runIndex}
        </span>
      ),
    },
    {
      title: 'ID',
      dataIndex: 'id',
      align: 'center',
    },
    {
      title: i18n.t('status'),
      dataIndex: 'status',
      width: 110,
      render: (status: string) => (
        <span>
          <span className="nowrap">{ciStatusMap[status].text}</span>
          <Badge className="ml-1" status={ciStatusMap[status].status} />
        </span>
      ),
    },
    {
      title: i18n.t('trigger time'),
      dataIndex: 'timeCreated',
      width: 180,
      render: (timeCreated: number) => <span>{moment(new Date(timeCreated)).format('YYYY-MM-DD HH:mm:ss')}</span>,
    },
  ];

  const { total, pageNo, pageSize } = recordPaging;
  const startIndex = total - pageSize * (pageNo - 1);
  const dataSource: TableRecord[] = map(executeRecords, (item, index) => {
    return { ...item, runIndex: '#'.concat(String(startIndex - index)) };
  });

  return (
    <div className="build-history-wp">
      <Table
        rowKey="runIndex"
        className="build-history-list"
        columns={columns}
        loading={isFetchingRecords}
        dataSource={dataSource}
        pagination={{
          pageSize,
          total,
          current: pageNo,
          onChange: (pNo: number) => getExecuteRecords({ pageNo: pNo }),
        }}
        onRow={(record: TableRecord) => ({
          onClick: () => {
            onClickRow(record);
          },
          className: activeItem.id === record.id ? 'active-tr' : '',
        })}
        scroll={{ x: '100%' }}
      />
    </div>
  );
};
