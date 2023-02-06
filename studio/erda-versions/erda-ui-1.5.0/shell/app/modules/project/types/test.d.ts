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

declare namespace TEST_SET {
  interface TestSet {
    id: number;
    name: string;
    parentID: number;
    recycled: boolean;
    isDeleted: boolean;
    directoryName: string;
    order: number;
    creatorId: string;
    updatedId: string;
  }

  interface TestSetNode {
    title: string;
    key: string;
    id: number;
    recycled?: boolean;
    parentID: number;
    isLeaf?: boolean;
    iconClass?: string;
    iconType?: string;
    children?: TestSetNode[];
  }

  interface CreateBody {
    name: string;
    parentID: number;
    projectID: number;
  }

  interface GetQuery {
    projectID: number;
    recycled: boolean;
    parentID: number;
    testPlanID?: number;
  }

  interface DeleteQuery {
    testSetID: number;
  }

  interface RecoverQuery {
    testSetID: number;
    recoverToTestSetID: number;
  }

  interface MoveOrCopyQuery {
    projectId: number;
    testSetId: number;
    parentId: number;
    selectProjectId?: number;
  }

  interface CopyTestSet {
    copyToTestSetID: number;
    testSetID: number;
  }

  interface updateBody {
    testSetID: number;
    name: string;
    moveToParentID: number;
  }
}

declare namespace TEST_CASE {
  type CaseResult = 'INIT' | 'PASS' | 'FAIL' | 'BLOCK';
  type Priority = 'P0' | 'P1' | 'P2' | 'P3';
  type PageScope = 'caseModal' | 'testPlan' | 'testCase' | 'temp';
  type CaseFileType = 'xmind' | 'excel';

  interface API {
    apiId: number;
    apiInfo: string;
    apiRequest: string;
    apiResponse: string;
    assertResult: string;
    projectId: number;
    status: string;
    usecaseId: number;
    usecaseOrder: number;
  }

  interface CaseBody {
    id?: number;
    apis: API[];
    attachmentIds: [string];
    bugIds: number[];
    desc: string;
    descIssues: Array<{ additionalProperties: number }>;
    labels: LABEL.Item[];
    module: number;
    preCondition: string;
    priority: string;
    projectId: number;
    recycled: boolean;
    selectProjectId: number;
    stepAndResults: {
      result: string;
      step: string;
    };
    testSetId: number;
    title: string;
  }

  type CaseBodyPart = {
    [P in keyof CaseBody]?: CaseBody[P];
  };

  interface MetaEnum {
    fieldUniqueName: string;
    showName: string;
    value: string;
    icon: string | null;
    order: number | null;
  }

  interface Field {
    uniqueName: string;
    showName: string;
    dataType: string;
    module: string;
    defaultValue: string;
    optionsUrl: string | null;
    enumerable: boolean;
    multiple: boolean;
    fieldRequired: boolean;
    contentRequired: boolean;
    desc: string | null;
    icon: string | null;
    order: number | null;
    fieldTypeCode: string;
    enums: MetaEnum[];
  }

  interface CaseFilter {
    testSetId?: number;
    selectProjectId?: number;
    projectId?: number;
    usecaseIds?: number[];
    testPlanId?: number;
  }

  interface ImportData {
    payload: FormData;
    query: {
      projectID: number;
      testSetID: number;
      fileType: CaseFileType;
    };
  }

  interface ImportAutoData {
    payload: FormData;
    query: {
      projectID: number;
      spaceID?: string;
      fileType: CaseFileType;
    };
  }

  interface TestApi {
    apis: Array<{ url: string; id: string }>;
    projectTestEnvID: number;
    usecaseTestEnvID: number;
  }

  interface ChoosenInfo {
    isAll: boolean;
    primaryKeys: number[];
    exclude: number[];
  }

  interface QueryCase {
    orderField?: string[];
    pageSize?: number;
    pageNo: number;
    projectID?: string;
    updaterID?: number[];
    executorID?: number[];
    query?: string;
    priority?: string[];
    timestampSecUpdatedAtBegin?: number;
    timestampSecUpdatedAtEnd?: number;
    testSetID?: number;
    testPlanID?: number;
    recycled?: boolean;
    execStatus?: string;
  }

  interface QueryCaseSort {
    orderByPriorityAsc?: boolean;
    orderByPriorityDesc?: boolean;
    orderByUpdaterIDAsc?: boolean;
    orderByUpdaterIDDesc?: boolean;
    orderByUpdatedAtAsc?: boolean;
    orderByUpdatedAtDesc?: boolean;
  }

  interface ExportFileQuery extends Merge<QueryCase, QueryCaseSort> {
    fileType: 'xmind' | 'excel';
  }

  interface RelatedBug {
    iterationID: number;
    createdAt: string;
    issueID: number;
    issueRelationID: number;
    priority: 'URGENT' | 'HIGH' | 'NORMAL' | 'LOW';
    state: 'OPEN' | 'RESOLVED' | 'REOPEN' | 'WONTFIX' | 'DUP' | 'CLOSED';
    title: string;
  }

  interface TestCaseItemDetail {
    apis: null | API[];
    attachments: null | string[];
    bugIDs: null | number[];
    createdAt: string;
    creatorID: string;
    desc: string;
    id: number;
    labelIDs: null;
    labels: null;
    name: string;
    preCondition: string;
    priority: Priority;
    projectID: number;
    recycled: boolean;
    stepAndResults: stepAndResult[];
    testSetID: number;
    updatedAt: string;
    updaterID: string;
    testCaseID: number;
    apiCount: {
      total: number;
      created: number;
      running: number;
      passed: number;
      failed: number;
    };
  }

  interface TestCaseRelationItemDetail {
    apiCount: {
      total: number;
      created: number;
      running: number;
      passed: number;
      failed: number;
    };
    createdAt: string;
    creatorID: string;
    execStatus: CaseResult;
    executorID: string;
    id: number;
    issueBugs: RelatedBug[];
    name: string;
    priority: Priority;
    testCaseID: number;
    testPlanID: number;
    testSetID: number;
    updatedAt: string;
    updaterID: string;
  }

  type TestCaseItem = Merge<TestCaseItemDetail, TestCaseRelationItemDetail>;

  interface CaseDirectoryItem {
    testSetID: number;
    recycled: boolean;
    directory: string;
    testCases: TestCaseItem[];
    testCaseCountWithoutFilter?: number;
  }

  interface CasePage {
    total: number;
    testSets: CaseDirectoryItem[];
  }

  interface stepAndResult {
    result: string;
    step: string;
  }

  interface QueryCaseDetail {
    id: number;
  }

  interface CaseDetail extends TestCaseItem {
    planRelationCaseID: number;
  }

  interface CaseRelationDetail {
    createdAt: string;
    creatorID: string;
    execStatus: CaseResult;
    executorID: string;
    id: number;
    name: string;
    priority: Priority;
    testCaseID: number;
    testPlanID: number;
    testSetID: number;
    updatedAt: string;
    updaterID: string;
    apiCount: {
      total: number;
      created: number;
      running: number;
      passed: number;
      failed: number;
    };
    issueBugs: RelatedBug[];
  }

  type CaseTableRecord = Merge<
    TestCaseItem,
    { parent: Omit<CaseDirectoryItem, 'testCases'>; children?: TestCaseItem[] }
  >;

  interface BatchUpdate {
    testCaseIDs: number[];
    priority?: Priority;
    recycled?: boolean;
    moveToTestSetID?: number;
  }

  interface BatchCopy {
    copyToTestSetID: number;
    projectID: number;
    testCaseIDs: number[];
  }

  interface RemoveRelation {
    id: number;
    issueTestCaseRelationIDs: number[];
    testPlanID: number;
  }

  interface AddRelation {
    id: number;
    issueIDs: number[];
    testPlanID: number;
  }

  type ImportOrExport = 'import' | 'export';

  type ImportOrExportState = 'processing' | 'success' | 'fail' | 'pending';

  interface ImportExportQuery {
    projectId: number;
    types: ImportOrExport[];
  }

  interface ImportExportResult {
    list: ImportExportRecordItem[];
    counter: ImportExportCounter;
  }

  interface ImportExportCounter {
    import?: number;
    export?: number;
  }
  interface ImportExportRecordItem {
    apiFileUUID: string;
    createdAt: string;
    description: string;
    id: number;
    name: string;
    operatorID: string;
    operatorName: string;
    projectId: number;
    state: ImportOrExportState;
    type: ImportOrExport;
    updatedAt: string;
    testSetID: number;
  }
}
