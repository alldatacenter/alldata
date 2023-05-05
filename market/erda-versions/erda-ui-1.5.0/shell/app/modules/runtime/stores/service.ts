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
import { getServiceInstances, updateServicesConfig, getServicePods, killServicePod } from '../services/service';
import { getLS } from 'common/utils';
import { isEmpty } from 'lodash';
import runtimeStore from './runtime';
import i18n from 'i18n';

interface ServiceIns {
  runs: RUNTIME_SERVICE.Instance[];
  completedRuns: RUNTIME_SERVICE.Instance[];
}

interface IState {
  serviceInsMap: {
    [k: string]: ServiceIns;
  };
  taskMap: ServiceIns;
  podListMap: {
    [k: string]: RUNTIME_SERVICE.Pod[];
  };
}

const initState: IState = {
  serviceInsMap: {},
  taskMap: {
    runs: [],
    completedRuns: [],
  },
  podListMap: {},
};

const runtimeService = createFlatStore({
  name: 'runtimeService',
  state: initState,
  subscriptions: ({ registerWSHandler }: IStoreSubs) => {
    registerWSHandler('R_RUNTIME_SERVICE_STATUS_CHANGED', ({ payload }) => {
      const runtimeDetail = runtimeStore.getState((s) => s.runtimeDetail);
      if (payload.runtimeId === runtimeDetail.id) {
        runtimeService.getServiceInstances(payload.serviceName);
      }
    });
  },
  effects: {
    async getServiceInstances({ select, call, update, getParams }, serviceName: string) {
      const serviceInsMap = select((s) => s.serviceInsMap);
      const { runtimeId: runtimeID } = getParams();
      const runningServiceIns = await call(getServiceInstances, {
        status: 'running',
        runtimeID,
        serviceName,
        // ...extraPayload,
      });
      const completedServiceIns = await call(getServiceInstances, {
        status: 'stopped',
        runtimeID,
        serviceName,
        // ...extraPayload,
      });
      const serviceIns = {
        runs: runningServiceIns,
        completedRuns: completedServiceIns,
      };

      if (serviceIns.runs) {
        serviceIns.runs = serviceIns.runs.map((ins) => ({ ...ins, isRunning: true }));
      }
      update({ serviceInsMap: { ...serviceInsMap, [serviceName]: serviceIns }, taskMap: { ...serviceIns } });
      return serviceIns;
    },
    async updateServicesConfig({ call, getParams }, data: RUNTIME_SERVICE.PreOverlay) {
      const { appId } = getParams();
      const {
        extra: { workspace },
        name: runtimeName,
      } = runtimeStore.getState((s) => s.runtimeDetail);

      if (isEmpty(data)) return;
      await call(
        updateServicesConfig,
        {
          data,
          query: {
            applicationId: appId,
            workspace,
            runtimeName,
          },
        },
        { successMsg: i18n.t('updated successfully') },
      );
    },
    async getServicePods({ call, update, select }, payload: RUNTIME_SERVICE.PodQuery) {
      const podList = await call(getServicePods, payload);
      const podListMap = select((s) => s.podListMap);
      update({ podListMap: { ...podListMap, [payload.service]: podList } });
    },
    async killServicePod({ call }, payload: RUNTIME_SERVICE.KillPodBody) {
      await call(killServicePod, payload);
    },
  },
  reducers: {
    clearServiceInsMap(state) {
      return { ...state, serviceInsMap: {} };
    },
  },
});

export default runtimeService;
