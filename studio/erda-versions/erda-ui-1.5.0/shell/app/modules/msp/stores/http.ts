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
import { getServiceList, toggleIPStatus } from '../services/http';

export interface IHttpServiceDto {
  address: string;
  online: boolean;
}

export interface IService {
  serviceName: string;
  serviceDomain: string;
  httpServiceDto: IHttpServiceDto[];
}

interface IState {
  serviceList: IService[];
}

const initState: IState = {
  serviceList: [],
};

const httpStore = createStore({
  name: 'zkproxy-http',
  state: initState,
  effects: {
    async getServiceList({ call, update, getParams }, payload: { az: string; appId: number; nacosId: string }) {
      const { env, projectId, tenantGroup } = getParams();
      const { serviceList } = await call(getServiceList, { env, projectId, tenantGroup, ...payload });
      update({ serviceList });
    },
    async toggleIPStatus(
      { call, getParams },
      { az, appId, nacosId, body }: { az: string; appId: number; nacosId: string; body: IStatusPayload },
    ) {
      const { env, projectId, tenantGroup } = getParams();
      await call(toggleIPStatus, { env, projectId, nacosId, az, appId, body, tenantGroup });
      await httpStore.effects.getServiceList({ az, appId, nacosId });
    },
  },
  reducers: {
    clearServiceList(state) {
      state.serviceList = [];
    },
  },
});

export default httpStore;
