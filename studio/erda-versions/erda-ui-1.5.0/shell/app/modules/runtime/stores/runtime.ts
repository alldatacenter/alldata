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

import runtimeDomainStore from 'runtime/stores/domain';
import breadcrumbStore from 'app/layout/stores/breadcrumb';
import { goTo, removeLS, getDefaultPaging } from 'common/utils';
import { createFlatStore } from 'core/cube';
import i18n from 'i18n';
import {
  getRuntimeDetail,
  rollbackRuntime,
  redeployRuntime,
  deleteRuntime,
  getRuntimeAddons,
  getDeploymentList,
  cancelDeployment,
} from '../services/runtime';
import userStore from 'app/user/stores';
import moment from 'moment';

const defaultRuntimeDetail = {
  services: {},
  extra: {},
};

interface State {
  runtimeDetail: RUNTIME.Detail;
  addons: ADDON.Instance[];
  deploymentList: RUNTIME.DeployRecord[];
  deploymentRecords: RUNTIME.DeployRecord[];
  deploymentListPaging: IPaging;
  deploymentRecordsPaging: IPaging;
  showRedirect: boolean;
  cancelDeploying: boolean;
  hasChange: boolean;
  deploymentListQuery: RUNTIME.DeployListQuery;
}

const initState: State = {
  runtimeDetail: defaultRuntimeDetail as RUNTIME.Detail,
  addons: [],
  deploymentList: [],
  deploymentRecords: [],
  deploymentListPaging: getDefaultPaging(),
  deploymentRecordsPaging: getDefaultPaging(),
  showRedirect: false,
  cancelDeploying: false,
  hasChange: false,
  deploymentListQuery: {} as RUNTIME.DeployListQuery,
};

const runtime = createFlatStore({
  name: 'runtime',
  state: initState,
  subscriptions({ listenRoute, registerWSHandler }: IStoreSubs) {
    listenRoute(({ isEntering, isLeaving, params }) => {
      if (isEntering('runtime')) {
        const { runtimeId } = params;
        runtime.getRuntimeDetail({ runtimeId });
      } else if (isLeaving('runtime')) {
        runtime.clearRuntimeDetail();
      }
    });

    registerWSHandler('R_DEPLOY_STATUS_UPDATE', ({ payload }) => {
      const [runtimeDetail, deploymentListQuery] = runtime.getState((s) => [s.runtimeDetail, s.deploymentListQuery]);
      if (payload.runtimeId === runtimeDetail.id) {
        runtime.updateRuntimeDeployStatus({ status: payload.status });
        runtime.getRuntimeDetail({ runtimeId: String(runtimeDetail.id), socketData: payload, fromSocket: true });
        if (payload.phase === 'INIT' && payload.status === 'DEPLOYING') {
          runtime.getDeploymentList({ pageNo: 1, pageSize: 10 });
        }
        if (payload.phase === 'COMPLETED' && payload.status === 'OK') {
          runtimeDomainStore.getDomains({
            domainType: ['k8s', 'edas'].includes(runtimeDetail.clusterType) ? 'k8s-domains' : 'domains',
          });
          runtime.getDeploymentList({ ...deploymentListQuery });
        }
      }
    });

    registerWSHandler('R_RUNTIME_STATUS_CHANGED', ({ payload }) => {
      const runtimeDetail = runtime.getState((s) => s.runtimeDetail);
      if (payload.runtimeId === runtimeDetail.id) {
        runtime.updateRuntimeStatus(payload);
      }
    });

    registerWSHandler('R_RUNTIME_DELETING', ({ payload }) => {
      const runtimeDetail = runtime.getState((s) => s.runtimeDetail);
      if (payload.runtimeId === runtimeDetail.id) {
        runtime.updateRuntimeDeleteStatus();
      }
    });

    registerWSHandler('R_RUNTIME_DELETED', ({ payload }) => {
      const runtimeDetail = runtime.getState((s) => s.runtimeDetail);
      if (payload.runtimeId === runtimeDetail.id) {
        runtime.toggleRedirect(true);
      }
    });

    registerWSHandler('R_RUNTIME_SERVICE_STATUS_CHANGED', ({ payload }) => {
      const runtimeDetail = runtime.getState((s) => s.runtimeDetail);
      if (payload.runtimeId === runtimeDetail.id) {
        runtime.updateRuntimeServiceStatus(payload);
      }
    });
  },
  effects: {
    async getRuntimeDetail(
      { call, select, update, getParams },
      {
        runtimeId: r_id,
        socketData,
        fromSocket = false,
        forceUpdate = false,
      }: { runtimeId?: string; socketData?: any; fromSocket?: boolean; forceUpdate?: boolean },
    ) {
      let runtimeDetail = await select((state) => state.runtimeDetail);
      let data = r_id;
      const { appId, runtimeId } = getParams();

      // 部署成功时，再次拉取 runtime 信息
      // if (socketData && socketData.status !== 'OK') {
      //   return runtimeDetail;
      // } else if (socketData && socketData.status === 'OK') {
      //   data = runtimeId;
      // }

      if (socketData && socketData.status === 'OK') {
        data = runtimeId;
      }

      const isCurrentRuntime = runtimeDetail.extra.applicationId === +appId && runtimeDetail.id === +r_id;
      // 每次url变化时，如果是当前runtime则不再请求数据，不是则请求url中的runtimeId的数据
      if (!forceUpdate && !fromSocket && isCurrentRuntime) {
        return runtimeDetail;
      }

      runtimeDetail = await call(getRuntimeDetail, { runtimeId: String(data) });
      update({ runtimeDetail });
      breadcrumbStore.reducers.setInfo('runtimeName', runtimeDetail.name);
      const domainType = ['k8s', 'edas'].includes(runtimeDetail.clusterType) ? 'k8s-domains' : 'domains';
      const {
        extra: { workspace },
        projectID,
      } = runtimeDetail;
      await runtime.getRuntimeAddons({ projectId: projectID, workspace });
      runtimeDomainStore.getDomains({ domainType });
      return runtimeDetail;
    },
    async getRuntimeAddons({ call, update, getParams }, payload: Omit<RUNTIME.AddonQuery, 'value'>) {
      const { runtimeId } = getParams();
      const addons = await call(getRuntimeAddons, { ...payload, value: runtimeId });
      update({ addons });
    },
    async cancelDeployment({ call, update, getParams }, { force, runtimeId }: { force: boolean; runtimeId?: string }) {
      const { runtimeId: rId } = getParams();
      const { id } = userStore.getState((s) => s.loginUser);
      await call(cancelDeployment, { runtimeId: runtimeId || rId, force, operator: id });
      !runtimeId && update({ cancelDeploying: true });
    },
    // 回滚记录
    async getRollbackList({ call, update, getParams }, payload: Omit<RUNTIME.DeployListQuery, 'runtimeId'>) {
      const { runtimeId } = getParams();
      const { list, total } = await call(
        getDeploymentList,
        { runtimeId, ...payload },
        { paging: { key: 'deploymentRecordsPaging' } },
      );
      update({ deploymentRecords: list });
      return { total, list };
    },
    // 部署单列表，和回滚记录是相同数据，分开不会造成分页时的数据混淆
    async getDeploymentList({ call, update, getParams }, payload: Omit<RUNTIME.DeployListQuery, 'runtimeId'>) {
      const { runtimeId } = getParams();
      update({ deploymentListQuery: { runtimeId, ...payload } });
      const { list, total } = await call(getDeploymentList, { runtimeId, ...payload });
      if (payload.pageNo === 1) {
        update({ deploymentList: list });
      } else {
        runtime.loadMoreDeploymentList(list);
      }
      return { total, list };
    },
    async rollbackRuntime({ call, getParams }, deploymentId: number) {
      const { runtimeId } = getParams();
      await call(
        rollbackRuntime,
        { runtimeId, deploymentId },
        { successMsg: i18n.t('runtime:start rolling back runtime') },
      );
    },
    async redeployRuntime({ call, getParams, update }) {
      const { runtimeId } = getParams();
      await call(redeployRuntime, runtimeId, { successMsg: i18n.t('runtime:start redeploying the runtime') });
      runtime.clearServiceConfig();
    },
    async deleteRuntime({ call, getParams }) {
      const { runtimeId } = getParams();
      await call(deleteRuntime, runtimeId, { successMsg: i18n.t('runtime:start deleting runtime') });
      const path = window.location.pathname;
      const index = path.indexOf('/deploy') + '/deploy'.length;
      goTo(path.slice(0, index), { replace: true });
    },
  },
  reducers: {
    loadMoreDeploymentList(state, list: RUNTIME.DeployRecord[]) {
      state.deploymentList = state.deploymentList.concat(list);
    },
    updateRuntimeStatus(state, payload: { status: RUNTIME.Status; errors: string | null }) {
      state.runtimeDetail.status = payload.status;
      state.runtimeDetail.errors = payload.errors;
    },
    updateRuntimeDeployStatus(state, payload: { status: RUNTIME.DeployStatus }) {
      state.runtimeDetail.deployStatus = payload.status;
      state.cancelDeploying = false;
    },
    updateRuntimeDeleteStatus(state) {
      state.runtimeDetail.deleteStatus = 'DELETING';
    },
    updateRuntimeServiceStatus(
      state,
      payload: { serviceName: string; status: RUNTIME.Status; errors: RUNTIME_SERVICE.Err[] | null },
    ) {
      const { serviceName, status, errors } = payload;
      const targetService = state.runtimeDetail.services[serviceName];
      targetService.status = status;
      targetService.errors = errors;
    },
    toggleRedirect(state, show: boolean) {
      state.showRedirect = show;
    },
    clearRuntimeDetail(state) {
      state.runtimeDetail = defaultRuntimeDetail as RUNTIME.Detail;
      state.addons = [];
    },
    clearServiceConfig(state) {
      const runtimeId = state.runtimeDetail.id;
      removeLS(`${runtimeId}`);
      removeLS(`${runtimeId}_domain`);
      state.hasChange = false;
    },
    setHasChange(state, payload: boolean) {
      state.hasChange = payload;
    },
  },
});

export default runtime;
