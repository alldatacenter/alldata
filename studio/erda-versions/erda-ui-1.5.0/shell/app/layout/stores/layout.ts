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
import { inviteToOrg } from 'layout/services';
import * as DiceWebSocket from 'core/utils/ws';
import { enableIconfont, setApiWithOrg } from 'common/utils';
import routeInfoStore from 'core/stores/route';
import { find, merge } from 'lodash';
import orgStore from 'app/org-home/stores/org';
import { getGlobal } from 'app/global-space';

const sendMsgUtilWsReady = async (targetWs: any, msg: { command: '__detach' | '__attach' }) => {
  while (targetWs.readyState !== 1 || targetWs.isReady !== true) {
    // eslint-disable-next-line no-await-in-loop
    await new Promise((resolve) => setTimeout(resolve, 300));
  }
  await layout.effects.notifyWs({ ws: targetWs, ...msg });
};

interface AnnouncementItem {
  [s: string]: any;
  id: number;
  content: string;
}

interface IState {
  currentApp: LAYOUT.IApp;
  appList: LAYOUT.IApp[];
  subSiderInfoMap: Obj;
  sideFold: boolean;
  subList: {
    [k: string]: any;
  };
  announcementList: AnnouncementItem[];
  customMain: Function | JSX.Element | null;
  showMessage: boolean;
  client: {
    height: number;
    width: number;
  };
  isImagePreviewOpen: boolean;
  scalableImgSrc: string;
}

const initState: IState = {
  currentApp: {} as LAYOUT.IApp,
  appList: [],
  subSiderInfoMap: {},
  subList: {},
  announcementList: [],
  customMain: null,
  showMessage: false,
  sideFold: false,
  client: {
    height: 0,
    width: 0,
  },
  isImagePreviewOpen: false,
  scalableImgSrc: '',
};

const layout = createStore({
  name: 'layout',
  state: initState,
  subscriptions: async ({ listenRoute }: IStoreSubs) => {
    const currentOrg = orgStore.getState((s) => s.currentOrg);
    // if (getGlobal('erdaInfo.currentOrgId')) {
    if (currentOrg.id) {
      const diceWs = DiceWebSocket.connect(setApiWithOrg('/api/websocket'));
      listenRoute(({ isEntering, isLeaving }) => {
        if (isEntering('pipeline') || isEntering('dataTask') || isEntering('deploy') || isEntering('testPlanDetail')) {
          // call ws when enter page
          sendMsgUtilWsReady(diceWs, { command: '__attach' });
        } else if (
          isLeaving('pipeline') ||
          isLeaving('dataTask') ||
          isLeaving('deploy') ||
          isLeaving('testPlanDetail')
        ) {
          sendMsgUtilWsReady(diceWs, { command: '__detach' });
        }
      });
    }
    listenRoute(async ({ isIn, isEntering }) => {
      const { switchToApp, switchMessageCenter } = layout.reducers;
      if (isIn('orgCenter')) {
        switchToApp('orgCenter');
      } else if (isIn('cmp')) {
        switchToApp('cmp');
      } else if (isIn('dop')) {
        switchToApp('dop');
      } else if (isIn('msp')) {
        switchToApp('msp');
      } else if (isIn('ecp')) {
        switchToApp('ecp');
      } else if (isIn('apiManage')) {
        switchToApp('apiManage');
      } else if (isIn('fdp')) {
        switchToApp('fdp');
      }

      if (
        isEntering('orgCenter') ||
        isEntering('cmp') ||
        isEntering('dop') ||
        isEntering('msp') ||
        isEntering('ecp') ||
        isEntering('sysAdmin')
      ) {
        enableIconfont('dice-icon');
      }

      switchMessageCenter(false);
    });
    window.addEventListener('resize', () => {
      layout.reducers.setClient();
    });
  },
  effects: {
    async notifyWs(_, payload: { ws: any; command: '__detach' | '__attach' }) {
      const { params, prevRouteInfo } = routeInfoStore.getState((s) => s);
      const { ws, command } = payload;
      const { appId, projectId } = params;
      let scopeType = '';
      let id = '';
      if (appId) {
        id = String(appId);
        scopeType = 'app';
        if (command === '__detach') {
          id = prevRouteInfo.params.appId; // in case switch application, so that use prev route to detach ws
        }
      } else if (projectId) {
        id = String(projectId);
        scopeType = 'project';
      }

      ws.send(JSON.stringify({ command, scope: { type: scopeType, id } }));
    },
    async inviteToOrg({ call }, payload: LAYOUT.InviteToOrgPayload) {
      const result = await call(inviteToOrg, payload);
      return result;
    },
  },
  reducers: {
    initLayout(state, payload: LAYOUT.IInitLayout | (() => LAYOUT.IInitLayout)) {
      let _payload = payload;
      if (typeof payload === 'function') {
        _payload = payload();
      }
      const { appList, currentApp, menusMap = {}, key } = (_payload as LAYOUT.IInitLayout) || {};
      if (key === 'sysAdmin' && !getGlobal('erdaInfo.isSysAdmin')) return;
      if (appList?.length) state.appList = appList;
      if (currentApp) state.currentApp = currentApp;
      state.subSiderInfoMap = merge(state.subSiderInfoMap, menusMap);
    },
    clearLayout(state) {
      state.subSiderInfoMap = {};
      state.appList = [];
      state.currentApp = {
        key: '',
        name: '',
        breadcrumbName: '',
        href: '',
      };
    },
    switchToApp(state, payload: string) {
      if (payload === (state.currentApp && state.currentApp.key)) return;
      const curApp = find(state.appList, { key: payload });
      if (curApp) {
        state.currentApp = curApp;
      }
    },
    setSubSiderInfoMap(state, payload: { [k: string]: any; key: string }) {
      const { key, ...rest } = payload;

      const siderInfoMap = state.subSiderInfoMap;
      if (!siderInfoMap[key]) {
        siderInfoMap[key] = {};
      }
      siderInfoMap[key] = { ...siderInfoMap[key], ...rest };
    },
    setSubSiderSubList(state, payload: Obj) {
      state.subList = { ...state.subList, ...payload };
    },
    setAnnouncementList(state, list: AnnouncementItem[]) {
      state.announcementList = list;
    },
    clearSubSiderSubList(state) {
      state.subList = {};
    },
    setClient(state) {
      state.client = {
        height: document.body.clientHeight,
        width: document.body.clientWidth,
      };
    },
    switchMessageCenter(state, payload) {
      state.showMessage = typeof payload === 'boolean' ? payload : !state.showMessage;
    },
    setImagePreviewOpen(state, status: boolean) {
      state.isImagePreviewOpen = status;
    },
    setScalableImgSrc(state, src: string) {
      state.scalableImgSrc = src;
    },
    // 动态更改appList中具体某个app的属性值，e.g. { cmp: { href: 'xxxx' } } 来运行时改变href
    updateAppListProperty(state, payload: Obj<Obj>) {
      const [appKey, valueObj] = Object.entries(payload)[0];
      const [key, value] = Object.entries(valueObj)[0];
      const targetApp = state.appList.find((app) => app.key === appKey);
      if (targetApp) {
        targetApp[key] = value;
      }
    },
    toggleSideFold(state, payload: boolean) {
      state.sideFold = payload;
    },
  },
});

export default layout;
