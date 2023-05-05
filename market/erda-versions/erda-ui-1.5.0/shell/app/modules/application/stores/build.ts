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

/* eslint-disable no-param-reassign */
import { createStore } from 'core/cube';
import { getDefaultPaging } from 'common/utils';
import { ciNodeStatusSet } from 'application/pages/pipeline/run-detail/config';
import {
  getRuntimeDetail,
  getPipelineDetail,
  cancelBuild,
  cancelBuildCron,
  startBuildCron,
  reRunFailed,
  reRunEntire,
  runBuild,
  getExecuteRecords,
  updateTaskEnv,
  addPipeline,
  getComboPipelines,
  batchCreateTask,
  getPipelineLog,
  getPipelineYmlList,
} from '../services/build';
import { cloneDeep, get } from 'lodash';
import i18n from 'i18n';
import routeInfoStore from 'core/stores/route';

interface IState {
  runtimeDetail: any;
  pipelineDetail: BUILD.IPipelineDetail | null;
  changeType: 'task' | 'stage' | 'flow' | '';
  recordPaging: IPaging;
  executeRecords: BUILD.ExecuteRecord[];
  comboPipelines: BUILD.IComboPipeline[];
  pipelineLog: BUILD.IPipelineLog[];
  pipelineLogID: string;
}

const initState: IState = {
  comboPipelines: [],
  pipelineDetail: null,
  changeType: '',
  runtimeDetail: {},
  executeRecords: [],
  recordPaging: getDefaultPaging(),
  pipelineLog: [],
  pipelineLogID: '',
};

const build = createStore({
  name: 'appBuild',
  state: initState,
  subscriptions: ({ registerWSHandler }: IStoreSubs) => {
    registerWSHandler('PIPELINE_TASK_STATUS_UPDATE', ({ payload }) => {
      build.reducers.onTaskStatusChange(payload);
    });

    registerWSHandler('PIPELINE_TASK_RUNTIME_ID_UPDATE', ({ payload }) => {
      build.reducers.onTaskRuntimeIdChange(payload);
    });

    registerWSHandler('PIPELINE_STATUS_UPDATE', ({ payload }) => {
      build.effects.onPipelineStatusChange(payload);
    });
  },
  effects: {
    async getBuildRuntimeDetail({ call, update }, payload: { runtimeId: number }): Promise<RUNTIME.Detail> {
      const runtimeDetail = await call(getRuntimeDetail, payload);
      update({ runtimeDetail });
      return runtimeDetail;
    },
    async getPipelineDetail({ call, update }, payload: { pipelineID: number }) {
      const { pipelineID } = payload;
      const pipelineDetail = await call(getPipelineDetail, { pipelineID });
      update({ pipelineDetail, changeType: '' });
      return pipelineDetail;
    },
    async runBuild({ call, update }, payload: { pipelineID: number }) {
      const result = await call(runBuild, payload, {
        successMsg: i18n.t('dop:start executing the build'),
        fullResult: true,
      });
      const pipelineDetail = await call(getPipelineDetail, payload);
      update({ pipelineDetail, changeType: 'task' });
      return result;
    },
    async reRunEntire({ call }, payload: { pipelineID: number }): Promise<BUILD.IRerunResponse> {
      const result = await call(reRunEntire, payload, { successMsg: i18n.t('dop:start retrying build') });
      await call(runBuild, { pipelineID: result.id }); // 重试流水线只是新建一个流水线，不会自动跑
      return result;
    },
    async reRunFailed({ call }, payload: { pipelineID: number }): Promise<BUILD.IRerunResponse> {
      const result = await call(reRunFailed, payload, {
        successMsg: i18n.t('dop:start retrying failed nodes'),
      });
      await call(runBuild, { pipelineID: result.id });
      return result;
    },
    async startBuildCron({ call }, payload: { cronID: number }) {
      const result = await call(startBuildCron, payload, { successMsg: i18n.t('dop:timed successfully') });
      return result;
    },
    async cancelBuildCron({ call }, payload: { cronID: number }) {
      const result = await call(cancelBuildCron, payload, { successMsg: i18n.t('dop:timing canceled') });
      return result;
    },
    async cancelBuild({ call }, payload: { pipelineID: number }) {
      const result = await call(cancelBuild, payload, { successMsg: i18n.t('dop:build cancelled') });
      return result;
    },
    async getExecuteRecords(
      { call, update, getParams },
      payload: { branch: string; source: string; pageNo: number; pagingYmlNames: string[] },
    ) {
      const { appId } = getParams();
      const { branch, source, pageNo, pagingYmlNames } = payload;
      const pagingYmlNamesStr = (pagingYmlNames || []).join(',');
      const params = {
        branches: branch,
        appID: +appId,
        sources: source,
        ymlNames: pagingYmlNamesStr,
        pageNo,
        pageSize: 10,
      };
      const { list: executeRecords } = await call(getExecuteRecords, params, {
        paging: { key: 'recordPaging', listKey: 'pipelines' },
      });
      update({ executeRecords });
      return executeRecords;
    },
    async updateTaskEnv({ call }, payload: BUILD.ITaskUpdatePayload) {
      await call(updateTaskEnv, payload);
    },
    async addPipeline({ call, getParams }, payload: BUILD.CreatePipelineBody) {
      const { appId } = getParams();
      const detail = await call(
        addPipeline,
        { ...payload, appId },
        { successMsg: i18n.t('dop:build created successfully') },
      );
      return detail;
    },
    async getComboPipelines({ call, update, getParams }) {
      const { routes } = routeInfoStore.getState((s) => s);
      const { appId } = getParams();
      const params = routes.some((route) => route.path === 'dataTask')
        ? { sources: 'bigdata', branches: 'master' }
        : {};
      const comboPipelines = await call(getComboPipelines, { ...params, appId });
      update({ comboPipelines });
      return comboPipelines;
    },
    async onPipelineStatusChange({ select, update, call }, payload: any) {
      const { pipelineID, status, costTimeSec } = payload;
      let { pipelineDetail, comboPipelines } = select((s) => s);
      if (pipelineDetail && pipelineDetail.id === pipelineID) {
        pipelineDetail = await call(getPipelineDetail, { pipelineID });
      }
      const cpComboPipelines = cloneDeep(comboPipelines);
      const targetBuild = cpComboPipelines.find((o) => o.pipelineID === pipelineID);
      if (targetBuild) {
        targetBuild.status = status;
        targetBuild.costTimeSec = costTimeSec;
        comboPipelines = [...comboPipelines];
      }
      update({ comboPipelines: cpComboPipelines, pipelineDetail, changeType: 'flow' });
    },
    async batchCreateTask({ call, getParams }, payload: any) {
      const { appId } = getParams();
      const result = await call(
        batchCreateTask,
        { ...payload, appId },
        { successMsg: i18n.t('dop:start executing the build') },
      );
      return result;
    },
    async getPipelineLog({ call, select, getParams, update }, payload: { resourceId: string; resourceType: string }) {
      const [pipelineLogID, logList] = select((s) => [s.pipelineLogID, s.pipelineLog]);
      const { appId } = getParams();
      const query = {
        ...payload,
        scopeId: appId,
        scopeType: 'app',
        startTime: get(logList, '[0].occurrenceTime'),
      };
      if (pipelineLogID !== payload.resourceId) {
        query.startTime = '';
      }
      const { list = [] } = await call(getPipelineLog, query);
      const newList = pipelineLogID === payload.resourceId ? [...list, ...logList] : [...list];
      update({ pipelineLog: newList, pipelineLogID: payload.resourceId });
      return newList || [];
    },
    async getPipelineYmlList({ call }, payload: BUILD.PipelineYmlListQuery) {
      return call(getPipelineYmlList, payload);
    },
  },
  reducers: {
    clearPipelineDetail(state) {
      state.pipelineDetail = null;
    },
    clearComboPipelines(state) {
      state.comboPipelines = [];
    },
    clearPipelineLog(state) {
      state.pipelineLog = [];
      state.pipelineLogID = '';
    },
    clearExecuteRecords(state) {
      state.executeRecords = [];
      state.recordPaging = getDefaultPaging();
    },
    onTaskRuntimeIdChange(state, payload) {
      const { pipelineID, pipelineTaskID, runtimeID } = payload;
      const { pipelineDetail } = state;
      if (pipelineDetail && pipelineDetail.id === pipelineID) {
        const { pipelineStages } = pipelineDetail;
        pipelineStages.forEach((o) =>
          o.pipelineTasks.forEach((task) => {
            if (task.id === pipelineTaskID && runtimeID) {
              const metadata = [{ name: 'runtimeID', value: runtimeID }];
              task.result.metadata = metadata;
              state.changeType = 'task';
            }
          }),
        );
      }
    },
    onTaskStatusChange(state, payload) {
      const { pipelineID, pipelineTaskID, status, costTimeSec, result } = payload;
      const { pipelineDetail } = state;
      if (pipelineDetail && pipelineDetail.id === pipelineID) {
        const { pipelineStages } = pipelineDetail;
        if (ciNodeStatusSet.taskFinalStatus.includes(status)) {
          // refresh pipeline detail when task is final status, prevent websocket data append
          build.effects.getPipelineDetail({ pipelineID });
        }
        pipelineStages.forEach((o) =>
          o.pipelineTasks.forEach((task) => {
            if (task.id === pipelineTaskID) {
              task.status = status;
              task.costTimeSec = costTimeSec;
              task.result = result;
              state.changeType = 'task';
            }
          }),
        );
      }
    },
  },
});

export default build;
