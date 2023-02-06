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
import { Select } from 'antd';
import { goTo } from 'common/utils';
import Table from 'common/components/table';
import { map } from 'lodash';
import { CustomFilter } from 'common';
import { useFilter } from 'common/use-hooks';
import { useMount } from 'react-use';
import moment from 'moment';
import { useLoading } from 'core/stores/loading';
import { ColumnProps } from 'core/common/interface';
import i18n from 'i18n';
import { AlarmState } from 'cmp/common/alarm-state';
import { useUserMap } from 'core/stores/userMap';
import routeInfoStore from 'core/stores/route';
import orgAlarmRecordStore from 'cmp/stores/alarm-record';
import mspAlarmRecordStore from 'msp/alarm-manage/alarm-record/stores/alarm-record';

export enum AlarmRecordScope {
  ORG = 'org',
  MICRO_SERVICE = 'micro_service',
}

const storeMap = {
  [AlarmRecordScope.ORG]: orgAlarmRecordStore,
  [AlarmRecordScope.MICRO_SERVICE]: mspAlarmRecordStore,
};

const memberScopeMap = {
  [AlarmRecordScope.ORG]: 'org',
  [AlarmRecordScope.MICRO_SERVICE]: 'project',
};

const urlMap = {
  [AlarmRecordScope.ORG]: goTo.pages.orgAlarmRecordDetail,
  [AlarmRecordScope.MICRO_SERVICE]: goTo.pages.microServiceAlarmRecordDetail,
};

const AlarmRecord = ({ scope }: { scope: string }) => {
  const alarmRecordStore = storeMap[scope];
  const [recordList, paging, alarmAttrs] = alarmRecordStore.useStore((s) => [
    s.recordList,
    s.recordListPaging,
    s.alarmAttrs,
  ]);
  const { getAlarmRecordList, getAlarmAttrs } = alarmRecordStore;
  const userMap = useUserMap();
  const params = routeInfoStore.useStore((s) => s.params);

  const [loading] = useLoading(alarmRecordStore, ['getAlarmRecordList']);
  useMount(() => {
    getAlarmAttrs();
  });

  const { onSubmit, onReset, autoPagination } = useFilter({
    getData: getAlarmRecordList,
    debounceGap: 500,
  });

  const columns: Array<ColumnProps<ALARM_REPORT.RecordListItem>> = [
    {
      title: i18n.t('title'),
      dataIndex: 'title',
    },
    {
      title: i18n.t('cmp:alarm status'),
      dataIndex: 'alertState',
      render: (alertState: string) => <AlarmState state={alertState} />,
    },
    {
      title: i18n.t('alarm type'),
      dataIndex: 'alertType',
    },
    {
      title: i18n.t('cmp:alarm time'),
      dataIndex: 'alertTime',
      render: (alertTime: number) => moment(alertTime).format('YYYY-MM-DD HH:mm:ss'),
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
      {
        type: Select,
        name: 'alertType',
        customProps: {
          mode: 'multiple',
          placeholder: i18n.t('dop:filter by alarm type'),
          options: map(alarmAttrs.alertType, ({ key, display }) => (
            <Select.Option key={key} value={key}>
              {display}
            </Select.Option>
          )),
        },
      },
    ],
    [alarmAttrs.alertState, alarmAttrs.alertType],
  );

  return (
    <>
      <Table
        rowKey={(r) => r.groupId}
        dataSource={recordList}
        loading={loading}
        columns={columns}
        onChange={() => getAlarmRecordList(...paging, ...alarmAttrs)}
        pagination={autoPagination(paging)}
        onRow={(record: ALARM_REPORT.RecordListItem) => {
          return {
            onClick: () => {
              goTo(urlMap[scope], { ...params, id: record.groupId });
            },
          };
        }}
        slot={<CustomFilter onReset={onReset} onSubmit={onSubmit} config={filterConfig} isConnectQuery />}
      />
    </>
  );
};

export default AlarmRecord;
