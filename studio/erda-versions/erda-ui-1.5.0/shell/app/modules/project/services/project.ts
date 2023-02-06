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

export const getProjectList = ({ query, ...rest }: PROJECT.ListQuery): IPagingResp<PROJECT.Detail> => {
  return agent
    .get('/api/projects')
    .query({ ...rest, q: query })
    .then((response: any) => response.body);
};

export const getProjectInfo = (projectId: string | number): Promise<PROJECT.Detail> => {
  return agent.get(`/api/projects/${projectId}`).then((response: any) => response.body);
};

export const createProject = (form: PROJECT.CreateBody) => {
  return agent
    .post('/api/projects')
    .send(form)
    .then((response: any) => response.body);
};

export const updateProject = ({ projectId, ...values }: Merge<PROJECT.UpdateBody, { projectId: string | number }>) => {
  return agent
    .put(`/api/projects/${projectId}`)
    .send(values)
    .then((response: any) => response.body);
};

export const deleteProject = (projectId: string | number) => {
  return agent.delete(`/api/projects/${projectId}`).then((response: any) => response.body);
};

export const getLeftResources = (): PROJECT.LeftResources => {
  return agent.get('/api/orgs/actions/fetch-resources').then((response: any) => response.body);
};

export const getDashboard = (type: string) => {
  return agent
    .get(`/api/dashboard/system/blocks/${type}?scope=report&scopeId=org`)
    .then((response: any) => response.body);
};

export const getAutoTestSpaceDetail = ({ spaceId }: { spaceId: string }) => {
  return agent.get(`/api/autotests/spaces/${spaceId}`).then((response: any) => response.body);
};

const apis = {
  saveTestReport: {
    api: 'post@/api/projects/:projectId/test-reports',
  },
  getTestReportDetail: {
    api: 'get@/api/projects/:projectId/test-reports/:reportId',
  },
  getProjects: {
    api: '/api/projects',
  },
};
export const saveTestReport = apiCreator<(params: Merge<PROJECT.ITestReportBody, { projectId: string }>) => void>(
  apis.saveTestReport,
);

export const getTestReportDetail = apiCreator<
  (params: { reportId: string; projectId: string }) => PROJECT.ITestReportBody
>(apis.getTestReportDetail);

export const getProjectListNew = apiCreator<(p: PROJECT.ListQuery) => IPagingResp<PROJECT.Detail>>(apis.getProjects);
