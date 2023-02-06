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

import { createStore } from 'core/cube';
import { getDefaultPaging } from 'common/utils';
import i18n from 'i18n';
import {
  addMachine,
  addCloudMachine,
  updaterMachineLabels,
  getClusterOperationHistory,
  getClusterOperationDetail,
  IOpHistoryQuery,
  getClusterOperationTypes,
} from '../services/machine';
import { getAlarmList, IMachineAlarmQuery } from 'dcos/services/alarm';
import orgStore from 'app/org-home/stores/org';

interface IState {
  operationList: ORG_MACHINE.IClusterOperateRecord[];
  operationPaging: IPaging;
  operationDetail: ORG_MACHINE.IClusterOperateRecord | null;
  cloudLogStatus: ORG_MACHINE.ICloudLogStatusResp | {};
  operationTypes: ORG_MACHINE.IClusterOperateType[];
  alarmList: ORG_ALARM.Ticket[];
  alarmListPaging: IPaging;
}

const initState: IState = {
  operationList: [],
  operationPaging: getDefaultPaging(),
  operationDetail: null,
  cloudLogStatus: {},
  operationTypes: [],
  alarmList: [],
  alarmListPaging: getDefaultPaging(),
};

const machine = createStore({
  name: 'machine',
  state: initState,
  effects: {
    async addMachine({ call }, payload: ORG_MACHINE.IAddMachineBody) {
      return call(addMachine, payload);
    },
    async addCloudMachine({ call }, payload: ORG_MACHINE.IAddCloudMachineBody) {
      return call(addCloudMachine, payload);
    },
    async updaterMachineLabels({ call }, payload: ORG_MACHINE.IMachineLabelBody) {
      await call(updaterMachineLabels, payload, {
        successMsg: i18n.t('cmp:set up successfully, wait for a while to take effect'),
      });
    },
    async getClusterOperationHistory({ call, update }, payload: IOpHistoryQuery) {
      const { list } = await call(getClusterOperationHistory, payload, { paging: { key: 'operationPaging' } });
      update({ operationList: list });
    },
    async getClusterOperationDetail({ call, update }, payload: string) {
      const operationDetail = await call(getClusterOperationDetail, payload);
      update({ operationDetail });
    },
    async getClusterOperationTypes({ call, update }) {
      const operationTypes = await call(getClusterOperationTypes);
      update({ operationTypes });
    },
    async getAlarmListByCluster(
      { call, update },
      payload: Pick<
        IMachineAlarmQuery,
        'orgID' | 'type' | 'endTime' | 'metricID' | 'pageNo' | 'pageSize' | 'startTime'
      >,
    ) {
      const { pageNo, pageSize = 10, type } = payload;
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { list: alarmList, total } = await call(
        getAlarmList,
        {
          ...payload,
          type: type || ['machine', 'dice_component', 'dice_addon', 'kubernetes'],
          targetType: 'org',
          targetID: orgId,
          pageNo,
          pageSize,
        },
        { paging: { key: 'alarmListPaging', listKey: 'tickets' } },
      );
      update({ alarmList });
      return {
        total,
        list: alarmList,
      };
    },
  },
  reducers: {
    clearAlarmListByCluster(state) {
      state.alarmList = [];
    },
  },
});

export default machine;
