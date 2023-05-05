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

import { apiCreator } from 'core/service';

const apis = {
  getFieldsByIssue: {
    api: '/api/issues/actions/get-property-instance',
  },
};

export const getFieldsByIssue = apiCreator<(params: ISSUE.IFiledQuery) => ISSUE.IFieldInstanceBody>(
  apis.getFieldsByIssue,
);

export const getIssues = (query: ISSUE.IssueListQuery): Promise<IPagingResp<ISSUE.IssueType>> => {
  return agent
    .get('/api/issues')
    .query(query)
    .then((response: any) => response.body);
};

export const getIssueDetail = ({ id }: ISSUE.IssueDetailQuery): Promise<ISSUE.IssueType> => {
  return agent.get(`/api/issues/${id}`).then((response: any) => response.body);
};

export const getIssueStreams = ({ id, ...rest }: ISSUE.IssueStreamListQuery): IPagingResp<ISSUE.IssueStream> => {
  return agent
    .get(`/api/issues/${id}/streams`)
    .query(rest)
    .then((response: any) => response.body);
};

export const addIssueStream = ({ id, ...rest }: Merge<ISSUE.IssueStreamBody, { id: number }>) => {
  return agent
    .post(`/api/issues/${id}/streams`)
    .send(rest)
    .then((response: any) => response.body);
};

export const createIssue = ({ customUrl, ...form }: ISSUE.Issue) => {
  const url = customUrl || '/api/issues';
  return agent
    .post(url)
    .send(form)
    .then((response: any) => response.body);
};

export const updateIssue = ({ customUrl, ...form }: ISSUE.Issue) => {
  const { id } = form;
  const url = customUrl || '/api/issues';
  return agent
    .put(`${url}/${id}`)
    .send(form)
    .then((response: any) => response.body);
};

export const updateType = (payload: ISSUE.IUpdateIssueTypeQuery) => {
  return agent
    .put('/api/issues/actions/update-issue-type')
    .send(payload)
    .then((response: any) => response.body);
};

export const deleteIssue = (id: number) => {
  return agent.delete(`/api/issues/${id}`).then((response: any) => response.body);
};

export const batchUpdateIssue = (body: ISSUE.BatchUpdateBody) => {
  return agent
    .put('/api/issues/actions/batch-update')
    .send(body)
    .then((response: any) => response.body);
};

export const getIssueRelation = ({ id, type }: { id: number; type: string }): ISSUE.RelationIssue => {
  return agent.get(`/api/issues/${id}/relations${type ? `?type=${type}` : ''}`).then((response: any) => response.body);
};

export const addIssueRelation = (payload: ISSUE.ICreateRelationBody) => {
  const { id, ...rest } = payload;
  return agent
    .post(`/api/issues/${id}/relations`)
    .send(rest)
    .then((response: any) => response.body);
};

export const deleteIssueRelation = ({
  id,
  relatedIssueID,
  type,
}: {
  id: number;
  relatedIssueID: number;
  type: string;
}) => {
  return agent
    .delete(`/api/issues/${id}/relations/${relatedIssueID}${type ? `?type=${type}` : ''}`)
    .then((response: any) => response.body);
};

export const getMilestone = (): Promise<IPagingResp<ISSUE.IssueType>> => {
  return agent.get('/api/issues/actions/get-milestone').then((response: any) => response.body);
};
export const addFieldsToIssue = (payload: ISSUE.ICreateField) => {
  return agent
    .post('/api/issues/actions/create-property-instance')
    .send(payload)
    .then((response: any) => response.body);
};

export function importFileInIssues({ payload, query }: any): { successCount: number } {
  return agent
    .post('/api/issues/actions/import-excel')
    .query(query)
    .send(payload)
    .then((response: any) => response.body);
}

export const subscribe = ({ id }: { id: number | string }) => {
  return agent.post(`/api/issues/${id}/actions/subscribe`).then((response: any) => response.body);
};

export const unsubscribe = ({ id }: { id: number | string }) => {
  return agent.post(`/api/issues/${id}/actions/unsubscribe`).then((response: any) => response.body);
};

export const batchSubscribe = ({ id, ...body }: { id: number | string; subscribers: Array<number | string> }) => {
  return agent
    .put(`/api/issues/${id}/actions/batch-update-subscriber`)
    .send(body)
    .then((response: any) => response.body);
};
