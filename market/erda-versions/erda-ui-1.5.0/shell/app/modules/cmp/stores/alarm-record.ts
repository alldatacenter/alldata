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
import orgStore from 'app/org-home/stores/org';
import {
  getAlarmRecordList,
  getAlarmAttrs,
  getAlarmRecordDetail,
  getAlarmTimesChart,
  getAlarmRecordHistories,
  getMachineAlarmRecordList,
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
  name: 'orgAlarmRecord',
  state: initState,
  effects: {
    async getAlarmRecordList({ call, update }, query: Merge<ALARM_REPORT.RecordListQuery, IPagingReq>) {
      const { list: recordList } = await call(getAlarmRecordList, query, { paging: { key: 'recordListPaging' } });
      update({ recordList });
    },
    async getMachineAlarmRecordList({ call, update }, query: Merge<ALARM_REPORT.RecordListQuery, IPagingReq>) {
      const { list: recordList } = await call(getMachineAlarmRecordList, query, {
        paging: { key: 'recordListPaging' },
      });
      update({ recordList });
    },
    async getAlarmAttrs({ call, update }) {
      const alarmAttrs = await call(getAlarmAttrs);
      update({ alarmAttrs });
    },
    async getAlarmRecordDetail({ call, update }, groupId: string) {
      const recordDetail = await call(getAlarmRecordDetail, groupId);
      breadcrumbStore.reducers.setInfo('alarmRecordName', recordDetail.alertName);
      update({ recordDetail });
    },
    async getAlarmTimesChart({ call, update }, query: Omit<ALARM_REPORT.AlarmTimesQuery, 'filter_dice_org_id'>) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const alarmTimesChart = await call(getAlarmTimesChart, { ...query, filter_dice_org_id: String(orgId) });
      update({ alarmTimesChart });
    },
    async getAlarmRecordHistories({ call, update }, payload: ALARM_REPORT.AlarmHistoriesQuery) {
      const recordHistories = await call(getAlarmRecordHistories, payload);
      update({ recordHistories });
    },
  },
});

export default alarmRecord;
