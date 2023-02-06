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
import { createStore } from 'core/cube';
import { createApp, initApp, queryTemplate } from '../services/application';
import breadcrumbStore from 'app/layout/stores/breadcrumb';
import { getVersionPushConfig, updateVersionPushConfig } from 'application/services/app-setting';
import { getAppDetail, updateAppDetail, remove, getBranchInfo } from 'application/services/application';
import { isEmpty, get } from 'lodash';
import permStore from 'user/stores/permission';
import { appMode } from 'application/common/config';
import layoutStore from 'layout/stores/layout';
import { HeadAppSelector } from 'application/common/app-selector';
import userStore from 'app/user/stores';
import { theme } from 'app/themes';
import { getAppMenu } from 'app/menus';
import { eventHub } from 'common/utils/event-hub';
import i18n from 'i18n';
import { goTo } from 'common/utils';

interface IState {
  versionPushConfig: APP_SETTING.PublisherConfig | null;
  detail: IApplication;
  branchInfo: APPLICATION.IBranchInfo[];
  curAppId: string;
}

const initState: IState = {
  versionPushConfig: null,
  detail: {} as IApplication,
  curAppId: '',
  branchInfo: [],
};

// 检测是否是app的首页，重定向
const checkIsAppIndex = (appMenu?: any) => {
  const appEnterRegex = /\/dop\/projects\/\d+\/apps\/\d+$/;
  if (appEnterRegex.test(location.pathname)) {
    appStore.reducers.onAppIndexEnter(appMenu);
  }
};
let loadingInApp = false;
const appStore = createStore({
  name: 'application',
  state: initState,
  subscriptions({ listenRoute }: IStoreSubs) {
    listenRoute(({ isIn, isLeaving, params }: IRouteInfo) => {
      const { appId } = params;
      const curAppId = appStore.getState((s) => s.curAppId);
      if (isIn('application')) {
        if (`${curAppId}` !== `${appId}`) {
          // 应用切换后才重新checkRouteAuth
          loadingInApp = true;
          appStore.reducers.updateCurAppId(appId);
          breadcrumbStore.reducers.setInfo('appName', '');
          permStore.effects.checkRouteAuth({
            type: 'app',
            id: appId,
            routeMark: 'application',
            cb() {
              appStore.effects.getBranchInfo({ appId }); // 请求app下全量branch
              appStore.effects.getAppDetail(appId).then((detail: IApplication) => {
                const curAppMenu = getAppMenu({ appDetail: detail });
                loadingInApp = false;
                layoutStore.reducers.setSubSiderInfoMap({
                  menu: curAppMenu,
                  key: 'application',
                  detail: { ...detail, icon: theme.appIcon },
                  getHeadName: () => <HeadAppSelector />,
                });
                checkIsAppIndex(curAppMenu); // 设置app的menu后，重定向
              });
            },
          });
        } else {
          !loadingInApp && checkIsAppIndex();
        }
      }

      if (isLeaving('application')) {
        layoutStore.reducers.setSubSiderInfoMap({ key: 'application', menu: [] });
        userStore.reducers.clearAppList();
        appStore.reducers.cleanAppDetail();
        appStore.reducers.updateCurAppId();
      }
    });
  },
  effects: {
    async getAppDetail({ call, select, update }, applicationId: string, force?: boolean): Promise<IApplication> {
      let detail = select((state) => state.detail);
      const newAppId = Number(applicationId);
      if (force || isEmpty(detail) || newAppId !== detail.id) {
        const _detail = await call(getAppDetail, newAppId);
        detail = { ..._detail, isProjectLevel: _detail.mode === appMode.PROJECT_SERVICE };
      }
      // gitRepoAbbrev后端为兼容旧的数据没有改，前端覆盖掉
      update({
        detail: {
          ...detail,
          gitRepoAbbrev: `${detail.projectName}/${detail.name}`,
        },
      });
      breadcrumbStore.reducers.setInfo('appName', detail.name);
      eventHub.emit('appStore/getAppDetail');
      return detail;
    },
    async getAppBlockNetworkStatus({ call, select, update }, applicationId: string) {
      const detail = select((state) => state.detail);
      const { blockStatus, unBlockEnd, unBlockStart } = await call(getAppDetail, applicationId);
      update({
        detail: {
          ...detail,
          blockStatus,
          unBlockEnd,
          unBlockStart,
        },
      });
    },
    async createApp({ call }, payload: APPLICATION.createBody) {
      return call(createApp, payload);
    },
    async initApp({ call }, payload: APPLICATION.initApp) {
      return call(initApp, payload);
    },
    async queryTemplate({ call }, payload: APPLICATION.queryTemplate) {
      const res = await call(queryTemplate, payload);
      return res;
    },
    async getVersionPushConfig({ getParams, call, update }) {
      const { appId } = getParams();
      const versionPushConfig = await call(getVersionPushConfig, appId);
      update({ versionPushConfig });
    },
    async updateVersionPushConfig({ getParams, call }, payload: Omit<APP_SETTING.PublisherUpdateBody, 'appID'>) {
      const { appId } = getParams();
      await call(updateVersionPushConfig, { ...payload, appID: appId });
      appStore.effects.getVersionPushConfig();
    },
    async updateAppDetail({ getParams, call }, payload: APPLICATION.createBody) {
      const params = getParams();
      await call(
        updateAppDetail,
        { values: payload, appId: params.appId },
        { successMsg: i18n.t('dop:modified successfully') },
      );
      await appStore.effects.getAppDetail(params.appId, true);
    },
    async remove({ call, getParams }) {
      const params = getParams();
      const result = await call(remove, params, {
        successMsg: i18n.t('deleted successfully'),
        fullResult: true,
      });
      if (result.success) {
        goTo(goTo.pages.project, { projectId: params.projectId, replace: true });
      }
    },
    async getBranchInfo({ call, update, select, getParams }, payload?: { appId?: string; enforce?: boolean }) {
      const { appId, enforce = false } = payload || {};
      const { detail, branchInfo } = select((s) => s);
      const newAppId = Number(appId);
      const params = getParams();
      if (enforce || isEmpty(branchInfo) || newAppId !== detail.id) {
        const curBranchInfo = await call(getBranchInfo, { appId: newAppId || params.appId });
        update({ branchInfo: curBranchInfo });
        return curBranchInfo;
      }
      return branchInfo;
    },
  },
  reducers: {
    updateAppDetail(state, data: IApplication) {
      state.detail = data;
      breadcrumbStore.reducers.setInfo('appName', data.name);
    },
    cleanAppDetail(state) {
      return { ...state, detail: {} };
    },
    clearVersionPushConfig(state) {
      state.versionPushConfig = null;
    },
    updateCurAppId(state, appId = '') {
      state.curAppId = appId;
    },
    onAppIndexEnter(_state, appMenu?: any[]) {
      let curAppMenu: any = appMenu;
      if (!curAppMenu) {
        const subSiderInfoMap = layoutStore.getState((s) => s.subSiderInfoMap);
        curAppMenu = get(subSiderInfoMap, 'application.menu');
      }
      // app首页重定向到第一个菜单链接
      const rePathname = get(curAppMenu, '[0].href');
      rePathname && goTo(rePathname, { replace: true });
    },
  },
});

export default appStore;
