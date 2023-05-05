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
import { getDubboDetailChart, getDubboDetailTime } from '../services/dubbo';

interface IState {
  dubboDetailTime: {
    providerTime: string;
    consumerTime: string;
  };
}

const initState: IState = {
  dubboDetailTime: {
    providerTime: '',
    consumerTime: '',
  },
};

const dubbo = createStore({
  name: 'zkproxy-dubbo',
  state: initState,
  effects: {
    async getDubboDetailTime(
      { call, update, getParams },
      payload: { az: string; interfacename: string; tenantId: string },
    ) {
      const { env, projectId } = getParams();
      const dubboDetailTime = await call(getDubboDetailTime, { env, projectId, ...payload });
      update({ dubboDetailTime });
    },
    async getDubboDetailChart({ call, getParams }, payload) {
      const { env, projectId } = getParams();
      return call(getDubboDetailChart, { env, projectId, ...payload });
    },
  },
});

export default dubbo;
