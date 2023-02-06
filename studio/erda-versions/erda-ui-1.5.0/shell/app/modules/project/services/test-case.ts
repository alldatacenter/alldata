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
import i18n, { getLang } from 'i18n';
import { qs, setApiWithOrg } from 'common/utils';
import { TestOperation } from 'project/pages/test-manage/constants';

export function getFields(): TEST_CASE.Field[] {
  // if (!mock) {
  //   return agent.get('/api/pmp/api/metadata/test/usecase')
  //     .then(response => response.body)
  // .catch((err) => { throw new Error(err.rawResponse); });
  // }
  return [
    {
      uniqueName: TestOperation.priority,
      showName: i18n.t('dop:priority'),
      dataType: 'STRING',
      module: 'TEST',
      defaultValue: 'P3',
      optionsUrl: null,
      enumerable: true,
      multiple: false,
      fieldRequired: true,
      contentRequired: false,
      desc: '',
      icon: null,
      order: null,
      fieldTypeCode: 'select',
      enums: [
        { fieldUniqueName: TestOperation.priority, showName: 'P0', value: 'P0', icon: null, order: null },
        {
          fieldUniqueName: TestOperation.priority,
          showName: i18n.t('dop:p1'),
          value: 'P1',
          icon: null,
          order: null,
        },
        {
          fieldUniqueName: TestOperation.priority,
          showName: i18n.t('dop:p2'),
          value: 'P2',
          icon: null,
          order: null,
        },
        { fieldUniqueName: TestOperation.priority, showName: 'P3', value: 'P3', icon: null, order: null },
      ],
    },
    {
      uniqueName: TestOperation.testPlanTestCasesExecutionResult,
      showName: i18n.t('dop:results of the'),
      dataType: 'STRING',
      module: 'TEST',
      defaultValue: 'INIT',
      optionsUrl: null,
      enumerable: true,
      multiple: false,
      fieldRequired: true,
      contentRequired: false,
      desc: '',
      icon: null,
      order: null,
      fieldTypeCode: 'select',
      enums: [
        {
          fieldUniqueName: TestOperation.testPlanTestCasesExecutionResult,
          showName: i18n.t('dop:not performed'),
          value: 'INIT',
          icon: null,
          order: null,
        },
        {
          fieldUniqueName: TestOperation.testPlanTestCasesExecutionResult,
          showName: i18n.t('dop:pass'),
          value: 'PASSED',
          icon: null,
          order: null,
        },
        {
          fieldUniqueName: TestOperation.testPlanTestCasesExecutionResult,
          showName: i18n.t('dop:not passed'),
          value: 'FAIL',
          icon: null,
          order: null,
        },
        {
          fieldUniqueName: TestOperation.testPlanTestCasesExecutionResult,
          showName: i18n.t('dop:blocking'),
          value: 'BLOCK',
          icon: null,
          order: null,
        },
      ],
    },
    {
      uniqueName: TestOperation.testPlanStatus,
      showName: i18n.t('dop:test plan status'),
      dataType: 'STRING',
      module: 'TEST',
      defaultValue: 'DOING',
      optionsUrl: null,
      enumerable: true,
      multiple: false,
      fieldRequired: true,
      contentRequired: false,
      desc: '',
      icon: null,
      order: null,
      fieldTypeCode: 'select',
      enums: [
        {
          fieldUniqueName: TestOperation.testPlanStatus,
          showName: i18n.t('processing'),
          value: 'DOING',
          icon: null,
          order: null,
        },
        {
          fieldUniqueName: TestOperation.testPlanStatus,
          showName: i18n.t('pause'),
          value: 'PAUSE',
          icon: null,
          order: null,
        },
        {
          fieldUniqueName: TestOperation.testPlanStatus,
          showName: i18n.t('dop:abandoned'),
          value: 'DISCARD',
          icon: null,
          order: null,
        },
        {
          fieldUniqueName: TestOperation.testPlanStatus,
          showName: i18n.t('dop:completed'),
          value: 'DONE',
          icon: null,
          order: null,
        },
      ],
    },
  ];
}

// 新建测试用例
export function create(payload: TEST_CASE.CaseBody, testPlanId?: number) {
  return agent
    .post('/api/testcases')
    .query({ testPlanId })
    .send(payload)
    .then((response: any) => response.body);
}

// 获取测试用例详情
export function getDetail({ id }: Merge<TEST_CASE.QueryCaseDetail, { testPlanID: number }>): TEST_CASE.CaseDetail {
  return agent.get(`/api/testcases/${id}`).then((response: any) => response.body);
}

export function getDetailRelations({
  id,
  testPlanID,
}: {
  id: number;
  testPlanID: number;
}): TEST_CASE.CaseRelationDetail {
  return agent.get(`/api/testplans/${testPlanID}/testcase-relations/${id}`).then((response: any) => response.body);
}

// 编辑测试用例
export function editPartial({ id, ...payload }: TEST_CASE.CaseBody) {
  return agent
    .put(`/api/testcases/${id}`)
    .send(payload)
    .then((response: any) => response.body);
}

export function exportFileInTestCase(payload: TEST_CASE.ExportFileQuery) {
  const lang = getLang();
  const query = qs.stringify({ ...payload, lang } as any, { arrayFormat: 'none' });
  return agent.get(`/api/testcases/actions/export?${query}`).then((response: any) => response.body);
}

export function importFileInTestCase({ payload, query }: TEST_CASE.ImportData): { successCount: number } {
  return agent
    .post('/api/testcases/actions/import')
    .query(query)
    .send(payload)
    .then((response: any) => response.body);
}

export function importFileInAutoTestCase({ payload, query }: TEST_CASE.ImportAutoData): { successCount: number } {
  return agent
    .post('/api/autotests/spaces/actions/import')
    .query(query)
    .send(payload)
    .then((response: any) => response.body);
}
export function importFileInAutoTestCaseSet({ payload, query }: TEST_CASE.ImportAutoData): { successCount: number } {
  return agent
    .post('/api/autotests/scenesets/actions/import')
    .query(query)
    .send(payload)
    .then((response: any) => response.body);
}

// 批量更新测试用例
export function updateCases({ query, payload }: { query: TEST_CASE.CaseFilter; payload: TEST_CASE.CaseBodyPart }) {
  return agent
    .put('/api/testcases/batch')
    .query(query)
    .send(payload)
    .then((response: any) => response.body);
}

// 批量更新测试用例（更新优先级，移入/移出回收站）
export function batchUpdateCase(payload: TEST_CASE.BatchUpdate) {
  return agent
    .post('/api/testcases/actions/batch-update')
    .send(payload)
    .then((response: any) => response.body);
}

// 批量增量增加标签
// export function updateTags({ query, payload }: { query: TEST_CASE.CaseFilter, payload: TEST_CASE.CaseBodyPart}) {
//   return agent.put('/api/pmp/api/testing/usecase/batch/label')
//     .query(query)
//     .send(payload)
//     .then((response: any) => response.body)
//     .catch((err: any) => { throw new Error(err.rawResponse); });
// }

// 彻底删除测试用例
export function deleteEntirely(payload: { testCaseIDs: number[] }) {
  return agent
    .delete('/api/testcases/actions/batch-clean-from-recycle-bin')
    .send(payload)
    .then((response: any) => response.body);
}

// 复制测试用例
export function copyCases(query: TEST_CASE.BatchCopy) {
  return agent
    .post('/api/testcases/actions/batch-copy')
    .send(query)
    .then((response: any) => response.body);
}

// 获取测试用例列表
export function getCases(query: Merge<TEST_CASE.QueryCase, TEST_CASE.QueryCaseSort>): TEST_CASE.CasePage {
  return agent
    .get('/api/testcases')
    .query(query)
    .then((response: any) => response.body);
}

export function getCasesRelations({
  testPlanID,
  ...query
}: Merge<TEST_CASE.QueryCase, TEST_CASE.QueryCaseSort>): TEST_CASE.CasePage {
  return agent
    .get(`/api/testplans/${testPlanID}/testcase-relations`)
    .query(query)
    .then((response: any) => response.body);
}

// export function updateRelBug(query: TEST_CASE.CaseBugsFilter) {
//   return agent.post('/api/pmp/api/testing/usecase/relate/bug')
//     .query(query)
//     .then((response: any) => response.body)
//     .catch((err: any) => { throw new Error(err.rawResponse); });
// }

// export function deleteRelBug(query: TEST_CASE.CaseBugsFilter) {
//   return agent.delete('/api/pmp/api/testing/usecase/relate/bug')
//     .query(query)
//     .then((response: any) => response.body)
//     .catch((err: any) => { throw new Error(err.rawResponse); });
// }

// export function getCaseBugIds({ id, testPlanId }: TEST_CASE.CaseBugs) {
//   return agent.get(`/api/pmp/api/testing/usecase/${id}/relate/bug`)
//     .query({ testPlanId })
//     .then((response: any) => response.body)
//     .catch((err: any) => { throw new Error(err.rawResponse); });
// }

export function attemptTestApi(data: TEST_CASE.TestApi) {
  return agent
    .post('/api/apitests/actions/attempt-test')
    .send(data)
    .then((response: any) => response.body);
}

export function removeRelation({ testPlanID, id, issueTestCaseRelationIDs }: TEST_CASE.RemoveRelation) {
  return agent
    .post(`/api/testplans/${testPlanID}/testcase-relations/${id}/actions/remove-issue-relations`)
    .send({ issueTestCaseRelationIDs })
    .then((response: any) => response.body);
}

export function addRelation({ testPlanID, id, issueIDs }: TEST_CASE.AddRelation) {
  return agent
    .post(`/api/testplans/${testPlanID}/testcase-relations/${id}/actions/add-issue-relations`)
    .send({ issueIDs })
    .then((response: any) => response.body);
}

export function getImportExportRecords(data: TEST_CASE.ImportExportQuery) {
  return agent
    .get('/api/test-file-records')
    .query(data)
    .then((response: any) => response.body);
}
