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

declare namespace MEMBER {
  type ScopeType = import('common/stores/member-scope').MemberScope;
  interface GetListQuery extends Partial<ScopeObj> {
    pageNo: number;
    scope: MemberScope;
    pageSize: number;
    roles?: string[];
    q?: string;
  }

  interface ScopeObj {
    scopeType: string;
    scopeId: string;
  }

  interface GetListServiceQuery {
    pageNo: number;
    pageSize: number;
    scopeType: ScopeType;
    scopeId: string;
    role?: string[];
    q?: string;
  }

  interface GetRoleTypeQuery {
    scopeType: string;
    scopeId?: number; // 不同企业根据是否开启发布商，角色会有区别，需要传scopeId
  }

  interface IRoleType {
    role: string;
    name: string;
  }

  interface MemberScope {
    id: string;
    type: ScopeType;
  }

  interface UpdateMemberBody {
    scope: MemberScope;
    roles: string[];
    userIds: string[];
    verifyCode?: string;
    targetScopeType?: string;
    targetScopeIds?: number[];
  }

  interface RemoveMemberBody {
    scopeType: ScopeType;
    scope: MemberScope;
    userIds: string[];
  }

  interface IMember {
    name: string;
    userId: string;
  }
}
