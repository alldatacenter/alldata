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
import i18n from 'i18n';
import {
  getCircuitBreaker,
  saveCircuitBreaker,
  getFaultInject,
  saveFaultInject,
  deleteFaultInject,
} from '../services/service-mesh';

interface IState {
  circuitBreaker: TOPOLOGY.ICircuitBreaker;
  faultInject: TOPOLOGY.IFaultInject;
}

const initState: IState = {
  circuitBreaker: {
    http: {} as TOPOLOGY.ICircuitBreakerHttp,
    dubbo: [] as TOPOLOGY.ICircuitBreakerDubbo[],
  },
  faultInject: {
    http: [] as TOPOLOGY.IFaultInjectHttp[],
    dubbo: [] as TOPOLOGY.IFaultInjectDubbo[],
  },
};
const serviceMesh = createStore({
  name: 'service-mesh',
  state: initState,
  effects: {
    async getCircuitBreaker({ call, getParams, update }, payload) {
      const { env, projectId, tenantGroup } = getParams();
      const circuitBreaker = await call(getCircuitBreaker, { env, projectId, tenantGroup, ...payload });
      update({ circuitBreaker });
    },
    async saveCircuitBreaker({ call, getParams }, payload) {
      const { env, projectId, tenantGroup } = getParams();
      const res = await call(
        saveCircuitBreaker,
        { env, projectId, tenantGroup, ...payload },
        { successMsg: i18n.t('saved successfully') },
      );
      return res;
    },
    async getFaultInject({ call, getParams, update }, payload) {
      const { env, projectId, tenantGroup } = getParams();
      const faultInject = await call(getFaultInject, { env, projectId, tenantGroup, ...payload });
      update({ faultInject });
    },
    async saveFaultInject({ call, getParams }, payload) {
      const { env, projectId, tenantGroup } = getParams();
      const res = await call(
        saveFaultInject,
        { env, projectId, tenantGroup, ...payload },
        { successMsg: i18n.t('saved successfully') },
      );
      return res;
    },
    async deleteFaultInject({ call, getParams }, payload) {
      const { env, projectId, tenantGroup } = getParams();
      const res = await call(
        deleteFaultInject,
        { env, projectId, tenantGroup, ...payload },
        { successMsg: i18n.t('deleted successfully') },
      );
      return res;
    },
  },
  reducers: {
    clearCircuitBreaker(state) {
      state.circuitBreaker = initState.circuitBreaker;
    },
    clearFaultInject(state) {
      state.faultInject = initState.faultInject;
    },
  },
});

export default serviceMesh;
