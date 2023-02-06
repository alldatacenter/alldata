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
import { getMembers, updateMembers, removeMember, getRoleMap, genOrgInviteCode } from '../services';
import userStore from 'app/user/stores';
import routeInfoStore from 'core/stores/route';
import i18n from 'app/i18n';
import { map } from 'lodash';
import { countPagination, getDefaultPaging } from '../utils';
import permStore from 'user/stores/permission';
import { PAGINATION } from 'app/constants';
import orgStore from 'app/org-home/stores/org';
import { MemberScope } from 'common/stores/member-scope';

export interface IState {
  paging: IPaging;
  list: IMember[];
  roleMap: Obj;
  roleMapMark: boolean;
}

interface UpdaterMemberExtra {
  isSelf?: boolean;
  forbidReload?: boolean;
  queryParams?: Obj;
  successMsg?: boolean;
}

export const createMemberStore = (scopeKey: MemberScope) => {
  const initState: IState = {
    paging: getDefaultPaging(),
    list: [],
    roleMap: {},
    roleMapMark: false,
  };

  const thisStore = createStore({
    name: `${scopeKey}Member`,
    state: initState,
    effects: {
      async getRoleMap({ call, update, select }, payload: MEMBER.GetRoleTypeQuery) {
        const roleMapMark = select((s) => s.roleMapMark);
        if (roleMapMark && payload.scopeType !== MemberScope.MSP) return;
        update({ roleMapMark: true });
        const result = await call(getRoleMap, payload);
        const roleMap = {};
        map(result.list, ({ role, name }) => {
          roleMap[role] = name;
        });
        update({ roleMap });
      },
      async getMemberList({ call, update }, payload: MEMBER.GetListQuery) {
        const { scope, ...rest } = payload;
        const result = await call(
          getMembers,
          { scopeType: scope.type, scopeId: scope.id, ...rest },
          { paging: { key: 'paging' } },
        );
        update({ list: result.list });
        return result;
      },
      async addMembers({ call }, payload: MEMBER.UpdateMemberBody, extra = { queryParams: {} } as UpdaterMemberExtra) {
        const { queryParams = {} } = extra;
        await call(updateMembers, payload, { successMsg: i18n.t('member added successfully') });
        await thisStore.effects.getMemberList({
          ...queryParams,
          pageNo: 1,
          pageSize: PAGINATION.pageSize,
          scope: payload.scope,
        });
      },
      async updateMembers(
        { call },
        payload: MEMBER.UpdateMemberBody,
        extra = { isSelf: false, forbidReload: false, queryParams: {}, successMsg: undefined } as UpdaterMemberExtra,
      ) {
        const { isSelf, forbidReload, queryParams = {} } = extra;
        await call(updateMembers, payload, {
          successMsg: extra.successMsg === false ? undefined : i18n.t('member updated successfully'),
        });
        if (forbidReload) {
          return;
        }
        const { pageNo } = thisStore.getState((s) => s.paging);
        await thisStore.effects.getMemberList({
          ...queryParams,
          pageNo,
          pageSize: PAGINATION.pageSize,
          scope: payload.scope,
        });
        if (isSelf) {
          const projectId = routeInfoStore.getState((s) => s.params.projectId);
          permStore.effects.getScopePermMap({
            scope: payload.scope.type,
            scopeID: payload.scope.type === 'msp' ? projectId : payload.scope.id,
          });
        }
      },
      async removeMember({ call }, payload: MEMBER.RemoveMemberBody, query?: Omit<MEMBER.GetListQuery, 'scope'>) {
        const { userIds, scope } = payload;
        const { id } = userStore.getState((s) => s.loginUser);
        const isSelf = userIds[0] === id; // 现在只有单个移除
        let successMsg = i18n.t('member deleted successfully');
        if (isSelf) {
          successMsg = i18n.t('exited the project successfully');
          if (scopeKey === MemberScope.APP) {
            successMsg = i18n.t('exited the application successfully');
          } else if (scopeKey === MemberScope.ORG) {
            successMsg = i18n.t('exited the organization successfully');
          }
        }

        await call(removeMember, { scope, userIds }, { successMsg });

        if (!isSelf && query) {
          // if is self,jump to upper scope and do not get list anymore
          const { total } = thisStore.getState((s) => s.paging);
          const { pageNo, pageSize, ...rest } = query;
          await thisStore.effects.getMemberList({
            ...rest,
            scope,
            ...countPagination({ pageNo, pageSize, total, minus: 1 }),
          });
        }
      },
      // 请求企业内成员
      async searchMembers({ call }, payload: MEMBER.GetListQuery) {
        const { scope, ...rest } = payload;
        const result = await call(getMembers, { scopeType: scope.type, scopeId: scope.id, ...rest });
        return result;
      },

      async genOrgInviteCode({ call }) {
        const orgId = orgStore.getState((s) => s.currentOrg.id);
        const result = await call(genOrgInviteCode, { orgId });
        return result;
      },
    },
    reducers: {
      cleanMembers(state) {
        return {
          ...state,
          paging: getDefaultPaging(),
          list: [],
        };
      },
    },
  });
  return thisStore;
};
