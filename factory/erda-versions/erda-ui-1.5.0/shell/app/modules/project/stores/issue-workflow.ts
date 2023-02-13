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
import { createFlatStore } from 'core/cube';
import i18n from 'i18n';
import {
  getIssueList,
  getStatesByIssue,
  batchUpdateIssueState,
  deleteIssueState,
  addIssueState,
} from '../services/issue-workflow';

interface IState {
  issueList: ISSUE_WORKFLOW.IIssueItem[];
  workflowStateList: ISSUE_WORKFLOW.IIssueStateItem[];
}

const initState: IState = {
  issueList: [],
  workflowStateList: [],
};

const issueWorkflowStore = createFlatStore({
  name: 'issueWorkflow',
  state: initState,
  effects: {
    async getIssueList({ call, update }, payload: { projectID: number }) {
      const issueList = await call(getIssueList, payload);
      update({ issueList });
    },
    async getStatesByIssue({ call, update }, payload: ISSUE_WORKFLOW.IStateQuery) {
      const workflowStateList = await call(getStatesByIssue, payload);
      update({ workflowStateList });
      return workflowStateList;
    },
    async batchUpdateIssueState({ call }, payload: ISSUE_WORKFLOW.IUpdateQuery) {
      return call(batchUpdateIssueState, payload, { successMsg: i18n.t('updated successfully') });
    },
    async deleteIssueState({ call }, payload: { id: number; projectID: number }) {
      return call(deleteIssueState, payload, { successMsg: i18n.t('deleted successfully') });
    },
    async addIssueState({ call }, payload: ISSUE_WORKFLOW.ICreateStateQuery) {
      return call(addIssueState, payload, { successMsg: i18n.t('added successfully') });
    },
  },
  reducers: {
    clearIssueList(state) {
      state.issueList = [];
    },
  },
});

export default issueWorkflowStore;
