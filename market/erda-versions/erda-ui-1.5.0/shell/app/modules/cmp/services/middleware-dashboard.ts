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

export const getMiddlewares = (
  payload: MIDDLEWARE_DASHBOARD.IMiddlewaresQuery,
): MIDDLEWARE_DASHBOARD.IMiddlewaresResp => {
  return agent
    .get('/api/middlewares')
    .query(payload)
    .then((response: any) => response.body);
};

export const getBaseInfo = (instanceId: string): MIDDLEWARE_DASHBOARD.IBaseInfo => {
  return agent.get(`/api/middlewares/${instanceId}`).then((response: any) => response.body);
};

export const getResourceList = (instanceId: string): MIDDLEWARE_DASHBOARD.IResource[] => {
  return agent.get(`/api/middlewares/${instanceId}/actions/get-resource`).then((response: any) => response.body);
};

export const getAddonUsage = (query?: MIDDLEWARE_DASHBOARD.AddonUsageQuery): MIDDLEWARE_DASHBOARD.AddonUsage => {
  return agent
    .get('/api/middlewares/resource/classification')
    .query(query)
    .then((response: any) => response.body);
};

export const getAddonDailyUsage = (
  query?: MIDDLEWARE_DASHBOARD.AddonUsageQuery,
): MIDDLEWARE_DASHBOARD.AddonDailyUsage => {
  return agent
    .get('/api/middlewares/resource/daily')
    .query(query)
    .then((response: any) => response.body);
};

// 扩缩容
export const scale = (payload: MIDDLEWARE_DASHBOARD.IScaleData) => {
  return agent
    .post('/api/addons/actions/scale')
    .send(payload)
    .then((response: any) => response.body);
};

// 备份
export const getBackupFiles = (payload: MIDDLEWARE_DASHBOARD.IMiddleBase): MIDDLEWARE_DASHBOARD.IBackupFiles => {
  return agent
    .get('/api/addons/actions/backupfile')
    .query(payload)
    .then((response: any) => response.body);
};

// 配置更新
export const getConfig = (
  payload: Pick<MIDDLEWARE_DASHBOARD.IMiddleBase, 'addonID'>,
): MIDDLEWARE_DASHBOARD.IActionsConfig => {
  return agent
    .get('/api/addons/actions/config')
    .query(payload)
    .then((response: any) => response.body);
};

export const submitConfig = (payload: MIDDLEWARE_DASHBOARD.IUpdateConfig): MIDDLEWARE_DASHBOARD.IActionsConfig => {
  return agent
    .post('/api/addons/actions/config')
    .send(payload)
    .then((response: any) => response.body);
};

export const getAddonStatus = (payload: MIDDLEWARE_DASHBOARD.IMiddleBase): MIDDLEWARE_DASHBOARD.IAddonStatus => {
  return agent
    .get('/api/addons/status')
    .query(payload)
    .then((response: any) => response.body);
};

export const getCurrentProject = (payload: { name: string }): IPagingResp<PROJECT.Detail> => {
  return agent
    .get('/api/projects')
    .query(payload)
    .then((response: any) => response.body);
};
