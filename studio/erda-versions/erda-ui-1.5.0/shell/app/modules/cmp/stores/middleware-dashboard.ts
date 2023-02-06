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

import { createStore } from 'core/cube';
import { getProjectList } from 'project/services/project';
import {
  getBaseInfo,
  getMiddlewares,
  getResourceList,
  getAddonUsage,
  getAddonDailyUsage,
  scale,
  getBackupFiles,
  getConfig,
  submitConfig,
  getAddonStatus,
  getCurrentProject,
} from '../services/middleware-dashboard';
import { PAGINATION } from 'app/constants';
import i18n from 'i18n';

interface IState {
  projectList: PROJECT.Detail[];
  overview: MIDDLEWARE_DASHBOARD.IOverview;
  middlewares: MIDDLEWARE_DASHBOARD.IMiddlewareDetail[];
  baseInfo: MIDDLEWARE_DASHBOARD.IBaseInfo;
  resourceList: MIDDLEWARE_DASHBOARD.IResource[];
  middelwaresPaging: IPaging;
  addonUsage: MIDDLEWARE_DASHBOARD.AddonUsage;
  addonDailyUsage: MIDDLEWARE_DASHBOARD.AddonDailyUsage;
  addonConfig: MIDDLEWARE_DASHBOARD.IActionsConfig;
  addonStatus: string;
  leftResources: MIDDLEWARE_DASHBOARD.LeftResources;
}

const initState: IState = {
  projectList: [],
  overview: {} as MIDDLEWARE_DASHBOARD.IOverview,
  middlewares: [],
  baseInfo: {} as MIDDLEWARE_DASHBOARD.IBaseInfo,
  resourceList: [],
  middelwaresPaging: {
    pageNo: 1,
    pageSize: PAGINATION.pageSize,
    total: 0,
  },
  addonUsage: {},
  addonDailyUsage: {
    abscissa: [],
    resource: [],
  },
  addonConfig: {} as MIDDLEWARE_DASHBOARD.IActionsConfig,
  addonStatus: '',
  leftResources: {} as MIDDLEWARE_DASHBOARD.LeftResources,
};

const middlewareDashboard = createStore({
  name: 'middleware-dashboard',
  state: initState,
  effects: {
    async getProjects({ call, update }, payload?: string) {
      const { list: projectList } = await call(getProjectList, { pageSize: 100, pageNo: 1, query: payload });
      update({ projectList });
    },
    async getMiddlewares({ call, update }, payload: MIDDLEWARE_DASHBOARD.IMiddlewaresQuery) {
      const res = await call(getMiddlewares, payload, { paging: { key: 'middelwaresPaging' } });
      const { overview, list } = res;
      update({ overview, middlewares: list });
      return res;
    },
    async getBaseInfo({ call, update }, payload: string) {
      const baseInfo = await call(getBaseInfo, payload);
      update({ baseInfo });
      return baseInfo;
    },
    async getResourceList({ call, update }, payload: string) {
      const resourceList = await call(getResourceList, payload);
      update({ resourceList });
    },
    async getAddonUsage({ call, update }, payload?: MIDDLEWARE_DASHBOARD.AddonUsageQuery) {
      const addonUsage = await call(getAddonUsage, payload);
      update({ addonUsage });
    },
    async getAddonDailyUsage({ call, update }, payload?: MIDDLEWARE_DASHBOARD.AddonUsageQuery) {
      const addonDailyUsage = await call(getAddonDailyUsage, payload);
      update({ addonDailyUsage });
    },
    async scale({ call }, payload: MIDDLEWARE_DASHBOARD.IScaleData) {
      const res = await call(scale, payload, { successMsg: i18n.t('saved successfully') });
      return res;
    },
    async getBackupFiles({ call }, payload) {
      const res = await call(getBackupFiles, payload);
      return res;
    },
    async getConfig({ call, update }, payload: Pick<MIDDLEWARE_DASHBOARD.IMiddleBase, 'addonID'>) {
      const addonConfig = await call(getConfig, payload);
      update({ addonConfig: addonConfig || {} });
      return addonConfig;
    },
    async submitConfig({ call }, payload: MIDDLEWARE_DASHBOARD.IUpdateConfig) {
      const res = await call(submitConfig, payload, { successMsg: i18n.t('saved successfully') });
      return res;
    },
    async getAddonStatus({ call, update, select }, payload: MIDDLEWARE_DASHBOARD.IMiddleBase) {
      const preStatus = select((s) => s.addonStatus);
      const res = await call(getAddonStatus, payload);
      if (preStatus !== res.status) {
        update({ addonStatus: res.status || '' });
      }
      return res;
    },
    async getLeftResources({ call, update }, payload: { name: string }) {
      const { list = [] } = await call(getCurrentProject, payload);
      const { cpuQuota, memQuota, cpuAddonUsed, cpuServiceUsed, memAddonUsed, memServiceUsed } = (list[0] ||
        {}) as PROJECT.Detail;
      const leftResources: MIDDLEWARE_DASHBOARD.LeftResources = {
        availableCpu: cpuQuota - cpuServiceUsed - cpuAddonUsed,
        availableMem: memQuota - memServiceUsed - memAddonUsed,
        totalCpu: cpuQuota,
        totalMem: memQuota,
      };
      update({ leftResources });
      return leftResources;
    },
  },
  reducers: {
    clearMiddlewares(state) {
      state.middlewares = [];
    },
    clearResourceList(state) {
      state.resourceList = [];
    },
    clearBaseInfo(state) {
      state.baseInfo = {} as MIDDLEWARE_DASHBOARD.IBaseInfo;
      state.addonConfig = {} as MIDDLEWARE_DASHBOARD.IActionsConfig;
    },
  },
});

export default middlewareDashboard;
