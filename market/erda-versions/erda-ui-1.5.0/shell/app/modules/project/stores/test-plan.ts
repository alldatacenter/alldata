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

import i18n, { getLang } from 'i18n';
import { message } from 'antd';
import { createStore } from 'core/cube';
import { getDefaultPaging } from 'common/utils';
import {
  addTestPlan,
  addToPlan,
  getPlanList,
  getReport,
  getTestPlanItem,
  getTestPlanItemDetail,
  updateTestCaseRelations,
  cancelBuild,
  exportFiles,
  updateTestPlan,
  executeCaseApi,
  getExecuteRecords,
  getExecuteRecordDetail,
} from '../services/test-plan';
import testCaseStore from './test-case';
import testSetStore from './test-set';
import { PAGINATION } from 'app/constants';
import userStore from 'user/stores';

const getEmptyTestSetIds = (ids: number[]): number[] => {
  const caseList = testCaseStore.getState((s) => s.caseList);
  const data: number[] = [];
  caseList.forEach(({ testSetID, testCases, testCaseCountWithoutFilter }) => {
    let sum = 0;
    testCases.forEach(({ id }) => {
      if (ids.includes(id)) {
        sum += 1;
      }
    });
    if (testCaseCountWithoutFilter === sum) {
      data.push(testSetID);
    }
  });
  return data;
};

interface IState {
  planPaging: IPaging;
  planList: TEST_PLAN.PlanBodySimpleResponse[];
  planTotal: number;
  planItem: TEST_PLAN.PlanBodySimpleResponse;
  issueList: any;
  planItemDetail: TEST_PLAN.PlanBodySimpleResponse;
  relatedPlansPaging: IPaging;
  relatedPlans: TEST_PLAN.PlanBodySimpleResponse[];
  planModalInfo: {
    // 添加计划的弹框
    type: string; // 集合 'collection', 用例 'case', 用例多选 'multi'
    ids: number[]; // 测试集/用例的ids
    selectProjectId: null | number; // 被打开时，选中的项目
  };
  planReport: TEST_PLAN.PlanReportResponse;
  remarkModalInfo: {
    // 添加备注的弹框
    type: string; // 用例 'case', 用例多选 'multi'
    id: null | number;
    remark: string;
  };
  rowSelectIssueModalVisible: boolean;
  statsByModule: TEST_PLAN.PlanStats;
  pipelineDetail: TEST_PLAN.PipeLineDetail;
  executeRecordsPaging: IPaging;
  executeRecords: TEST_PLAN.Pipeline[];
  changeType: 'stage' | 'node' | '' | 'task'; // ws更新流水线的类型：stage | node | task
}

const defaultPlanModalInfo = {
  type: '',
  ids: [],
  selectProjectId: null,
};

const defaultRemarkModalInfo = {
  type: '',
  id: null,
  remark: '',
};

const initState: IState = {
  planPaging: getDefaultPaging(),
  planList: [],
  planTotal: 0,
  planItem: {} as TEST_PLAN.PlanBodySimpleResponse,
  issueList: {},
  planItemDetail: {} as TEST_PLAN.PlanBodySimpleResponse,
  relatedPlansPaging: getDefaultPaging(),
  relatedPlans: [],
  planModalInfo: defaultPlanModalInfo,
  planReport: {} as TEST_PLAN.PlanReportResponse,
  remarkModalInfo: defaultRemarkModalInfo,
  rowSelectIssueModalVisible: false,
  statsByModule: {} as TEST_PLAN.PlanStats,
  pipelineDetail: {} as TEST_PLAN.PipeLineDetail,
  executeRecordsPaging: getDefaultPaging(),
  executeRecords: [],
  changeType: '',
};

const typeMap = {
  manual: 'm',
  auto: 'a',
};

const testPlan = createStore({
  name: 'testPlan',
  state: initState,
  subscriptions: ({ registerWSHandler }: IStoreSubs) => {
    registerWSHandler('PIPELINE_TASK_STATUS_UPDATE', ({ payload }) => {
      testPlan.reducers.onTaskStatusChange(payload);
    });
  },
  effects: {
    async getPlanList({ call, update, getParams }, payload?: Omit<TEST_PLAN.PlanListQuery, 'projectID'>) {
      const { projectId: projectID, testType = 'manual' } = getParams();
      const result = await call(
        getPlanList,
        { pageNo: 1, pageSize: PAGINATION.pageSize, ...payload, projectID, type: typeMap[testType] },
        { paging: { key: 'planPaging' } },
      );
      update({ planList: result.list, planTotal: result.total });
      return result;
    },
    async addTestPlan({ call, getParams }, payload: TEST_PLAN.PlanSubmitBody) {
      const { projectId } = getParams();
      await call(addTestPlan, { ...payload, projectID: +projectId });
    },
    async updateTestPlan({ call, getParams }, payload: TEST_PLAN.PlanSubmitBody) {
      const { testPlanId } = getParams();
      await call(updateTestPlan, { ...payload });
      // 在计划详情页时，重新拉取详情
      if (testPlanId) {
        testPlan.effects.getTestPlanItemDetail(testPlanId);
      }
    },
    async getTestPlanItem({ call, update }, testPlanId) {
      const planItem = await call(getTestPlanItem, testPlanId);
      update({ planItem });
    },
    async getTestPlanItemDetail({ call, update }, testPlanId: number) {
      const planItemDetail = await call(getTestPlanItemDetail, testPlanId);
      update({ planItemDetail });
    },
    async getPlanRelateMe({ call, getParams, update, select }, payload: { pageNo: number; name?: string }) {
      const { id: userID } = userStore.getState((s) => s.loginUser);
      const { projectId } = getParams();
      const { list } = await call(
        getPlanList,
        { ...payload, pageSize: 10, userID: +userID, projectID: +projectId },
        { paging: { key: 'relatedPlansPaging' } },
      );
      let relatedPlans = list;
      if (payload.pageNo > 1) {
        const prevList = select((s) => s.relatedPlans);
        relatedPlans = [...prevList, ...list];
      }
      update({ relatedPlans });
    },
    async updatePlanStatus({ call, select }, payload: { id: number; status: TEST_PLAN.PlanStatus }) {
      const { ownerID, partnerIDs } = select((s) => s.planItemDetail);
      await call(updateTestPlan, { ownerID, partnerIDs, ...payload });
      testPlan.effects.getTestPlanItemDetail(payload.id);
    },
    // 测试用例中添加用例至测试计划
    async addToPlanByPlanModal({ call, select }, testPlanID: number) {
      const { type, ids } = select((s) => s.planModalInfo);
      let res = { totalCount: 0 };
      if (type === 'collection') {
        // 测试集添加测试计划
        res = await call(addToPlan, { testPlanID, testSetIDs: ids });
      } else if (type === 'case') {
        // 测试用例添加测试计划
        res = await call(addToPlan, { testPlanID, testCaseIDs: ids });
      } else if (type === 'multi') {
        // 用例多选
        const { testCaseIDs } = await testCaseStore.effects.getSelectedCaseIds();
        res = await call(addToPlan, { testPlanID, testCaseIDs });
        testCaseStore.reducers.clearChoosenInfo({ mode: 'testCase' });
      }
      message.success(i18n.t('{total} item added successfully', { total: res.totalCount }));
    },
    // 测试计划中新建用例自动关联计划
    async addSingleCaseToTestPlan({ call, getParams }, { testCaseIDs }: { testCaseIDs: number[] }) {
      const { testPlanId: testPlanID } = getParams();
      const res = await call(addToPlan, { testPlanID, testCaseIDs });
      message.success(i18n.t('{total} item added successfully', { total: res.totalCount }));
      return res;
    },
    async addToPlanInCaseModal({ call, getParams }, query: { testSetID: number; parentId: number }) {
      const { projectId, testPlanId: testPlanID } = getParams();
      const selectProjectId = projectId;
      const { testCaseIDs } = await testCaseStore.effects.getSelectedCaseIds('caseModal');
      const { totalCount } = await call(addToPlan, { testPlanID, testCaseIDs });
      testSetStore.reducers.updateReloadTestSet({
        isMove: false,
        testSetID: query.testSetID,
        parentID: query.parentId,
        reloadParent: true,
        selectProjectId,
        projectID: projectId,
      });
      testCaseStore.reducers.triggerChoosenAll({ scope: 'caseModal', isAll: false });
      testCaseStore.effects.getCases();
      testPlan.effects.getTestPlanItemDetail(testPlanID);
      message.success(i18n.t('{total} item added successfully', { total: totalCount }));
    },
    async getReport({ call, update }, testPlanID: number) {
      const planReport = await call(getReport, testPlanID);
      update({ planReport });
    },
    async deleteRelations(
      { call, getParams },
      { relationIDs, type }: { relationIDs: number[]; type: 'multi' | 'single' },
    ) {
      const { testPlanId: testPlanID } = getParams();
      let emptyTestSetIDs: number[] = [];
      if (type === 'single') {
        emptyTestSetIDs = getEmptyTestSetIds(relationIDs);
        await call(updateTestCaseRelations, { relationIDs, delete: true, testPlanID });
      } else if (type === 'multi') {
        const { testCaseIDs } = await testCaseStore.effects.getSelectedCaseIds();
        emptyTestSetIDs = getEmptyTestSetIds(testCaseIDs);
        await call(updateTestCaseRelations, { relationIDs: testCaseIDs, delete: true, testPlanID });
      }
      testCaseStore.reducers.clearChoosenInfo({ mode: 'testPlan' });
      // testCaseStore.effects.getCases();
      message.success(i18n.t('deleted successfully'));
      return emptyTestSetIDs;
    },
    async deleteTestPlans({ call, getParams }, testSetID: number) {
      const { testPlanId: testPlanID } = getParams();
      const caseList = testCaseStore.getState((s) => s.caseList);
      const relationIDs: number[] = caseList.map(({ testSetID: id }) => id);

      await call(
        updateTestCaseRelations,
        { relationIDs, delete: true, testPlanID, testSetID },
        { successMsg: i18n.t('deleted successfully') },
      );

      testCaseStore.reducers.clearChoosenInfo({ mode: 'testPlan' });
      return relationIDs;
    },
    async updateCasesStatus({ call, getParams }, payload: Omit<TEST_PLAN.PlanBatch, 'testPlanID'>) {
      const { testPlanId } = getParams();
      const { id: executorID } = userStore.getState((s) => s.loginUser);
      await call(updateTestCaseRelations, { executorID, testPlanID: testPlanId, ...payload });
      testPlan.effects.getTestPlanItemDetail(testPlanId);
      testCaseStore.reducers.clearChoosenInfo({ mode: 'testPlan' });
      testCaseStore.effects.getCases();
    },
    async planUserCaseBatch(
      { call, getParams },
      payload: Partial<Omit<TEST_PLAN.PlanBatch, 'testPlanID' | 'relationIDs'>>,
    ) {
      const { testPlanId } = getParams();
      const { testCaseIDs: relationIDs } = await testCaseStore.effects.getSelectedCaseIds();
      await call(updateTestCaseRelations, { testPlanID: testPlanId, relationIDs, ...payload });
      message.success(i18n.t('updated successfully'));
      testCaseStore.effects.getCases();
      testCaseStore.reducers.clearChoosenInfo({ mode: 'testPlan' });
    },
    async updateSummary({ call, getParams }, payload: Pick<TEST_PLAN.PlanSubmitBody, 'summary'>) {
      const { testPlanId: id } = getParams();
      await call(updateTestPlan, { id, ...payload });
      message.success(i18n.t('updated successfully'));
    },
    async updateDate(
      { call, getParams },
      payload: Pick<TEST_PLAN.PlanSubmitBody, 'timestampSecEndedAt' | 'timestampSecStartedAt'>,
    ) {
      const { testPlanId: id } = getParams();
      const newDate = await call(updateTestPlan, { id, ...payload });
      testPlan.effects.getReport(id);
      return newDate;
    },
    async toggleArchived({ call }, payload: { id: number; isArchived: boolean }) {
      await call(updateTestPlan, payload, { successMsg: i18n.t('operated successfully') });
    },
    async executeCaseApi({ call, getParams }, payload: Omit<TEST_PLAN.CaseApi, 'testPlanID'>) {
      const { testPlanId: testPlanID } = getParams();
      await call(
        executeCaseApi,
        { testPlanID, ...payload },
        { successMsg: i18n.t('dop:start performing interface testing, please wait') },
      );
    },
    async getExecuteRecords(
      { call, update, getParams },
      payload: Omit<TEST_PLAN.ExecuteRecordQuery, 'projectId' | 'testPlanId'>,
    ) {
      const { projectId, testPlanId } = getParams();
      const { pipelines: executeRecords, total } = await call(
        getExecuteRecords,
        { projectId, testPlanId, ...payload },
        { paging: { key: 'executeRecordsPaging', listKey: 'pipelines' } },
      );
      update({ executeRecords });
      return { list: executeRecords, total };
    },
    async getPipelineDetail({ call, update }, payload: TEST_PLAN.ExecuteRecordDetailQuery) {
      const pipelineDetail = await call(getExecuteRecordDetail, payload);
      update({ pipelineDetail });
    },
    async cancelBuild({ call, getParams }, { pipelineID }: { pipelineID: number }) {
      const { testPlanId: testPlanID } = getParams();
      await call(
        cancelBuild,
        { pipelineID, testPlanID },
        { successMsg: i18n.t('dop:interface test has been canceled') },
      );
    },
    async exportFiles({ call, getParams }, queryParam: string) {
      const { testPlanId: testPlanID } = getParams();
      const lang = getLang();
      await call(exportFiles, { testPlanID, queryParam: `lang=${lang}&${queryParam}` });
    },
  },
  reducers: {
    clearPlanList(state) {
      state.planList = [];
      state.planPaging = getDefaultPaging();
      state.planTotal = 0;
    },
    openPlanModal(state, payload) {
      state.planModalInfo = payload;
    },
    closePlanModal(state) {
      state.planModalInfo = defaultPlanModalInfo;
    },
    openRemarkModal(state, payload) {
      state.remarkModalInfo = payload;
    },
    closeRemarkModal(state) {
      state.remarkModalInfo = defaultRemarkModalInfo;
    },
    resetStore() {
      return initState;
    },
    clearPipelineDetail(state) {
      state.pipelineDetail = {} as TEST_PLAN.PipeLineDetail;
    },
    clearExecuteRecords(state) {
      state.executeRecords = [];
    },
    cleanTestPlan(state) {
      state.planItem = {} as TEST_PLAN.PlanBodySimpleResponse;
    },
    onTaskRuntimeIdChange(state, payload) {
      const { pipelineID, pipelineTaskID, runtimeID } = payload;
      const { pipelineDetail } = state;
      if (pipelineDetail && pipelineDetail.id === pipelineID) {
        const { pipelineStages } = pipelineDetail;

        pipelineStages.forEach((o: any) =>
          o.pipelineTasks.forEach((task: any) => {
            if (task.id === pipelineTaskID && runtimeID) {
              const metadata = [{ name: 'runtimeID', value: runtimeID }];
              // eslint-disable-next-line no-param-reassign
              task.result.metadata = metadata;
              state.changeType = 'task';
            }
          }),
        );
      }
    },
    onTaskStatusChange(state, payload) {
      const { pipelineID, pipelineTaskID, status, costTimeSec } = payload;
      const { pipelineDetail } = state;
      if (pipelineDetail && pipelineDetail.id === pipelineID) {
        const { pipelineStages } = pipelineDetail;

        pipelineStages.forEach((o: any) =>
          o.pipelineTasks.forEach((task: any) => {
            if (task.id === pipelineTaskID) {
              // eslint-disable-next-line no-param-reassign
              task.status = status;
              // eslint-disable-next-line no-param-reassign
              task.costTimeSec = costTimeSec;
              state.changeType = 'task';
            }
          }),
        );
      }
    },
  },
});

export default testPlan;
