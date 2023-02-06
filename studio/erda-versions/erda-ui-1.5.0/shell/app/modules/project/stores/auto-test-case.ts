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
import i18n from 'i18n';
import { getDefaultPaging } from 'common/utils';
import { get } from 'lodash';
import {
  getCaseDetail,
  updateCasePipeline,
  getAutoTestConfigEnv,
  getPipelineRecordList,
  getConfigDetailRecordList,
  getPipelineDetail,
  getPipelineReport,
  cancelBuild,
  runBuild,
  reRunFailed,
  reRunEntire,
  updateTaskEnv,
} from '../services/auto-test-case';

interface IState {
  caseDetail: AUTO_TEST.ICaseDetail;
  configEnvs: AUTO_TEST.IConfigEnv[];
  changeType: 'task' | 'stage' | 'flow' | '';
  pipelineRecordList: PIPELINE.IPipeline[];
  pipelineRecordPaging: IPaging;
  firstPipelineRecord: PIPELINE.IPipeline | null;
  pipelineDetail: PIPELINE.IPipelineDetail;
  configDetailRecordList: AUTO_TEST.ICaseDetail[];
  pipelineReportList: any;
}

const initState: IState = {
  caseDetail: {} as AUTO_TEST.ICaseDetail,
  configEnvs: [],
  changeType: '',
  pipelineRecordList: [],
  firstPipelineRecord: null,
  pipelineRecordPaging: getDefaultPaging(),
  pipelineDetail: {} as PIPELINE.IPipelineDetail,
  configDetailRecordList: [],
  pipelineReportList: [],
};

const autoTestCase = createFlatStore({
  name: 'autoTestStore',
  state: initState,
  effects: {
    async getCaseDetail({ call, update }, payload: { id: string }) {
      const caseDetail = await call(getCaseDetail, payload);
      update({ caseDetail });
      return caseDetail;
    },
    async updateCasePipeline({ call }, payload: AUTO_TEST.IUpdateCaseBody) {
      const res = await call(updateCasePipeline, payload, { successMsg: i18n.t('saved successfully') });
      return res;
    },
    async getAutoTestConfigEnv({ call, update }, payload: AUTO_TEST.IConfigEnvQuery) {
      const configEnvs = await call(getAutoTestConfigEnv, payload);
      update({ configEnvs: configEnvs || [] });
      return configEnvs || [];
    },
    async getPipelineRecordList({ call, update }, payload: AUTO_TEST.IRunRecordQuery) {
      const res = await call(getPipelineRecordList, payload, {
        paging: { key: 'pipelineRecordPaging', listKey: 'pipelines' },
      });
      const updateObj = {
        pipelineRecordList: res.list || [],
      } as any;
      if (payload.pageNo === 1 && get(res, 'list.length') > 0) {
        updateObj.firstPipelineRecord = res.list[0];
      }
      update({ ...updateObj });
      return res;
    },
    async getPipelineDetail({ call, update }, payload: { pipelineID: string }) {
      const pipelineDetail = await call(getPipelineDetail, payload);
      update({ pipelineDetail, changeType: '' });
      return pipelineDetail;
    },
    async getPipelineReport({ call, update }, payload: { pipelineID: number }) {
      const pipelineReport = await call(getPipelineReport, payload);
      update({ pipelineReportList: pipelineReport.reports });
      return pipelineReport;
    },
    async cancelBuild({ call }, payload: { pipelineID: string }) {
      const result = await call(cancelBuild, payload, { successMsg: i18n.t('dop:build cancelled') });
      return result;
    },
    async runBuild({ call, update }, payload: { pipelineID: string; runPipelineParams?: any }) {
      const { pipelineID } = payload;
      const result = await call(runBuild, payload, {
        successMsg: i18n.t('dop:start executing the build'),
        fullResult: true,
      });
      const pipelineDetail = await call(getPipelineDetail, { pipelineID });
      update({ pipelineDetail, changeType: 'task' });
      return result;
    },
    async reRunFailed(
      { call },
      payload: { pipelineID: string; runPipelineParams?: any },
    ): Promise<BUILD.IRerunResponse> {
      const { pipelineID, runPipelineParams } = payload;
      const result = await call(reRunFailed, { pipelineID }, { successMsg: i18n.t('dop:start retrying failed nodes') });
      await call(runBuild, { pipelineID: result.id, runPipelineParams });
      return result;
    },
    async reRunEntire(
      { call },
      payload: { pipelineID: string; runPipelineParams?: any },
    ): Promise<BUILD.IRerunResponse> {
      const { pipelineID, runPipelineParams } = payload;
      const result = await call(reRunEntire, { pipelineID }, { successMsg: i18n.t('dop:start retrying build') });
      await call(runBuild, { pipelineID: result.id, runPipelineParams }); // 重试流水线只是新建一个流水线，不会自动跑
      return result;
    },
    // async onPipelineStatusChange({ select, update, call }, payload: any) {
    //   const { pipelineID } = payload;
    //   let { pipelineDetail } = select(s => s);
    //   if (pipelineDetail && pipelineDetail.id === pipelineID) {
    //     pipelineDetail = await call(getPipelineDetail, { pipelineID });
    //   }
    //   update({ pipelineDetail, changeType: 'flow' });
    // },
    async updateTaskEnv({ call }, payload: { pipelineID: string; taskID: number; disabled: boolean }) {
      const { pipelineID, taskID, disabled } = payload;
      await call(updateTaskEnv, { taskID, disabled, pipelineID });
    },

    async getConfigDetailRecordList({ call, update }, payload: string) {
      const res = await call(getConfigDetailRecordList, payload, { paging: { listKey: 'data' } });
      update({ configDetailRecordList: res });
      return res;
    },
  },
  reducers: {
    clearCaseDetail(state) {
      state.caseDetail = {} as AUTO_TEST.ICaseDetail;
    },
    clearConfigEnvs(state) {
      state.configEnvs = [];
    },
    clearPipelineRecord(state) {
      state.pipelineRecordList = [];
      state.pipelineRecordPaging = getDefaultPaging();
    },
    clearPipelineDetail(state) {
      state.pipelineDetail = {} as PIPELINE.IPipelineDetail;
    },
    clearPipelineReport(state) {
      state.pipelineReportList = [];
    },
    clearConfigDetailRecordList(state) {
      state.configDetailRecordList = [];
    },
    // onTaskRuntimeIdChange(state, payload) {
    //   const { pipelineID, pipelineTaskID } = payload;
    //   const { pipelineDetail } = state;
    //   if (pipelineDetail && pipelineDetail.id === pipelineID) {
    //     const { pipelineStages } = pipelineDetail;
    //     pipelineStages.forEach(o => o.pipelineTasks.forEach((task) => {
    //       if (task.id === pipelineTaskID) {
    //         state.changeType = 'task';
    //       }
    //     }));
    //   }
    // },
    // onTaskStatusChange(state, payload) {
    //   const { pipelineID, pipelineTaskID, status, costTimeSec, result } = payload;
    //   const { pipelineDetail } = state;
    //   if (pipelineDetail && pipelineDetail.id === pipelineID) {
    //     const { pipelineStages } = pipelineDetail;
    //     pipelineStages.forEach(o => o.pipelineTasks.forEach((task) => {
    //       if (task.id === pipelineTaskID) {
    //         task.status = status;
    //         task.costTimeSec = costTimeSec;
    //         task.result = result;
    //         state.changeType = 'task';
    //       }
    //     }));
    //   }
    // },
  },
});

export default autoTestCase;
