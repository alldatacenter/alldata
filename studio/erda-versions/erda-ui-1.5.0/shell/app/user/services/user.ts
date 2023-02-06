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
import { RES_BODY } from 'core/service';

export const login = () => {
  return agent.get('/api/openapi/login').then((response: any) => response.body);
};

export const logout = () => {
  return agent.post('/api/openapi/logout').then((response: any) => response.body);
};

export const newUCLogout = () => {
  return agent.get('/api/uc/self-service/logout/browser').then((res: any) => {
    const token = res?.body?.logout_url?.split('token=')?.[1];
    return agent.get(`/api/uc/self-service/logout?token=${token}`).then((logoutRes: any) => logoutRes.body);
  });
};

export const getJoinedProjects = ({
  pageSize,
  pageNo,
  q,
  searchKey,
}: {
  pageSize: number;
  pageNo: number;
  q?: string;
  searchKey?: string;
}): IPagingResp<PROJECT.Detail> => {
  return agent
    .get('/api/projects/actions/list-my-projects')
    .query({ pageSize, pageNo, q: q || searchKey })
    .then((response: any) => response.body);
};

export const getJoinedApps = ({
  pageSize,
  pageNo,
  q,
  projectID,
  ...rest
}: {
  pageSize: number;
  pageNo: number;
  q?: string;
  projectID?: number;
  mode?: string;
}): IPagingResp<IApplication> => {
  return agent
    .get('/api/applications/actions/list-my-applications')
    .query({ ...rest, pageSize, pageNo, q, projectId: projectID })
    .then((response: any) => response.body);
};

export const pinApp = (appId: number) => {
  return agent.put(`/api/applications/${appId}/actions/pin`).then((response: any) => response.body);
};

export const unpinApp = (appId: number) => {
  return agent.put(`/api/applications/${appId}/actions/unpin`).then((response: any) => response.body);
};

export const getResourcePermissions = ({
  scope,
  scopeID,
}: IGetScopePermQuery): Promise<RES_BODY<IPermResponseData>> => {
  return agent
    .post('/api/permissions/actions/access')
    .send({ scope: { type: scope, id: String(scopeID) } })
    .then((response: any) => response.body);
};

export const getMspResourcePermissions = ({
  scope,
  scopeID,
}: IGetScopePermQuery): Promise<RES_BODY<IPermResponseData>> => {
  return agent
    .post('/api/msp/permission/access')
    .send({ scope: { type: scope, id: String(scopeID) } })
    .then((response: any) => response.body);
};
