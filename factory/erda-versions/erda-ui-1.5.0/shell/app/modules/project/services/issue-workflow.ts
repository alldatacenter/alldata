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

export const getIssueList = (query: { projectID: number }): ISSUE_WORKFLOW.IIssueItem[] => {
  return agent
    .get('/api/issues/actions/get-states')
    .query(query)
    .then((response: any) => response.body);
};

export const getStatesByIssue = (query: ISSUE_WORKFLOW.IStateQuery): ISSUE_WORKFLOW.IIssueStateItem[] => {
  return agent
    .get('/api/issues/actions/get-state-relations')
    .query(query)
    .then((response: any) => response.body);
};

export const addIssueState = (query: ISSUE_WORKFLOW.ICreateStateQuery) => {
  return agent
    .post('/api/issues/actions/create-state')
    .send(query)
    .then((response: any) => response.body);
};

export const batchUpdateIssueState = (query: ISSUE_WORKFLOW.IUpdateQuery) => {
  return agent
    .put('/api/issues/actions/update-state-relation')
    .send(query)
    .then((response: any) => response.body);
};

export const deleteIssueState = (query: { id: number; projectID: number }) => {
  return agent
    .delete('/api/issues/actions/delete-state')
    .query(query)
    .then((response: any) => response.body);
};
