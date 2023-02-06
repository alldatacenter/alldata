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

import { get, map } from 'lodash';
import { fetchContainerList, getServiceList, getRuntimeStatus, getMetrics, getRuntimeJson } from '../services/service';
import { createStore } from 'core/cube';

const initState: DCOS_SERVICES.IState = {
  containerList: [],
  serviceList: [],
  runtimeJson: null,
  runtimeStatus: {},
  serviceReqStatus: true,
  metrics: {
    cpu: {} as DCOS_SERVICES.metric,
    mem: {} as DCOS_SERVICES.metric,
  },
};

const dcosServiceStore = createStore({
  name: 'dcosService',
  state: initState,
  effects: {
    async getContainerList({ call, update }, payload: DCOS_SERVICES.path[]) {
      const [, project, app, runtime, service] = payload.map((p) => p.q);
      const query = {
        type: 'service',
      } as Omit<DCOS_SERVICES.QueryServices, 'status'>;
      const qMap = { project, app, runtimeID: runtime, serviceName: service };
      Object.keys(qMap).forEach((key) => {
        if (qMap[key] !== undefined) {
          query[key] = qMap[key];
        }
      });
      const containerList = await call(fetchContainerList, query);
      update({ containerList });
    },
    async getServiceList({ call, update }, payload: { paths: DCOS_SERVICES.path[]; environment: string }) {
      const serviceList = await call(getServiceList, payload);
      update({ serviceList, serviceReqStatus: true });
    },
    async getRuntimeStatus({ select, call, update }, payload: { runtimeIds: string }) {
      try {
        const { runtimeStatus } = select((state) => state);
        const newStatus = await call(getRuntimeStatus, payload);
        update({ runtimeStatus: { ...runtimeStatus, ...newStatus }, serviceReqStatus: true });
      } catch (e) {
        update({ serviceReqStatus: false });
        throw e;
      }
    },
    async getMetrics({ call }, payload: DCOS_SERVICES.QueryMetrics) {
      const { type, filter_workspace } = payload;
      const data = await call(getMetrics, payload);
      dcosServiceStore.reducers.getMetricsDataSuccess({ data, type, filter_workspace });
    },
    async getRuntimeJson({ call, update }, payload: { runtimeId: string }) {
      const runtimeJson = await call(getRuntimeJson, payload);
      update({ runtimeJson });
    },
  },
  reducers: {
    getMetricsDataSuccess(state, payload) {
      const { type, data } = payload;
      const { metrics } = state;
      const list = get(data, 'results[0].data');
      const reData: any[] = [];
      if (list) {
        const dataKeys = {
          cpu: 'last.usage_percent',
          mem: 'last.usage',
        };
        map(list, (l) => {
          const { tag, data: listData } = l[dataKeys[type]] || {};
          reData.push({ tag, data: listData });
        });
      }
      state.metrics = { ...metrics, [type]: { loading: false, data: reData } };
    },
    clearContainerList(state) {
      state.containerList = [];
    },
    clearRuntimeJson(state) {
      state.runtimeJson = null;
    },
    clearRuntimeStatus(state) {
      state.runtimeStatus = {};
    },
  },
});

export default dcosServiceStore;
