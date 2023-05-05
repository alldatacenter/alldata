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

import React from 'react';
import breadcrumbStore from 'app/layout/stores/breadcrumb';
import { createStore } from 'core/cube';
import { theme } from 'app/themes';
import { pick, isEmpty, get } from 'lodash';
import {
  getProjectInfo,
  createProject,
  updateProject,
  deleteProject,
  getLeftResources,
  getProjectList,
  getAutoTestSpaceDetail,
} from '../services/project';
import { getApps } from 'common/services';
import i18n from 'i18n';
import userStore from 'app/user/stores';
import orgStore from 'app/org-home/stores/org';
import { getDefaultPaging, goTo } from 'common/utils';
import { HeadProjectSelector } from 'project/common/components/project-selector';
import layoutStore from 'layout/stores/layout';
import permStore from 'user/stores/permission';
import { PAGINATION } from 'app/constants';
import { getProjectMenu } from 'app/menus';
import issueWorkflowStore from 'project/stores/issue-workflow';

interface IState {
  list: PROJECT.Detail[];
  paging: IPaging;
  info: PROJECT.Detail;
  isAdmin: boolean;
  curProjectId: string;
  curSpaceId: string;
  statusPageVisible: boolean;
  projectAppList: IApplication[];
  projectAppPaging: IPaging;
  projectSettingAppList: IApplication[];
  projectSettingAppPaging: IPaging;
  leftResources: PROJECT.LeftResources | {};
}

const initState: IState = {
  list: [],
  paging: getDefaultPaging(),
  info: {} as any,
  isAdmin: false,
  statusPageVisible: false,
  curProjectId: '',
  curSpaceId: '',
  projectAppList: [],
  projectAppPaging: getDefaultPaging(),
  projectSettingAppList: [],
  projectSettingAppPaging: getDefaultPaging(),
  leftResources: {},
};

// 检测是否是app的首页，重定向
const checkIsProjectIndex = () => {
  const projectEnterMath = /\/dop\/projects\/\d+$/;
  if (projectEnterMath.test(location.pathname)) {
    project.reducers.onProjectIndexEnter();
  }
};

let loadingInProject = false;
const project = createStore({
  name: 'project',
  state: initState,
  subscriptions({ listenRoute }: IStoreSubs) {
    listenRoute(({ params, isIn, isLeaving }) => {
      const { projectId, spaceId } = params;
      const [curProjectId, curSpaceId] = project.getState((s) => [s.curProjectId, s.curSpaceId]);
      if (isLeaving('project') && isIn('orgProject')) {
        const info = project.getState((s) => s.info);
        userStore.reducers.cleanNoAuth();
        breadcrumbStore.reducers.setInfo('projectName', info.name || '');
      }
      if (isIn('project')) {
        if (`${curProjectId}` !== projectId) {
          loadingInProject = true;
          issueWorkflowStore.getStatesByIssue({ projectID: +projectId });
          // 项目切换后才重新checkRouteAuth
          project.reducers.updateCurProjectId(projectId);
          breadcrumbStore.reducers.setInfo('projectName', '');
          permStore.effects.checkRouteAuth({
            type: 'project',
            id: projectId,
            routeMark: 'project',
            cb() {
              project.effects.getProjectInfo(projectId, true).then((detail) => {
                loadingInProject = false;
                layoutStore.reducers.setSubSiderInfoMap({
                  key: 'project',
                  detail: { ...detail, icon: theme.projectIcon },
                  menu: getProjectMenu(projectId, location.pathname),
                  getHeadName: () => <HeadProjectSelector />,
                });
                checkIsProjectIndex();
              });
            },
          });
        } else {
          !loadingInProject && checkIsProjectIndex();
        }
      } else if (isIn('orgProject') && `${curProjectId}` !== projectId) {
        project.effects.getProjectInfo(projectId, true);
      }

      if (isIn('autoTestSpaceDetail') && curSpaceId !== spaceId) {
        project.reducers.updateCurSpaceId(spaceId);
        getAutoTestSpaceDetail({ spaceId }).then((res: any) => {
          breadcrumbStore.reducers.setInfo('testSpaceName', get(res, 'data.name'));
        });
      }

      if (isLeaving('autoTestSpaceDetail')) {
        project.reducers.updateCurSpaceId('');
      }

      if (isLeaving('project')) {
        userStore.reducers.clearProjectList();
        project.reducers.updateCurProjectId();
      }
    });
  },
  effects: {
    async getProjectList({ call, update }, payload: Optional<PROJECT.ListQuery, 'orgId' | 'pageSize'>) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      if (orgId !== undefined) {
        const { list } = await call(
          getProjectList,
          { orgId, pageSize: PAGINATION.pageSize, ...payload },
          { paging: { key: 'paging' } },
        );
        update({ list });
      }
    },
    async getProjectInfo({ select, call, update }, projectId: string | number, fromRouteChange?: boolean) {
      const projectInfo = select((s) => s.info);
      if (fromRouteChange && +projectInfo?.id === +projectId) {
        breadcrumbStore.reducers.setInfo('projectName', projectInfo.displayName || projectInfo.name);
        return projectInfo;
      }
      const info = await call(getProjectInfo, projectId);
      update({ info });
      breadcrumbStore.reducers.setInfo('projectName', info.displayName || info.name);
      return info;
    },
    async createProject({ call }, payload: PROJECT.CreateBody) {
      return call(createProject, payload, {
        successMsg: i18n.t('dop:project created successfully'),
        fullResult: true,
      });
    },
    async updateProject({ select, call }, payload: PROJECT.UpdateBody, isUpdateCluster?: boolean) {
      const projectInfo = select((state) => state.info);
      if (isEmpty(projectInfo)) {
        return;
      }
      const projectId = projectInfo.id;
      const updateFields = ['name', 'logo', 'desc', 'ddHook', 'clusterConfig', 'memQuota', 'cpuQuota'];
      const originalInfo: any = pick(projectInfo, updateFields);
      await call(
        updateProject,
        { ...originalInfo, ...payload, projectId },
        {
          successMsg: isUpdateCluster
            ? i18n.t('dop:settings updated successfully')
            : i18n.t('dop:project updated successfully'),
        },
      );
      if (!isUpdateCluster) {
        userStore.effects.getJoinedProjects({ pageNo: 1, pageSize: 20 });
      }
      await project.effects.getProjectInfo(projectId);
    },
    async deleteProject({ call, getParams }) {
      const { projectId } = getParams();
      await call(deleteProject, projectId, { successMsg: i18n.t('dop:project deleted successfully') });
    },
    async getProjectApps({ call, update, getParams, select }, payload: Optional<APPLICATION.GetAppList, 'projectId'>) {
      const routeParams = getParams();
      const { loadMore, ...rest } = payload;
      const projectId = rest.projectId || routeParams.projectId;
      const { total, list: projectAppList } = await call(
        getApps,
        { ...rest, projectId },
        { paging: { key: 'projectAppPaging' } },
      );
      const oldAppList = select((s) => s.projectAppList);
      const newProjectAppList = loadMore && payload.pageNo !== 1 ? oldAppList.concat(projectAppList) : projectAppList;
      update({ projectAppList: newProjectAppList });
      return { list: newProjectAppList, total };
    },
    async getProjectSettingApps({ call, update, getParams }, payload: Optional<APPLICATION.GetAppList, 'projectId'>) {
      const routeParams = getParams();
      const projectId = payload.projectId || routeParams.projectId;
      const { total, list: projectSettingAppList } = await call(
        getApps,
        { ...payload, projectId },
        { paging: { key: 'projectSettingAppPaging' } },
      );
      update({ projectSettingAppList });
      return { list: projectSettingAppList, total };
    },
    async getLeftResources({ call, update }) {
      const leftResources = await call(getLeftResources);
      update({ leftResources });
      return leftResources;
    },
  },
  reducers: {
    clearProjectList(state) {
      state.list = [];
      state.paging = getDefaultPaging();
    },
    clearLeftResources(state) {
      state.leftResources = {};
    },
    clearProjectAppList(state) {
      state.projectAppList = [];
    },
    clearProjectSettingAppList(state) {
      state.projectSettingAppList = [];
    },
    updateCurProjectId(state, proId = '') {
      state.curProjectId = proId;
    },
    updateCurSpaceId(state, spaceId) {
      state.curSpaceId = spaceId;
    },
    onProjectIndexEnter() {
      // project首页重定向到第一个菜单链接
      const subSiderInfoMap = layoutStore.getState((s) => s.subSiderInfoMap);
      const curMenus = get(subSiderInfoMap, 'project.menu') || [];
      const rePathname = curMenus[0]?.href;
      rePathname && goTo(rePathname, { replace: true });
    },
  },
});

export default project;
