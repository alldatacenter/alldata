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

declare namespace ISSUE {
  interface IIssueButton {
    permission: boolean;
    stateBelong: string;
    stateID: number;
    stateName: string;
  }
  interface Issue {
    id: number;
    projectID: number;
    appID: string;
    iterationID: number;
    type: 'REQUIREMENT' | 'TASK' | 'BUG' | 'TICKET' | 'EPIC';
    title: string;
    content: string;
    createdAt: string;
    updatedAt: string;
    planStartedAt: string;
    planFinishedAt: string;
    priority: 'URGENT' | 'HIGH' | 'NORMAL' | 'LOW';
    complexity: 'DIFFICULT' | 'NORMAL' | 'EASY';
    severity: 'FATAL' | 'SERIOUS' | 'NORMAL' | 'SLIGHT' | 'SUGGEST';
    creator: string;
    assignee: string;
    labels: string[];
    testPlanCaseRels: IRelativeTestCase[];
    issueButton: IIssueButton[];
    issueSummary: null | {
      processingCount: number;
      doneCount: number;
    };
    issueManHour: issueManHour;
    customUrl?: string;
    taskType: string;
    state: number;
  }

  interface IIssueButton {
    permission: boolean;
    stateBelong: string;
    stateID: number;
    stateName: string;
  }

  interface IRelativeTestCase {
    id: number;
    name: string;
    createdAt: string;
    execStatus: string;
    testCaseID: number;
    testPlanID: number;
    priority: string;
    testSetID: string;
  }

  interface issueManHour {
    thisElapsedTime: number; // 此次记录的工时
    estimateTime: number; // 预估工时，int64单位是分钟
    elapsedTime: number; // 已用工时，int64单位是分钟
    remainingTime: number; // 剩余工时，int64单位是分钟
    startTime: string; // 此次工作开始的时间
    workContent: string;
    isModifiedRemainingTime: boolean; // 是否保存过剩余工时
  }

  type RequirementState = 'OPEN' | 'WORKING' | 'TESTING' | 'DONE';

  type TaskState = 'OPEN' | 'WORKING' | 'DONE';

  interface Requirement extends Issue {
    state: RequirementState;
  }

  interface Task extends Issue {
    requirementTitle: string;
    state: TaskState;
    taskType: string;
  }
  interface Epic extends Issue {
    state: RequirementState;
  }
  interface Bug extends Issue {
    state: 'OPEN' | 'RESOLVED' | 'REOPEN' | 'WONTFIX' | 'DUP' | 'CLOSED';
    owner: string;
    bugStage: 'demandDesign' | 'architectureDesign' | 'codeDevelopment';
  }

  interface Ticket extends Issue {
    state: 'OPEN' | 'RESOLVED' | 'REOPEN' | 'WONTFIX' | 'DUP' | 'CLOSED';
  }

  type IssueState = 'OPEN' | 'RESOLVED' | 'REOPEN' | 'WONTFIX' | 'DUP' | 'CLOSED' | 'WORKING' | 'TESTING' | 'DONE';

  type IssueType = Requirement | Task | Bug | Ticket | Epic;

  interface IssueStream {
    id: string;
    issueID: string;
    operator: string;
    content: string;
    streamType: string;
    createdAt: string;
    updatedAt: string;
    mrInfo?: IssueStreamMrInfo;
  }

  interface IssueStreamMrInfo {
    appID: number;
    mrID: number;
    mrTitle: string;
  }

  interface IssueListQuery {
    pageSize?: number;
    state?: string;
    type?: string | string[];
    pageNo?: number;
    projectID: number;
    iterationID?: number;
    label?: number[];
    title?: string;
  }
  interface IssueStreamListQuery {
    pageSize?: number;
    pageNo: number;
    id: number;
  }
  interface IssueDetailQuery {
    id: number;
  }
  interface IssueStreamBody {
    type?: string;
    content: string;
    mrInfo?: IssueStreamMrInfo;
  }
  interface BatchUpdateBody {
    all: boolean; // +required 是否全量更新
    mine: boolean; // +required 是否过滤我的
    ids?: number[]; // +optional all为false时，不可为空
    assignee?: string;
    state?: string;
    newIterationID?: number;
    currentIterationID?: number; // 当前迭代id
    currentIterationIDs?: number[]; // 当前迭代id
    type: string; // 事件类型
  }

  interface IGetIssueQuery extends Omit<ISSUE.IssueListQuery, 'projectID'> {
    loadMore?: boolean;
    startFinishedAt?: number;
    endFinishedAt?: number;
  }

  interface TaskMap {
    [k: string]: {
      label: string;
      value: string;
      icon: string | React.ReactNode;
      nextStates: TaskState[];
      color: string;
      iconLabel: React.ReactNode;
    };
  }

  interface BacklogIssueCreateBody {
    type: string;
    title: string;
    content?: string;
    priority: 'URGENT' | 'HIGH' | 'NORMAL' | 'LOW';
    assignee?: string;
    iterationID?: number;
    projectID?: number;
  }

  interface BacklogIssueQuery {
    type?: string | string[];
    title?: string;
    priority?: 'URGENT' | 'HIGH' | 'NORMAL' | 'LOW';
    label?: string[];
    pageNo?: number;
    loadMore?: boolean;
    state?: number[];
  }

  interface ICreateRelationBody {
    id: number;
    relatedIssues: number;
    projectId: number;
    comment?: string;
    type: string;
  }

  interface RelationIssue {
    RelatingIssues?: ISSUE.IssueType[];
    RelatedIssues?: ISSUE.IssueType[];
  }

  interface IssueMilestoneEpic {
    planFinishedAt: string;
    title: string;
    id: number;
    relationNum: number;
    relationClosedNum: number;
  }
  interface IssueMilestone {
    year: number;
    month: number;
    epic: IssueMilestoneEpic[];
  }
  interface IUpdateIssueTypeQuery {
    id: number;
    type: string;
    projectId: number;
  }

  interface ICreateField {
    orgID: number;
    projectID: number;
    issueID: number;
    property: ISSUE_FIELD.IFiledItem[];
  }
  interface IFiledQuery {
    issueID: number;
    orgID: number;
    propertyIssueType: string;
  }
  interface IFieldInstanceBody {
    issueID: number;
    property: ISSUE_FIELD.IFiledItem[];
  }
}

interface CreateDrawerData {
  [k: string]: any;
  assignee: string;
  bugStage: string;
  priority: 'URGENT' | 'HIGH' | 'NORMAL' | 'LOW';
  complexity: 'DIFFICULT' | 'NORMAL' | 'EASY';
  iterationID: number;
  planFinishedAt: string;
  severity: 'FATAL' | 'SERIOUS' | 'NORMAL' | 'SLIGHT' | 'SUGGEST';
  taskType: string;
  title: string;
  content: string;
  issueManHour: { estimateTime: number; remainingTime: number };
  label: string[];
}
