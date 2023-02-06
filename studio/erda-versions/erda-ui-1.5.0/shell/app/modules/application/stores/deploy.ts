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
import { getDefaultPaging, removeLS } from 'common/utils';
import {
  getRunTimes,
  getExtensions,
  getActionGroup,
  getActionConfigs,
  addRuntimeByRelease,
  getReleaseByWorkspace,
  getLaunchedDeployList,
  getApprovalList,
  updateApproval,
} from '../services/deploy';
import { redeployRuntime, deleteRuntime } from 'runtime/services/runtime';
import { isEmpty, find, findIndex, fill } from 'lodash';

import appStore from 'application/stores/application';

interface IState {
  runtimes: DEPLOY.Runtime[];
  actions: DEPLOY.ExtensionAction[];
  groupActions: Obj<DEPLOY.IGroupExtensionActionObj[]>;
  actionConfigs: DEPLOY.ActionConfig[];
  launchedDeployList: DEPLOY.IDeploy[];
  launchedDeployPaging: IPaging;
  approvalList: DEPLOY.IDeploy[];
  approvalPaging: IPaging;
}

const initState: IState = {
  runtimes: [],
  actions: [],
  groupActions: {},
  actionConfigs: [],
  launchedDeployList: [],
  launchedDeployPaging: getDefaultPaging(),
  approvalList: [],
  approvalPaging: getDefaultPaging(),
};

const deploy = createStore({
  name: 'appDeploy',
  state: initState,
  subscriptions({ listenRoute, registerWSHandler }: IStoreSubs) {
    listenRoute(({ isIn, params }: IRouteInfo) => {
      const { appId } = params;
      // 进入流水线和部署中心 请求应用详情
      if (isIn('deploy') || isIn('pipeline')) {
        appStore.effects.getAppBlockNetworkStatus(appId);
      }
    });

    registerWSHandler('R_DEPLOY_STATUS_UPDATE', ({ payload }) => {
      deploy.reducers.updateRuntimeDeployStatus(payload);
      deploy.reducers.updateRuntimeStatus({ status: payload.status, runtimeId: payload.runtimeId });
    });

    registerWSHandler('R_RUNTIME_STATUS_CHANGED', ({ payload }) => {
      deploy.reducers.updateRuntimeStatus(payload);
    });

    registerWSHandler('R_RUNTIME_DELETING', ({ payload }) => {
      deploy.reducers.deleteDeployRuntime(payload);
    });

    registerWSHandler('R_RUNTIME_DELETED', ({ payload }) => {
      deploy.reducers.deleteDeployRuntime(payload);
    });
  },
  effects: {
    async getRunTimes({ call, getParams, update, select }) {
      const { appId } = getParams();
      const oldRuntimes = select((s) => s.runtimes);
      if (appId) {
        const runtimes = await call(getRunTimes, appId);
        if (runtimes !== null) {
          update({ runtimes });
        }
        return runtimes;
      }
      return oldRuntimes;
    },
    async getActions({ call, update }) {
      const actions = (await call(getExtensions)) as DEPLOY.ExtensionAction[];
      if (!isEmpty(actions)) {
        update({ actions });
      }
      return actions;
    },
    async getGroupActions({ call, update }, payload: { labels?: string } = {}) {
      const groupActions = await call(getActionGroup, payload);
      if (!isEmpty(groupActions)) {
        update({ groupActions });
      }
      return groupActions;
    },
    async getActionConfigs({ call, update }, payload: any) {
      const actionConfigs = await call(getActionConfigs, payload);
      if (!isEmpty(actionConfigs)) {
        update({ actionConfigs });
      }
      return actionConfigs;
    },
    async redeployRuntime({ call, getParams }, id?: string) {
      const { runtimeId } = getParams();
      await call(redeployRuntime, id || runtimeId, { successMsg: i18n.t('runtime:start redeploying the runtime') });
      deploy.effects.getRunTimes();
      deploy.reducers.clearServiceConfig(runtimeId);
    },
    async deleteRuntime({ call, getParams }, id?: string) {
      const { runtimeId } = getParams();
      await call(deleteRuntime, id || runtimeId, { successMsg: i18n.t('runtime:start deleting runtime') });
      deploy.effects.getRunTimes();
    },
    async addRuntimeByRelease({ call, getParams }, payload: DEPLOY.AddByRelease) {
      const { projectId, appId } = getParams();
      await call(
        addRuntimeByRelease,
        { ...payload, projectId: +projectId, applicationId: +appId },
        { successMsg: i18n.t('added successfully') },
      );
      deploy.effects.getRunTimes();
    },
    async getReleaseByWorkspace({ call, getParams }, payload: { workspace: string }) {
      const { appId } = getParams();
      const res = await call(getReleaseByWorkspace, { appId, ...payload });
      return res;
    },
    async getLaunchedDeployList({ call, update }, payload: IPagingReq) {
      const res = await call(getLaunchedDeployList, { ...payload }, { paging: { key: 'launchedDeployPaging' } });
      update({ launchedDeployList: res.list });
      return res;
    },
    async getApprovalList({ call, update }, payload: IPagingReq) {
      const res = await call(getApprovalList, { ...payload }, { paging: { key: 'approvalPaging' } });
      update({ approvalList: res.list });
      return res;
    },
    async updateApproval({ call }, payload: DEPLOY.IUpdateApproveBody) {
      const res = await call(updateApproval, payload, { successMsg: i18n.t('operated successfully') });
      return res;
    },
  },
  reducers: {
    deletingDeployRuntime(state, payload) {
      const newList = state.runtimes.map((item) => {
        if (item.id.toString() === payload.runtimeId.toString()) {
          return { ...item, deleteStatus: 'DELETING' };
        }
        return item;
      });
      state.runtimes = newList;
    },
    deleteDeployRuntime(state, payload) {
      const newList = state.runtimes.filter((item) => item.id.toString() !== payload.runtimeId.toString());
      state.runtimes = newList;
    },
    clearDeploy() {
      return { ...initState };
    },
    updateRuntimeDeployStatus(state, payload) {
      const { runtimeId, status } = payload;
      const runtime = find(state.runtimes, { id: runtimeId });
      const index = findIndex(state.runtimes, { id: runtimeId });
      if (runtime) {
        fill(state.runtimes, { ...runtime, deployStatus: status }, index, index + 1);
      }
    },
    updateRuntimeStatus(state, payload) {
      const { runtimeId, status, error } = payload;
      const runtime = find(state.runtimes, { id: runtimeId });
      const index = findIndex(state.runtimes, { id: runtimeId });
      if (runtime) {
        fill(state.runtimes, { ...runtime, status, error }, index, index + 1);
      }
    },
    clearServiceConfig(_, runtimeId) {
      removeLS(`${runtimeId}`);
      removeLS(`${runtimeId}_domain`);
    },
    clearDeployList(state, type) {
      const keyMap = {
        approval: {
          listKey: 'approvalList',
          pagingKey: 'approvalPaging',
        },
        launched: {
          listKey: 'launchedDeployList',
          pagingKey: 'launchedDeployPaging',
        },
      };
      if (keyMap[type]) {
        const { listKey, pagingKey } = keyMap[type];
        state[listKey] = [];
        state[pagingKey] = getDefaultPaging();
      }
    },
    clearLaunchedDeployList(state) {
      state.launchedDeployList = [];
      state.launchedDeployPaging = getDefaultPaging();
    },
  },
});

export default deploy;
