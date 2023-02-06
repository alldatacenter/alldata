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
import * as GatewayIngressCommonService from '../services/common';
import orgStore from 'app/org-home/stores/org';

interface IState {
  domainList: string[];
  chosenDomain: string | undefined;
}

const initState: IState = {
  domainList: [],
  chosenDomain: undefined,
};

const common = createStore({
  name: 'gatewayIngressCommon',
  state: initState,
  effects: {
    async getDomainList({ call, update, getParams }) {
      const { projectId, env } = getParams();
      const { id: orgId } = orgStore.getState((s) => s.currentOrg);
      const query = { projectId, env, orgId };
      const domainList = await call(GatewayIngressCommonService.getDomainList, query);
      update({ domainList });
    },
  },
  reducers: {
    changeChosenDomain(state, payload: { chosenDomain: string }) {
      state.chosenDomain = payload.chosenDomain;
    },
  },
});

export default common;
