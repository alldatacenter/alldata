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

import agent from 'agent';
import { get } from 'lodash';
import { setApiWithOrg } from 'common/utils';
import routeInfoStore from 'core/stores/route';

// 获取测试计划列表
export function getPlanList(query: TEST_PLAN.PlanListQuery): TEST_PLAN.PlanPaging {
  return agent
    .get('/api/testplans')
    .query(query)
    .then((response: any) => response.body);
}
// 添加测试计划
export function addTestPlan(payload: TEST_PLAN.PlanSubmitBody): number {
  return agent
    .post('/api/testplans')
    .send(payload)
    .then((response: any) => response.body);
}
// 更新测试计划
export function updateTestPlan({ id, ...others }: Partial<TEST_PLAN.PlanSubmitBody>): boolean {
  return agent
    .put(`/api/testplans/${id}`)
    .send(others)
    .then((response: any) => response.body);
}

// 获取测试计划详情
export function getTestPlanItem(testPlanID: number): TEST_PLAN.PlanBodySimpleResponse {
  return agent.get(`/api/testplans/${testPlanID}`).then((response: any) => response.body);
}

// 获取测试计划详情，含用例数等统计信息
export function getTestPlanItemDetail(testPlanId: number): TEST_PLAN.PlanBodySimpleResponse {
  return agent.get(`/api/testplans/${testPlanId}`).then((response: any) => response.body);
}

// 将测试集/用例添加到测试计划
export function addToPlan({ testPlanID, ...rest }: TEST_PLAN.relationsCase): { totalCount: number } {
  return agent
    .post(`/api/testplans/${testPlanID}/testcase-relations`)
    .send(rest)
    .then((response: any) => response.body);
}

export function getReport(testPlanID: number): TEST_PLAN.PlanReportResponse {
  return agent.get(`/api/testplans/${testPlanID}/actions/generate-report`).then((response: any) => response.body);
}

export function updateTestCaseRelations({ testPlanID, ...payload }: TEST_PLAN.PlanBatch) {
  return agent
    .post(`/api/testplans/${testPlanID}/testcase-relations/actions/batch-update`)
    .send(payload)
    .then((response: any) => response.body);
}

// 更新测试计划的时间
export function updateDate({ testPlanId, date }: TEST_PLAN.PlanDate) {
  const { params } = routeInfoStore.getState((s) => s);
  const projectId = get(params, 'projectId');
  return agent
    .put(`/api/pmp/api/testing/testplan/${testPlanId}/update/date?projectId=${projectId}`)
    .query(date)
    .then((response: any) => response.body);
}

export function executeCaseApi({ testPlanID, ...rest }: TEST_PLAN.CaseApi) {
  return agent
    .post(`/api/testplans/${testPlanID}/actions/execute-apitest`)
    .send({ ...rest })
    .then((response: any) => response.body);
}

export const getExecuteRecords = ({
  testPlanId,
  projectId,
  pageNo,
  pageSize,
}: TEST_PLAN.ExecuteRecordQuery): Promise<{
  pipelines: TEST_PLAN.Pipeline[];
  total: number;
  currentPageSize: number;
}> => {
  return agent
    .get('/api/pipelines')
    .query({
      pageNo,
      pageSize,
      mustMatchLabels: JSON.stringify({ testPlanID: testPlanId }),
      sources: 'api-test',
      ymlNames: `api-test-${projectId}.yml`,
    })
    .then((response: any) => response.body);
};

export const getExecuteRecordDetail = ({
  runVersion,
  pipelineID,
}: TEST_PLAN.ExecuteRecordDetailQuery): TEST_PLAN.PipeLineDetail => {
  return agent
    .get(`/api/apitests/pipeline/${pipelineID}`)
    .query({ rerunVer: runVersion })
    .then((response: any) => response.body);
};

export const cancelBuild = ({ testPlanID, pipelineID }: { testPlanID: string; pipelineID: number }) => {
  return agent
    .post(`/api/testplans/${testPlanID}/actions/cancel-apitest/${pipelineID}`)
    .then((response: any) => response.body);
};

export const exportFiles = ({ testPlanID, queryParam }: { testPlanID: number; queryParam: string }) => {
  window.open(setApiWithOrg(`/api/testplans/${testPlanID}/actions/export?${queryParam}`));
};
