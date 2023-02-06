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

declare namespace TEST_PLAN {
  type PlanStatus = 'DOING' | 'DONE' | 'PAUSE';
  interface Plan {
    name: string;
    ownerName: string;
    ownerID: number;
    partnerIDs: string[];
    partnerNames: null;
    relatedModuleNames: null;
    relatedIterative: null;
    relatedIterativeId: null;
    status: PlanStatus;
    statusName: string;
    summary: null;
    isArchived: boolean;
    useCaseTotalCount: number;
    useCaseTestedCount: number;
    useCasePassedCount: number;
    relsCount: {
      total: number;
      init: number;
      succ: number;
      fail: number;
      block: number;
    };
    refBugCount: number;
    refUnclosedBugCount: number;
    startDate: null;
    endDate: null;
    creatorId: null;
    updatedId: null;
    projectID: null;
    apiTotalCount: null;
    apiPassedCount: null;
    apiPassedPercent: null;
    actorResult: null;
    id: number;
    createdAt: string;
    updatedAt: string;
    defaultId: boolean;
  }

  interface PlanListQuery {
    projectID: number;
    pageNo: number;
    pageSize?: number;
    type?: string;
    name?: string;
    status?: string[];
    userID?: number;
  }

  interface PlanPaging {
    list: PlanBodySimpleResponse[];
    total: number;
    pageSize: number;
  }

  interface PlanSubmitBody {
    id?: number;
    name: string;
    ownerID: number;
    partnerIDs: number[];
    type?: string;
    status: PlanStatus;
    summary?: string;
    timestampSecStartedAt?: number;
    timestampSecEndedAt?: number;
  }

  interface PlanBodySimpleResponse {
    id: number;
    name: string;
    ownerID: number;
    partnerIDs: number[];
    status: PlanStatus;
    creatorID: string;
    updatedID: string;
    createdAt: string;
    updatedAt: string;
    startedAt: number;
    endedAt: number;
    summary: number;
    relsCount: {
      total: number;
      init: number;
      succ: number;
      fail: number;
      block: number;
    };
    iterationID: number;
  }

  interface PlanBodyResponse extends PlanBodySimpleResponse {
    refBugCount: number;
    refUnclosedBugCount: number;
    startDate: string;
    endDate: string;
    updatedId: number;
    projectID: number;
    apiTotalCount: number;
    apiPassedCount: number;
    apiPassedPercent: number;
    actorResult: null;
  }

  interface relationsCase {
    testCaseIDs?: number[];
    testPlanID: number;
    testSetIDs?: number[];
  }

  interface PlanReportResponse {
    testPlan: {
      id: number;
      name: string;
      ownerID: string;
      partnerIDs: string[];
      status: PlanStatus;
      projectID: number;
      creator: string;
      createdAt: string;
      updatedAt: string;
      summary: string;
      startedAt: string;
      endedAt: string;
      relsCount: {
        total: number;
        init: number;
        succ: number;
        fail: number;
        block: number;
      };
    };
    relsCount: {
      total: number;
      init: number;
      succ: number;
      fail: number;
      block: number;
    };
    apiCount: {
      total: number;
      created: number;
      running: number;
      passed: number;
      failed: number;
    };
    executorStatus: {
      [k: string]: {
        total: number;
        init: number;
        succ: number;
        fail: number;
        block: number;
      };
    };
    userIDs: string[];
  }

  interface PlanBatch {
    relationIDs: number[];
    executorID?: string;
    execStatus?: string;
    testPlanID: number;
    delete?: boolean;
    testSetID?: number;
  }

  interface PlanDate {
    testPlanId: number;
    date: { [key: string]: string };
  }

  interface PlanStats {
    name: string;
    names: string[];
    datas: Array<{ name: string; value: number; data: null }>;
  }

  interface CaseApi {
    testPlanID: number;
    envID: number;
    testCaseIDs: number[];
  }

  interface ExecuteRecordQuery {
    testPlanId: string;
    projectId: number;
    pageNo: number;
    pageSize: number;
  }

  interface ExecuteRecordDetailQuery {
    runVersion?: string;
    pipelineID: number;
  }

  interface Pipeline {
    id: number;
    source: string;
    ymlName: string;
    triggerMode?: string;
    extra: {
      diceWorkspace: string;
      isAutoRun: true;
    };
    filterLabels: {
      diceWorkspace: string;
      orgName: string;
      projectID: string;
      testPlanID: string;
    };
    normalLabels: Record<string, any>;
    clusterName: string;
    status: string;
    progress: number;
    costTimeSec: number;
    timeBegin: string;
    timeEnd: string;
    timeCreated: string;
    timeUpdated: string;
  }

  interface PipelineTask {
    id: number;
    pipelineID: number;
    stageID: number;
    name: string;
    opType: string;
    type: string;
    status: string;
    extra: {
      uuid: string;
      allowFailure: false;
    };
    result: Record<string, any>;
    costTimeSec: number;
    queueTimeSec: number;
    timeBegin: string;
    timeEnd: string;
    timeCreated: string;
    timeUpdated: string;
  }

  interface PipeLineStage {
    id: number;
    pipelineID: number;
    name: string;
    status: string;
    costTimeSec: number;
    timeBegin: string;
    timeEnd: string;
    timeCreated: string;
    timeUpdated: string;
    pipelineTasks: PipelineTask[];
  }
  interface PipeLineDetail {
    id: number;
    projectID: number;
    commitDetail: Record<string, any>;
    labels: {
      diceWorkspace: string;
      orgName: string;
      projectID: string;
      testPlanID: string;
    };
    source: string;
    ymlName: string;
    ymlContent: string;
    extra: {
      diceWorkspace: string;
      isAutoRun: boolean;
    };
    namespace: string;
    clusterName: string;
    status: string;
    progress: number;
    costTimeSec: number;
    timeBegin: string;
    timeEnd: string;
    timeCreated: string;
    timeUpdated: string;
    pipelineStages: PipeLineStage[];
    pipelineCron: {
      id: number;
      timeCreated: string;
      timeUpdated: string;
      applicationID: number;
      branch: string;
      cronExpr: string;
      cronStartTime: string | null;
      pipelineYmlName: string;
      basePipelineID: number;
      enable: boolean | null;
    };
    pipelineButton: {
      canManualRun: boolean;
      canCancel: boolean;
      canForceCancel: boolean;
      canRerun: boolean;
      canRerunFailed: boolean;
      canStartCron: boolean;
      canStopCron: boolean;
      canPause: boolean;
      canUnpause: boolean;
      canDelete: boolean;
    };
  }
}
