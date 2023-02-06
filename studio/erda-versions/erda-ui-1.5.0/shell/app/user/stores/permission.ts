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
import { getResourcePermissions, getMspResourcePermissions } from '../services/user';
import { orgRoleMap } from './_perm-org';
import { projectRoleMap } from './_perm-project';
import { appRoleMap } from './_perm-app';
import { mspRoleMap } from './_perm-msp';
import { map, set, get, cloneDeep } from 'lodash';
import routeInfoStore from 'core/stores/route';
import { getUserMap } from 'core/stores/userMap';
import userStore from './index';
import { permPrefix, permState } from './_perm-state';
import { pickRandomlyFromArray } from 'common/utils';

const rolesMap = {
  app: appRoleMap,
  project: projectRoleMap,
  org: orgRoleMap,
  msp: mspRoleMap,
};

const getPermObj = (data: IPermResponseData, scope: string) => {
  const newPermObj = cloneDeep({ ...(permState[scope] || {}) });
  const { permissionList, resourceRoleList = [], access, roles = [], scopeInfo } = data;
  const ROLES = rolesMap[scope];
  map(permissionList, ({ resource, action }) => {
    if (resource.startsWith(permPrefix)) {
      const resourceArr = resource.split('.');
      resourceArr.shift();
      const resourceStr = resourceArr.join('.');
      const attr = `${resourceStr ? `${resourceStr}.` : ''}${action}.pass`;
      if (get(newPermObj, attr) === false) {
        set(newPermObj, attr, true);
      }
    }
  });
  map(resourceRoleList, ({ resource, action, resourceRole = '' }) => {
    if (resource.startsWith(permPrefix)) {
      const customRole = resourceRole.split(',');
      const resourceArr = resource.split('.');
      resourceArr.shift();
      const resourceStr = resourceArr.join('.');
      let totalRole = [] as string[];
      const roleAttr = `${resourceStr ? `${resourceStr}.` : ''}${action}.role`;
      const prevRole = get(newPermObj, roleAttr);
      if (prevRole) {
        map(prevRole, (rItem) => {
          if (ROLES[rItem]) {
            totalRole.push(rItem);
          }
        });
        totalRole = [...totalRole, ...customRole];
        set(newPermObj, roleAttr, totalRole);
      }
    }
  });
  if (scope === 'project') {
    newPermObj.access = access;
    newPermObj.roles = roles;
    newPermObj.scopeInfo = scopeInfo;
  }

  if (scope === 'app') {
    newPermObj.scopeInfo = scopeInfo;
  }

  return newPermObj;
};

const permission = createStore({
  name: 'permission',
  state: permState,
  effects: {
    async checkRouteAuth(_, payload: { id: string; type: string; cb?: any; routeMark: string; ignoreCache?: boolean }) {
      const { routeMarks } = routeInfoStore.getState((s) => s);

      const pathNeedCheckAuth = routeMarks.includes(payload.routeMark);
      if (pathNeedCheckAuth) {
        // const { id, type, cb, ignoreCache } = payload;
        // TODO: 其他人改了当前用户角色时，如果不触发重新拉取，缓存会产生问题，应该让后端在权限更新时推送消息过来更新，否则路由变化就拉取太频繁
        // 暂时使用每次进入时都拉取权限
        // if (permScope[type] === id && !ignoreCache) {
        //   // 使用缓存
        //   const loginUser = select(state => state.loginUser);
        //   cb({ loginUser });
        //   return;
        // }
        await permission.effects.getScopePermMap({
          scope: payload.type,
          scopeID: String(payload.id),
          routeMark: payload.routeMark,
          cb: payload.cb,
        });
      }
    },
    async getScopePermMap(
      { call },
      {
        cb = () => {},
        scope,
        scopeID,
        routeMark,
      }: {
        scope: string;
        scopeID: string;
        routeMark?: string;
        cb?: (arg?: any) => any;
      },
    ) {
      const realScope = scope === 'msp' ? 'project' : scope;
      const request = scope === 'msp' ? getMspResourcePermissions : getResourcePermissions;
      const data = (await call(request, { scope: realScope, scopeID })) as unknown as IPermResponseData;
      const { access, exist, contactsWhenNoPermission } = data;
      if (exist === false) {
        userStore.reducers.setNotFound();
        return;
      }
      // API 管理可以继承项目和应用权限，
      const needShowNoAuth = routeMark === 'apiManage' && ['project', 'app'].includes(realScope);
      permission.reducers.updatePerm(realScope, data);
      if (!access && !needShowNoAuth) {
        // 新的scope无权限时才清理，新的scope有权限时会在下面更新掉，无需清理
        const userMap = getUserMap();
        userStore.reducers.setNoAuth(
          map((contactsWhenNoPermission && pickRandomlyFromArray(contactsWhenNoPermission, 6)) || [], (id) => {
            const match = userMap[id] || {};
            return `${match.nick || match.name} (${match.phone || match.email})`;
          }).join(', '),
        );

        return;
      }
      cb(data);
    },
  },
  reducers: {
    updatePerm(state, scope?: string, data?: IPermResponseData | null) {
      if (!scope || !data) {
        return;
      }
      const newPermObj = getPermObj(data, scope);
      state[scope] = newPermObj;
    },
    clearScopePerm(state, scope: string) {
      state[scope] = permState[scope];
    },
  },
});

export default permission;
