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
import { goTo } from 'common/utils';
import { getSubSiderInfoMap, getAppCenterAppList } from 'app/menus';
import layoutStore from 'layout/stores/layout';
import { orgPerm } from 'user/stores/_perm-org';
import { createStore } from 'core/cube';
import userStore from 'app/user/stores';
import { getOrgByDomain, getJoinedOrgs, updateOrg } from '../services/org';
import { getResourcePermissions } from 'user/services/user';
import permStore from 'user/stores/permission';
import breadcrumbStore from 'app/layout/stores/breadcrumb';
import { get, intersection, map } from 'lodash';
import { eventHub } from 'common/utils/event-hub';
import announcementStore from 'org/stores/announcement';

interface IState {
  currentOrg: ORG.IOrg;
  curPathOrg: string;
  orgs: ORG.IOrg[];
  initFinish: boolean;
}

const initState: IState = {
  currentOrg: {} as ORG.IOrg,
  curPathOrg: '',
  orgs: [],
  initFinish: false,
};

export const isAdminRoute = () => {
  const locationPath = window.location.pathname;
  return locationPath.split('/')?.[2] === 'sysAdmin'; // in case getOrgByDomain is invoked before App load, so that routeMarks is empty, then can't use isIn function
};

const org = createStore({
  name: 'org',
  state: initState,
  subscriptions: async ({ listenRoute }: IStoreSubs) => {
    listenRoute(async ({ params, isIn, isMatch, isLeaving }) => {
      if (isIn('orgIndex')) {
        const { orgName } = params;
        const [curPathOrg, initFinish] = org.getState((s) => [s.curPathOrg, s.initFinish]);
        if (!isAdminRoute() && initFinish && (curPathOrg !== orgName || orgName === '-') && !isMatch(/\w\/notFound/)) {
          layoutStore.reducers.clearLayout();
          org.effects.getOrgByDomain({ orgName });
        }

        if (orgName === '-') {
          layoutStore.reducers.setAnnouncementList([]);
        } else if (curPathOrg !== orgName) {
          const list = await announcementStore.effects.getAllNoticeListByStatus('published');
          layoutStore.reducers.setAnnouncementList(list);
        }
      }

      if (isLeaving('orgIndex')) {
        org.reducers.clearOrg();
      }

      eventHub.once('layout/mount', () => {
        const loginUser = userStore.getState((s) => s.loginUser);
        const orgId = org.getState((s) => s.currentOrg.id);
        // 非系统管理员
        if (!loginUser.isSysAdmin && orgId) {
          announcementStore.effects.getAllNoticeListByStatus('published').then((list) => {
            layoutStore.reducers.setAnnouncementList(list);
          });
        }
      });
    });

    const orgId = org.getState((s) => s.currentOrg.id);
    if (orgId) {
      announcementStore.effects.getAllNoticeListByStatus('published').then((list) => {
        layoutStore.reducers.setAnnouncementList(list);
      });
    }
  },
  effects: {
    async updateOrg({ call, update }, payload: Merge<Partial<ORG.IOrg>, { id: number }>) {
      const currentOrg = await call(updateOrg, payload);
      await org.effects.getJoinedOrgs(true);
      update({ currentOrg });
    },
    async getOrgByDomain({ call, update, select }, payload: { orgName: string }) {
      if (isAdminRoute()) {
        update({ initFinish: true });
        return;
      }
      let domain = window.location.hostname;
      if (domain.startsWith('local')) {
        domain = domain.split('.').slice(1).join('.');
      }
      const { orgName } = payload;
      // if orgName exist, check valid
      const resOrg = await call(getOrgByDomain, { domain, orgName });
      const orgs = select((s) => s.orgs); // get joined orgs

      if (!orgName) return;
      if (orgName === '-' && !Object.keys(resOrg).length) {
        if (orgs?.length) {
          goTo(`/${get(orgs, '[0].name')}`);
        }
        update({ curPathOrg: orgName, initFinish: true });
        return;
      }
      const curPathname = location.pathname;
      if (!Object.keys(resOrg).length) {
        goTo(goTo.pages.notFound);
        update({ initFinish: true });
      } else {
        const currentOrg = resOrg || {};
        const orgId = currentOrg.id;
        if (curPathname.startsWith(`/${orgName}/inviteToOrg`)) {
          if (orgs?.find((x) => x.name === currentOrg.name)) {
            goTo(`/${currentOrg.name}`, { replace: true });
          }
          return;
        }
        // if pathname is '/orgName/' instead of '/orgName', the route is not matched
        if (curPathname === `/${orgName}/`) {
          if (orgs?.find((x) => x.name === currentOrg.name)) {
            goTo(`/${currentOrg.name}`, { replace: true });
          }
        }

        if (currentOrg.name !== orgName) {
          goTo(location.pathname.replace(`/${orgName}`, `/${currentOrg.name}`), { replace: true }); // just replace the first match, which is org name
        }
        if (orgId) {
          const orgPermQuery = { scope: 'org', scopeID: `${orgId}` };
          const orgPermRes = await getResourcePermissions(orgPermQuery);

          // user doesn't joined the public org, go to dop
          // temporary solution, it will removed until new solution is proposed by PD
          // except Support role
          if (
            !orgPermRes?.data?.roles.includes('Support') &&
            resOrg?.isPublic &&
            curPathname?.split('/')[2] !== 'dop'
          ) {
            if (!orgs?.find((x) => x.name === currentOrg.name) || orgs?.length === 0) {
              goTo(goTo.pages.dopRoot, { replace: true });
            }
          }

          const orgAccess = get(orgPermRes, 'data.access');
          // 当前无该企业权限
          if (!orgAccess) {
            const joinOrgTip = map(orgPermRes.userInfo, (u) => u.nick).join(', ');
            userStore.reducers.setJoinOrgTip(joinOrgTip);
            goTo(goTo.pages.freshMan);
            update({ initFinish: true });
            return;
          }
          // redirect path by roles.
          // due to once orgAccess is false will redirect to freshMan page forcedly, then no need to hasAuth param
          const roles = get(orgPermRes, 'data.roles');
          setLocationByAuth({
            roles,
            ...payload,
          });

          // 有企业权限，正常用户
          const appMap = {} as {
            [k: string]: LAYOUT.IApp;
          };
          permStore.reducers.updatePerm(orgPermQuery.scope, orgPermRes.data);
          update({ currentOrg, curPathOrg: payload.orgName });
          const menusMap = getSubSiderInfoMap();
          const appCenterAppList = getAppCenterAppList();
          appCenterAppList.forEach((a) => {
            appMap[a.key] = a;
          });
          layoutStore.reducers.initLayout({
            appList: appCenterAppList,
            currentApp: appMap.dop,
            menusMap,
            key: 'dop',
          });
          breadcrumbStore.reducers.setInfo('curOrgName', currentOrg.displayName);
          update({ initFinish: true });
        }
      }
    },
    async getJoinedOrgs({ call, select, update }, force?: boolean) {
      const orgs = select((state) => state.orgs);
      if (!orgs.length || force) {
        const { list } = await call(getJoinedOrgs);
        update({ orgs: list });
      }
    },
  },
  reducers: {
    updateJoinedOrg(state, orgs: ORG.IOrg[]) {
      state.orgs = orgs;
    },
    clearOrg(state) {
      breadcrumbStore.reducers.setInfo('curOrgName', '');
      state.currentOrg = {} as ORG.IOrg;
      state.curPathOrg = '';
    },
  },
});

export default org;

const setLocationByAuth = (authObj: { roles: string[]; orgName: string }) => {
  const curPathname = location.pathname;
  const { roles, orgName } = authObj;
  const checkMap = {
    dataEngineer: {
      isCurPage: curPathname.startsWith(`/${orgName}/fdp`),
      authRole: intersection(orgPerm.entryFastData.role, roles),
    },
    orgCenter: {
      isCurPage: curPathname.startsWith(`/${orgName}/orgCenter`),
      authRole: intersection(orgPerm.entryOrgCenter.role, roles),
    },
    msp: {
      isCurPage: curPathname.startsWith(`/${orgName}/msp`),
      authRole: intersection(orgPerm.entryMsp.role, roles),
    },
    ecp: {
      isCurPage: curPathname.startsWith(`/${orgName}/ecp`),
      authRole: intersection(orgPerm.ecp.view.role, roles),
    },
    cmp: {
      isCurPage: curPathname.startsWith(`/${orgName}/cmp`),
      authRole: intersection(orgPerm.cmp.showApp.role, roles),
    },
    dop: {
      isCurPage: curPathname.startsWith(`/${orgName}/dop`),
      authRole: intersection(orgPerm.dop.read.role, roles),
    },
    freshMan: {
      isCurPage: curPathname.startsWith(`/${orgName}/freshMan`),
      authRole: [],
    },
    notFound: {
      isCurPage: curPathname.startsWith(`/${orgName}/notFound`),
      authRole: [],
    },
  };

  map(checkMap, (item) => {
    // 当前页，但是无权限，则重置
    if (item.isCurPage && !item.authRole.length) {
      let resetPath = goTo.resolve.orgRoot({ orgName });
      if (roles.toString() === 'DataEngineer') {
        // DataEngineer redirect to DataEngineer role page
        resetPath = `/${orgName}/fdp/__cluster__/__workspace__/data-govern-platform/data-source`;
      } else if (roles.toString() === 'Ops') {
        // 企业运维只有云管的权限
        resetPath = `/${orgName}/cmp/overview`;
      } else if (roles.toString() === 'EdgeOps') {
        // 边缘运维工程师只有边缘计算平台的权限
        resetPath = `/${orgName}/ecp/application`;
      }
      goTo(resetPath);
    }
  });
};
