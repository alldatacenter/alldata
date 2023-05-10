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
import orgStore from 'app/org-home/stores/org';
import { getMachineStatus, deleteMachine } from '../services/machine';

const machine = createStore({
  name: 'org-machine',
  state: {},
  effects: {
    async getMachineStatus({ call }, payload: string[]) {
      const { name: orgName } = orgStore.getState((s) => s.currentOrg);
      const hostsStatus = await call(getMachineStatus, { org_name: orgName, hosts: payload });
      return hostsStatus;
    },
    async deleteMachine({ call }, payload: ORG_MACHINE.IDeleteMachineBody) {
      const res = await call(deleteMachine, payload);
      return res;
    },
  },
});

export default machine;
