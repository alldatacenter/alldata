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
import orgStore from 'app/org-home/stores/org';
import i18n from 'i18n';
import { getGroupInfos, offlineMachine } from '../services/machine-manage';

interface IState {
  groupInfos: MACHINE_MANAGE.IGroupInfo[];
}
const initState: IState = {
  groupInfos: [],
};

const machineManageStore = createFlatStore({
  name: 'machineManage',
  state: initState,
  effects: {
    async getGroupInfos({ call, update }, payload: Omit<MACHINE_MANAGE.IGroupInfoQuery, 'orgName'>) {
      const { name: orgName } = orgStore.getState((s) => s.currentOrg);
      const data = await call(getGroupInfos, { orgName, ...payload });
      const { groups: groupInfos } = data || {};

      update({
        groupInfos: groupInfos || [],
      });
      return groupInfos;
    },
    async offlineMachine({ call }, payload: MACHINE_MANAGE.IOfflineMachine) {
      await call(offlineMachine, payload, {
        successMsg: i18n.t('ecp:it is getting offline and it will take a effect later'),
      });
    },
  },
  reducers: {
    clearGroupInfos(state) {
      state.groupInfos = [];
    },
  },
});

export default machineManageStore;
