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

import projectLabelStore from './label';
import { createStore } from 'core/cube';
import orgStore from 'app/org-home/stores/org';
import { getDefaultPaging, convertToFormData } from 'common/utils';
import {
  getIssues,
  getIssueDetail,
  createIssue,
  updateIssue,
  updateType,
  getIssueStreams,
  addIssueStream,
  deleteIssue,
  batchUpdateIssue,
  getIssueRelation,
  addIssueRelation,
  deleteIssueRelation,
  getMilestone,
  getFieldsByIssue,
  addFieldsToIssue,
  importFileInIssues,
  subscribe,
  unsubscribe,
  batchSubscribe,
} from '../services/issue';
import i18n from 'i18n';
import { PAGINATION } from 'app/constants';
import { ISSUE_TYPE } from 'project/common/components/issue/issue-config';
import issueFieldStore from 'org/stores/issue-field';

interface IState {
  issueList: ISSUE.Issue[];
  issuePaging: IPaging;
  fullRequirementList: ISSUE.Requirement[];
  openRequirementList: ISSUE.Requirement[];
  workingRequirementList: ISSUE.Requirement[];
  testingRequirementList: ISSUE.Requirement[];
  doneRequirementList: ISSUE.Requirement[];
  fullRequirementsPaging: IPaging;
  openRequirementsPaging: IPaging;
  workingRequirementsPaging: IPaging;
  testingRequirementsPaging: IPaging;
  doneRequirementsPaging: IPaging;
  requirementDetail: ISSUE.Requirement | null;
  taskList: ISSUE.Task[];
  openTaskList: ISSUE.Task[];
  workingTaskList: ISSUE.Task[];
  doneTaskList: ISSUE.Task[];
  taskDetail: ISSUE.Task | null;
  taskPaging: IPaging;
  openTaskPaging: IPaging;
  workingTaskPaging: IPaging;
  doneTaskPaging: IPaging;
  bugList: ISSUE.Bug[];
  ticketList: ISSUE.Ticket[];
  bugDetail: ISSUE.Bug | null;
  ticketDetail: ISSUE.Ticket | null;
  bugPaging: IPaging;
  ticketPaging: IPaging;
  requirementStreamList: ISSUE.IssueStream[];
  taskStreamList: ISSUE.IssueStream[];
  bugStreamList: ISSUE.IssueStream[];
  ticketStreamList: ISSUE.IssueStream[];
  epicStreamList: ISSUE.IssueStream[];
  requirementStreamPaging: IPaging;
  taskStreamPaging: IPaging;
  bugStreamPaging: IPaging;
  ticketStreamPaging: IPaging;
  epicList: ISSUE.Epic[];
  epicPaging: IPaging;
  epicDetail: ISSUE.Requirement | null;
  customFieldDetail: ISSUE.IFieldInstanceBody;
}

const initState: IState = {
  issueList: [],
  issuePaging: getDefaultPaging(),
  fullRequirementList: [],
  openRequirementList: [],
  workingRequirementList: [],
  testingRequirementList: [],
  doneRequirementList: [],
  fullRequirementsPaging: getDefaultPaging(),
  openRequirementsPaging: getDefaultPaging(),
  workingRequirementsPaging: getDefaultPaging(),
  testingRequirementsPaging: getDefaultPaging(),
  doneRequirementsPaging: getDefaultPaging(),
  requirementDetail: null,
  taskList: [],
  openTaskList: [],
  workingTaskList: [],
  doneTaskList: [],
  taskPaging: getDefaultPaging(),
  openTaskPaging: getDefaultPaging(),
  workingTaskPaging: getDefaultPaging(),
  doneTaskPaging: getDefaultPaging(),
  taskDetail: null,
  bugList: [],
  ticketList: [],
  bugPaging: {
    pageNo: 1,
    pageSize: PAGINATION.pageSize,
    total: 0,
  },
  ticketPaging: {
    pageNo: 1,
    pageSize: PAGINATION.pageSize,
    total: 0,
  },
  bugDetail: null,
  ticketDetail: null,
  requirementStreamList: [],
  taskStreamList: [],
  bugStreamList: [],
  ticketStreamList: [],
  epicStreamList: [],
  requirementStreamPaging: getDefaultPaging(),
  taskStreamPaging: getDefaultPaging(),
  bugStreamPaging: getDefaultPaging(),
  ticketStreamPaging: getDefaultPaging(),
  epicList: [],
  epicPaging: { ...getDefaultPaging(), pageSize: 200 },
  epicDetail: null,
  customFieldDetail: {} as ISSUE.IFieldInstanceBody,
};

const DETAIL_KEY_MAP = {
  REQUIREMENT: 'requirementDetail',
  TASK: 'taskDetail',
  BUG: 'bugDetail',
  TICKET: 'ticketDetail',
  EPIC: 'epicDetail',
};
const issueStore = createStore({
  name: 'issues',
  state: initState,
  subscriptions({ listenRoute }: IStoreSubs) {
    listenRoute(({ isEntering, isLeaving }) => {
      if (isEntering('issues')) {
        projectLabelStore.effects.getLabels({ type: 'issue' });
        const orgID = orgStore.getState((s) => s.currentOrg.id);
        issueFieldStore.effects.getSpecialFieldOptions({ orgID, issueType: 'BUG' });
        issueFieldStore.effects.getSpecialFieldOptions({ orgID, issueType: 'TASK' });
      } else if (isLeaving('issues')) {
        projectLabelStore.reducers.clearList();
      }
    });
  },
  effects: {
    async getAllIssues({ call, update, select, getParams }, payload: ISSUE.IGetIssueQuery) {
      const { projectId: projectID, iterationId: iterationID } = getParams();
      const { loadMore } = payload;
      const { list = [] } = await call(
        getIssues,
        { iterationID, ...payload, projectID },
        { paging: { key: 'issuePaging' } },
      );
      const oldList = select((s) => s.issueList);
      const newList = loadMore && payload.pageNo !== 1 ? oldList.concat(list) : list;
      update({ issueList: newList });
      return list;
    },
    async getIssues({ call, update, getParams, select }, payload: ISSUE.IGetIssueQuery) {
      const { projectId: projectID } = getParams();
      const { state, type, loadMore } = payload;
      let pagingKey = '';
      let listKey = '';
      if (type === ISSUE_TYPE.REQUIREMENT) {
        // if (state && !Array.isArray(state)) {
        //   pagingKey = `${state.toLowerCase()}RequirementsPaging`;
        //   listKey = `${state.toLowerCase()}RequirementList`;
        // } else {
        pagingKey = 'fullRequirementsPaging';
        listKey = 'fullRequirementList';
        // }
      } else if (type === ISSUE_TYPE.TASK) {
        pagingKey = 'taskPaging';
        listKey = 'taskList';
        if (state && !Array.isArray(state)) {
          pagingKey = `${state.toLowerCase()}TaskPaging`;
          listKey = `${state.toLowerCase()}TaskList`;
        } else {
          pagingKey = 'taskPaging';
          listKey = 'taskList';
        }
      } else if (type === ISSUE_TYPE.BUG) {
        pagingKey = 'bugPaging';
        listKey = 'bugList';
      } else if (type === ISSUE_TYPE.TICKET) {
        pagingKey = 'ticketPaging';
        listKey = 'ticketList';
      } else if (type === ISSUE_TYPE.EPIC) {
        pagingKey = 'epicPaging';
        listKey = 'epicList';
      }
      const { list = [] } = await call(getIssues, { ...payload, projectID }, { paging: { key: pagingKey } });
      const oldList = select((s) => s[listKey]);
      const newList = loadMore && payload.pageNo !== 1 ? oldList.concat(list) : list;
      update({ [listKey]: newList });
      return list;
    },
    async getIssueDetail({ call, update }, { type, id }: Merge<ISSUE.IssueDetailQuery, { type: string }>) {
      const detail = await call(getIssueDetail, { id });
      const detailKey = DETAIL_KEY_MAP[type];
      update({ [detailKey]: detail });
    },
    async getIssueStreams({ call, update }, { type, ...rest }: Merge<ISSUE.IssueStreamListQuery, { type: string }>) {
      const pagingKey = `${type.toLowerCase()}StreamPaging`;
      const { list = [] } = await call(getIssueStreams, rest, { paging: { key: pagingKey } });
      const listKey = `${type.toLowerCase()}StreamList`;
      update({ [listKey]: list });
    },
    async addIssueStream({ call }, issue: ISSUE.Issue, data: ISSUE.IssueStreamBody) {
      await call(addIssueStream, { id: issue.id, ...data });
      await issueStore.effects.getIssueStreams({
        type: issue.type,
        id: issue.id,
        pageNo: 1,
      });
    },
    async copyIssue({ call }, payload: ISSUE.IssueType) {
      const res = await call(createIssue, { ...payload });
      return res;
    },
    async createIssue({ call, getParams }, payload: ISSUE.IssueType, extraParams: { hideActionMsg: boolean }) {
      const { projectId: projectID = payload.projectID } = getParams();
      const msgTip = !extraParams?.hideActionMsg ? { successMsg: i18n.t('created successfully') } : undefined;
      const id = await call(createIssue, { ...payload, projectID: +projectID }, msgTip);
      return id;
      // const query = { type: payload.type, iterationID: payload.iterationID } as any;
      // if (payload.type === ISSUE_TYPE.REQUIREMENT) {
      //   query.state = 'open';
      // }
      // await issueStore.effects.getIssues(query);
    },
    async updateIssue({ call }, payload: ISSUE.IssueType) {
      const res = await call(updateIssue, payload);
      return res;
    },
    async updateType({ call, getParams }, payload: { id: number; type: string }) {
      const { projectId } = getParams();
      await call(updateType, { ...payload, projectId: +projectId });
    },
    async deleteIssue({ call }, issueId: number) {
      return call(deleteIssue, issueId, { successMsg: i18n.t('deleted successfully') });
    },
    async batchUpdateIssue({ call }, payload: ISSUE.BatchUpdateBody) {
      await call(batchUpdateIssue, payload);
    },
    async getIssueRelation({ call }, payload: { id: number; type: string }) {
      const { RelatingIssues, RelatedIssues } = await call(getIssueRelation, payload);
      return [RelatingIssues, RelatedIssues];
    },
    async addIssueRelation({ call }, payload: ISSUE.ICreateRelationBody) {
      const res = await call(addIssueRelation, payload, { successMsg: i18n.t('dop:relation added successfully') });
      return res;
    },
    async deleteIssueRelation({ call }, payload: { id: number; relatedIssueID: number; type: string }) {
      const res = await call(deleteIssueRelation, payload, { successMsg: i18n.t('deleted successfully') });
      return res;
    },
    async getMilestone({ call }) {
      const { list = [] } = await call(getMilestone);
      // update({ milestoneList: list });
      return list;
    },
    async addFieldsToIssue({ call, getParams }, payload: ISSUE.ICreateField, extraParams?: { customMsg?: string }) {
      const { projectId: projectID = payload.projectID } = getParams();
      const msgTip = extraParams?.customMsg ? { successMsg: extraParams.customMsg } : undefined;
      const res = await call(addFieldsToIssue, { ...payload, projectID: +projectID }, msgTip);
      return res;
    },
    async getFieldsByIssue({ call, update }, payload: ISSUE.IFiledQuery) {
      const customFieldDetail = (await call(getFieldsByIssue, payload)) as unknown as ISSUE.IFieldInstanceBody;
      if (customFieldDetail) {
        update({ customFieldDetail });
      }
      return customFieldDetail;
    },
    async importIssueFile({ call }, payload: { file: any; issueType: string; projectID: number | string }) {
      const orgID = orgStore.getState((s) => s.currentOrg.id);
      const { file, issueType, projectID } = payload;
      const formData = convertToFormData(file);
      const res = await call(importFileInIssues, {
        payload: formData,
        query: { orgID, projectID: +projectID, type: issueType },
      });
      return res;
    },
    async subscribe({ call }, payload: { id: number }) {
      return call(subscribe, payload);
    },
    async unsubscribe({ call }, payload: { id: number }) {
      return call(unsubscribe, payload);
    },
    async batchSubscribe({ call }, payload: { id: number | string; subscribers: Array<number | string> }) {
      return call(batchSubscribe, payload);
    },
  },
  reducers: {
    clearRequirementList(state) {
      return {
        ...state,
        fullRequirementsList: [],
        openRequirementList: [],
        workingRequirementList: [],
        testingRequirementList: [],
        doneRequirementList: [],
        fullRequirementsPaging: getDefaultPaging(),
        openRequirementsPaging: getDefaultPaging(),
        workingRequirementsPaging: getDefaultPaging(),
        testingRequirementsPaging: getDefaultPaging(),
        doneRequirementsPaging: getDefaultPaging(),
      };
    },
    clearEpic(state) {
      return {
        ...state,
        fullRequirementsList: [],
        openRequirementList: [],
        workingRequirementList: [],
        testingRequirementList: [],
        doneRequirementList: [],
        fullRequirementsPaging: getDefaultPaging(),
        openRequirementsPaging: getDefaultPaging(),
        workingRequirementsPaging: getDefaultPaging(),
        testingRequirementsPaging: getDefaultPaging(),
        doneRequirementsPaging: getDefaultPaging(),
      };
    },
    clearIssues(state, { type, state: exState }: { type: keyof typeof ISSUE_TYPE; state?: string }) {
      let pagingKey = '';
      let listKey = '';
      if (type === ISSUE_TYPE.REQUIREMENT) {
        if (exState) {
          pagingKey = `${exState.toLowerCase()}RequirementsPaging`;
          listKey = `${exState.toLowerCase()}RequirementList`;
        } else {
          pagingKey = 'fullRequirementsPaging';
          listKey = 'fullRequirementList';
        }
      } else if (type === ISSUE_TYPE.TASK) {
        pagingKey = 'taskPaging';
        listKey = 'taskList';
      } else if (type === ISSUE_TYPE.BUG) {
        pagingKey = 'bugPaging';
        listKey = 'bugList';
      } else if (type === ISSUE_TYPE.TICKET) {
        pagingKey = 'ticketPaging';
        listKey = 'ticketList';
      } else if (type === ISSUE_TYPE.ALL) {
        pagingKey = 'issuePaging';
        listKey = 'issueList';
      }
      return { ...state, [listKey]: [], [pagingKey]: getDefaultPaging() };
    },
    clearIssueDetail(state, type: keyof typeof ISSUE_TYPE) {
      return { ...state, [`${type.toLowerCase()}Detail`]: undefined };
    },
    updateCustomFieldDetail(state, detail) {
      state.customFieldDetail = detail;
    },
  },
});

export default issueStore;
