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
import { floor } from 'lodash';
import { Tooltip, Progress, Table, Spin } from 'antd';
import { goTo, cutStr, secondsToTime, fromNow } from 'common/utils';
import { theme } from 'app/themes';
import i18n from 'i18n';
import './test-list.scss';
import applicationTestStore from 'application/stores/test';
import { useLoading } from 'core/stores/loading';
import { ColumnProps } from 'core/common/interface';

const getTestDuration = (duration: any) => {
  const seconds = floor(parseInt(duration, 10) / 10 ** 9, 3); // 时间为纳秒
  return duration !== 0 && seconds === 0
    ? `${duration / 1000000} ${i18n.t('dop:millisecond(s)')}`
    : secondsToTime(seconds, true);
};

const ExecuteResult = ({ totals }: { totals: { tests: number; statuses: TEST.Statuses } }) => {
  if (!totals) {
    return null;
  }
  const { failed, error, passed, skipped } = totals.statuses;
  const { tests } = totals;
  const passedPercent = tests ? floor(((passed + skipped) * 100) / tests, 2) : 0;
  const title = (
    <div>
      <div>
        {i18n.t('failed')}: {failed}
      </div>
      <div>
        {i18n.t('error')}: {error}
      </div>
      <div>
        {i18n.t('dop:pass')}: {passed}
      </div>
      <div>
        {i18n.t('dop:jump over')}: {skipped}
      </div>
    </div>
  );
  return (
    <Tooltip title={title} placement="right">
      <Progress
        percent={100}
        successPercent={passedPercent}
        strokeColor={theme.strokeColor}
        format={(_percent: number, successPercent: number) => `${Math.floor(successPercent)}%`}
      />
    </Tooltip>
  );
};

const columns: Array<ColumnProps<TEST.RunTestItem>> = [
  {
    title: i18n.t('default:name'),
    dataIndex: 'name',
    width: 176,
    render: (text) => <span>{cutStr(text, 30, { showTip: true })}</span>,
  },
  {
    title: i18n.t('dop:branch'),
    dataIndex: 'branch',
  },
  {
    title: i18n.t('default:creator'),
    dataIndex: 'operatorName',
    width: 120,
  },
  {
    title: i18n.t('default:create time'),
    dataIndex: 'createdAt',
    width: 176,
    render: (text) => fromNow(text),
  },
  {
    title: i18n.t('default:type'),
    dataIndex: 'type',
    width: 120,
  },
  {
    title: i18n.t('dop:time consuming'),
    dataIndex: ['totals', 'duration'],
    width: 160,
    render: (text) => getTestDuration(text),
  },
  {
    title: i18n.t('dop:execute result'),
    width: 200,
    dataIndex: ['totals', 'tests'],
    render: (_text, record) => <ExecuteResult totals={record.totals} />,
  },
];

const TestList = () => {
  const [list, testListPaging] = applicationTestStore.useStore((s) => [s.list, s.testListPaging]);
  const [isFetching] = useLoading(applicationTestStore, ['getTestList']);
  const { getTestTypes, getTestList } = applicationTestStore.effects;
  const { clearTestList } = applicationTestStore.reducers;
  React.useEffect(() => {
    getTestTypes();
    getTestList({ pageNo: 1 });
    return () => {
      clearTestList();
    };
  }, [clearTestList, getTestList, getTestTypes]);
  const handlePageChange = (pageNo: number) => {
    getTestList({ pageNo });
  };
  return (
    <div className="application-test">
      <Spin spinning={isFetching}>
        <Table
          rowKey="id"
          dataSource={list}
          columns={columns}
          onRow={({ id }: TEST.RunTestItem) => {
            return {
              onClick: () => {
                goTo(`./${id}`);
              },
            };
          }}
          pagination={{
            current: testListPaging.pageNo,
            ...testListPaging,
            onChange: handlePageChange,
          }}
          scroll={{ x: 1100 }}
        />
      </Spin>
    </div>
  );
};

export default TestList;
