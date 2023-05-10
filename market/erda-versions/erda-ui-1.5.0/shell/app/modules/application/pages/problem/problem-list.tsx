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
import { Input, Spin, Select, Table } from 'antd';
import { SwitchAutoScroll, CustomFilter } from 'common';
import { goTo, fromNow, insertWhen } from 'common/utils';
import { getProblemType, ProblemPriority } from 'application/pages/problem/problem-form';
import { useLoading } from 'core/stores/loading';
import problemStore from 'application/stores/problem';
import i18n from 'i18n';
import { ColumnProps } from 'core/common/interface';
import { IUseFilterProps } from 'app/interface/common';
import routeInfoStore from 'core/stores/route';
import './problem-list.scss';

interface IFilter {
  onSubmit: (value: Obj) => void;
  onReset: () => void;
}

const Filter = React.memo(({ onReset, onSubmit }: IFilter) => {
  const filterConfig = React.useMemo(() => {
    return [
      {
        type: Select,
        name: 'type',
        customProps: {
          placeholder: i18n.t('filter by {name}', { name: i18n.t('type') }),
          options: getProblemType().map(({ name, value }) => (
            <Option key={value} value={value}>
              {name}
            </Option>
          )),
        },
      },
      {
        type: Select,
        name: 'priority',
        customProps: {
          placeholder: i18n.t('filter by {name}', { name: i18n.t('dop:priority') }),
          options: ProblemPriority.map((priorityType: any) => (
            <Option key={priorityType.value} value={priorityType.value}>
              {priorityType.name}
            </Option>
          )),
        },
      },
      {
        type: Input,
        name: 'q',
        customProps: {
          placeholder: i18n.t('filter by {name}', { name: i18n.t('title') }),
        },
      },
    ];
  }, []);
  return <CustomFilter onReset={onReset} onSubmit={onSubmit} config={filterConfig} isConnectQuery />;
});

const { Option } = Select;
const updateKeyMap = {
  open: 'createdAt',
  closed: 'closedAt',
};

export const ProblemList = (props: Pick<IUseFilterProps, 'onSubmit' | 'onReset' | 'onPageChange'>) => {
  const [ticketList, paging] = problemStore.useStore((s) => [s.ticketList, s.paging]);
  const { onSubmit, onReset, onPageChange } = props;
  const [loading] = useLoading(problemStore, ['getTicketList']);
  const { ticketType } = routeInfoStore.useStore((s) => s.params);

  const handleSubmit = React.useCallback(onSubmit, []);
  const handleReset = React.useCallback(onReset, []);

  const columns: Array<ColumnProps<PROBLEM.Ticket>> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 80,
      render: (text) => `#${text}`,
    },
    {
      title: i18n.t('title'),
      dataIndex: 'title',
    },
    {
      title: i18n.t('type'),
      dataIndex: 'type',
      width: 120,
      render: (text) => {
        const type = getProblemType().find((t) => t.value === text);
        return type ? type.name : '-';
      },
    },
    {
      title: i18n.t('dop:priority'),
      dataIndex: 'priority',
      width: 96,
      render: (text) => {
        const priority: any = ProblemPriority.find((t: any) => t.value === text);
        return <span className={priority.color}>{priority.name}</span>;
      },
    },
    {
      title: i18n.t('creator'),
      dataIndex: 'creator',
      width: 120,
    },
    {
      title: i18n.t('create time'),
      dataIndex: 'createdAt',
      width: 176,
      render: (text) => fromNow(text),
    },
    ...insertWhen(ticketType !== 'open', [
      {
        title: i18n.t('close person'),
        dataIndex: 'content',
        width: 120,
        render: (_text, record) => {
          return record.status === 'closed' ? record.lastOperatorUser : '';
        },
      },
      {
        title: i18n.t('close time'),
        dataIndex: 'status',
        width: 176,
        render: (text, record) => (text === 'closed' ? fromNow(record[updateKeyMap[text]]) : ''),
      },
    ] as Array<ColumnProps<PROBLEM.Ticket>>),
  ];

  return (
    <React.Fragment>
      <SwitchAutoScroll toPageTop triggerBy={paging.pageNo} />
      <Filter onReset={handleReset} onSubmit={handleSubmit} />
      <Spin spinning={loading}>
        <Table
          tableKey="ticket_list"
          rowKey="id"
          dataSource={ticketList}
          columns={columns}
          pagination={{
            current: paging.pageNo,
            total: paging.total,
            pageSize: paging.pageSize,
            onChange: onPageChange,
          }}
          onRow={(record: PROBLEM.Ticket) => {
            return {
              onClick: () => {
                goTo(`./${record.id}`);
              },
            };
          }}
          scroll={{ x: 1100 }}
        />
      </Spin>
    </React.Fragment>
  );
};
