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

export const getApps = ({
  pageSize,
  pageNo,
  projectId,
  q,
  searchKey,
  memberID,
}: APPLICATION.GetAppList): IPagingResp<IApplication> => {
  return agent
    .get('/api/applications')
    .query({ pageSize, pageNo, projectId, q: q || searchKey, memberID })
    .then((response: any) => response.body);
};

export const createApp = (form: APPLICATION.createBody): IApplication => {
  return agent
    .post('/api/applications')
    .send(form)
    .then((response: any) => response.body);
};

export const initApp = ({ applicationID, ...rest }: APPLICATION.initApp): string => {
  return agent
    .put(`/api/applications/${applicationID}/actions/init`)
    .send(rest)
    .then((response: any) => response.body);
};

export const queryTemplate = (payload: APPLICATION.queryTemplate) => {
  return agent
    .get('/api/applications/actions/list-templates')
    .query(payload)
    .then((response: any) => response.body);
};
export const getAppDetail = (applicationId: string | number): IApplication => {
  return agent.get(`/api/applications/${applicationId}`).then((response: any) => response.body);
};
export const updateAppDetail = ({ appId, values }: { appId: string; values: APPLICATION.createBody }): string => {
  return agent
    .put(`/api/applications/${appId}`)
    .send(values)
    .then((response: any) => response.body);
};

export const remove = ({ appId }: { appId: string }): boolean => {
  return agent.delete(`/api/applications/${appId}`).then((response: any) => response.body);
};

export const getBranchInfo = ({ appId }: { appId: number }): APPLICATION.IBranchInfo[] => {
  return agent
    .get('/api/cicds/actions/app-all-valid-branch-workspaces')
    .query({ appID: appId })
    .then((response: any) => response.body);
};
