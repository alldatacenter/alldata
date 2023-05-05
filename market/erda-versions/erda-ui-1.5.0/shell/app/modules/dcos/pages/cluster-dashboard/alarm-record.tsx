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
import { Table, Select } from 'antd';
import { goTo } from 'common/utils';
import { map } from 'lodash';
import { Avatar, CustomFilter, MemberSelector } from 'common';
import { useMount } from 'react-use';
import moment from 'moment';
import { useLoading } from 'core/stores/loading';
import { ColumnProps } from 'core/common/interface';
import i18n from 'i18n';
import { useUserMap } from 'core/stores/userMap';
import alarmRecordStore from 'cmp/stores/alarm-record';
import { AlarmState } from 'cmp/common/alarm-state';

export default ({ clusters }: { clusters: any }) => {
  const [recordList, paging, alarmAttrs] = alarmRecordStore.useStore((s) => [
    s.recordList,
    s.recordListPaging,
    s.alarmAttrs,
  ]);
  const { getMachineAlarmRecordList, getAlarmAttrs } = alarmRecordStore;
  const userMap = useUserMap();

  const [loading] = useLoading(alarmRecordStore, ['getMachineAlarmRecordList']);

  useMount(() => {
    getAlarmAttrs();
    onSubmit();
  });

  const onSubmit = React.useCallback(
    (v?: Omit<ALARM_REPORT.RecordListQuery, 'alertType'>) => {
      getMachineAlarmRecordList({
        pageSize: paging.pageSize,
        pageNo: paging.pageNo,
        alertType: 'machine',
        clusters,
        ...v,
      });
    },
    [clusters, getMachineAlarmRecordList, paging.pageNo, paging.pageSize],
  );

  const columns: Array<ColumnProps<ALARM_REPORT.RecordListItem>> = [
    {
      title: i18n.t('title'),
      dataIndex: 'title',
    },
    {
      title: i18n.t('cmp:alarm status'),
      dataIndex: 'alertState',
      width: 150,
      render: (alertState) => <AlarmState state={alertState} />,
    },
    {
      title: i18n.t('alarm type'),
      dataIndex: 'alertType',
      width: 150,
    },
    {
      title: i18n.t('cmp:alarm time'),
      dataIndex: 'alertTime',
      width: 200,
      render: (alertTime) => moment(alertTime).format('YYYY-MM-DD HH:mm:ss'),
    },
  ];

  const filterConfig = React.useMemo(
    () => [
      {
        type: Select,
        name: 'alertState',
        customProps: {
          mode: 'multiple',
          placeholder: i18n.t('filter by {name}', { name: i18n.t('cmp:alarm status') }),
          options: map(alarmAttrs.alertState, ({ key, display }) => (
            <Select.Option key={key} value={key}>
              {display}
            </Select.Option>
          )),
        },
      },
    ],
    [alarmAttrs.alertState],
  );

  return (
    <div className="bg-white p-5">
      <CustomFilter onSubmit={onSubmit} config={filterConfig} isConnectQuery={false} />
      <Table
        rowKey="id"
        dataSource={recordList}
        loading={loading}
        columns={columns}
        pagination={{ ...paging }}
        onRow={(record: ALARM_REPORT.RecordListItem) => {
          return {
            onClick: () => {
              goTo(goTo.pages.orgAlarmRecordDetail, { id: record.groupId, jumpOut: true });
            },
          };
        }}
        scroll={{ x: '100%' }}
      />
    </div>
  );
};
