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

import { createFlatStore } from 'core/cube';
import { getDefaultPaging } from 'common/utils';
import breadcrumbStore from 'layout/stores/breadcrumb';
import routeInfoStore from 'core/stores/route';
import {
  getAlarmRecordList,
  getAlarmAttrs,
  getAlarmRecordDetail,
  getAlarmTimesChart,
  getAlarmRecordHistories,
} from '../services/alarm-record';

interface IState {
  recordList: ALARM_REPORT.RecordListItem[];
  recordListPaging: IPaging;
  alarmAttrs: { [k: string]: Array<{ key: string; display: string }> };
  recordDetail: ALARM_REPORT.RecordListItem;
  alarmTimesChart: any;
  recordHistories: ALARM_REPORT.AlarmHistory[];
}

const initState: IState = {
  recordList: [],
  recordListPaging: getDefaultPaging(),
  alarmAttrs: {} as any,
  recordDetail: {} as ALARM_REPORT.RecordListItem,
  alarmTimesChart: {} as any,
  recordHistories: [],
};

const alarmRecord = createFlatStore({
  name: 'mspAlarmRecord',
  state: initState,
  effects: {
    async getAlarmRecordList({ call, update }, query: Merge<ALARM_REPORT.RecordListQuery, IPagingReq>) {
      const { tenantGroup } = routeInfoStore.getState((s) => s.params);
      const { list: recordList } = await call(
        getAlarmRecordList,
        { ...query, tenantGroup },
        { paging: { key: 'recordListPaging' } },
      );
      update({ recordList });
    },
    async getAlarmAttrs({ call, update }) {
      const { tenantGroup } = routeInfoStore.getState((s) => s.params);
      const alarmAttrs = await call(getAlarmAttrs, tenantGroup);
      update({ alarmAttrs });
    },
    async getAlarmRecordDetail({ call, update }, groupId: string) {
      const { tenantGroup } = routeInfoStore.getState((s) => s.params);
      const recordDetail = await call(getAlarmRecordDetail, { groupId, tenantGroup });
      breadcrumbStore.reducers.setInfo('alarmRecordName', recordDetail.alertName);
      update({ recordDetail });
    },
    async getAlarmTimesChart({ call, update }, query: Omit<ALARM_REPORT.AlarmTimesQuery, 'filter_terminus_key'>) {
      const { terminusKey } = routeInfoStore.getState((s) => s.params);
      const alarmTimesChart = await call(getAlarmTimesChart, { ...query, filter_terminus_key: String(terminusKey) });
      update({ alarmTimesChart });
    },
    async getAlarmRecordHistories({ call, update }, payload: ALARM_REPORT.AlarmHistoriesQuery) {
      const { tenantGroup } = routeInfoStore.getState((s) => s.params);
      const recordHistories = await call(getAlarmRecordHistories, { tenantGroup, ...payload });
      update({ recordHistories });
    },
  },
});

export default alarmRecord;
