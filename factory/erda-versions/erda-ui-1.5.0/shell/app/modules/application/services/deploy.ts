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

export const getRunTimes = (appId: string): DEPLOY.Runtime[] => {
  return agent
    .get('/api/runtimes')
    .query({ applicationId: appId })
    .then((response: any) => response.body);
};

export const getExtensions = (query?: {
  menu: boolean;
}): DEPLOY.ExtensionAction[] | Obj<Obj<DEPLOY.ExtensionAction[]>> => {
  return agent
    .get('/api/extensions')
    .query({ public: true, type: 'action', ...query })
    .then((response: any) => response.body);
};

export const getActionGroup = (query: { labels?: string } = {}): Obj<DEPLOY.IGroupExtensionActionObj[]> => {
  return agent
    .get('/api/extensions/actions/query-menu')
    .query(query)
    .then((response: any) => response.body);
};

export const getActionConfigs = ({ actionType }: { actionType: string }): DEPLOY.ActionConfig[] => {
  return agent.get(`/api/extensions/${actionType}`).then((response: any) => response.body);
};

export const addRuntimeByRelease = (body: DEPLOY.AddByRelease) => {
  return agent
    .post('/api/runtimes/actions/deploy-release')
    .send(body)
    .then((response: any) => response.body);
};

export const getReleaseByWorkspace = ({ appId, ...rest }: { appId: string; workspace: string }): DEPLOY.IReleaseMap => {
  return agent
    .get('/api/runtimes/actions/get-app-workspace-releases')
    .query({ appID: appId, ...rest })
    .then((response: any) => response.body);
};

// 我发起的
export const getLaunchedDeployList = (query: IPagingReq): IPagingResp<DEPLOY.IDeploy> => {
  return agent
    .get('/api/reviews/actions/list-launched-approval')
    .query(query)
    .then((response: any) => response.body);
};

// 需要审批的列表
export const getApprovalList = (query: IPagingReq): IPagingResp<DEPLOY.IDeploy> => {
  return agent
    .get('/api/reviews/actions/list-approved')
    .query(query)
    .then((response: any) => response.body);
};

// 审批
export const updateApproval = (data: DEPLOY.IUpdateApproveBody) => {
  return agent
    .put('/api/reviews/actions/updateReview')
    .send(data)
    .then((response: any) => response.body);
};
