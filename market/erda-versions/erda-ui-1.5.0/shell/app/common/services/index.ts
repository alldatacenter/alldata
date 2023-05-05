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
import { MemberScope } from 'common/stores/member-scope';
import { apiCreator } from 'core/service';

const apis = {
  getApps: {
    api: '/api/applications',
  },
};

export const getAppList = apiCreator<(p: APPLICATION.GetAppList) => IPagingResp<IApplication>>(apis.getApps);

interface IPlatformUser {
  avatar: string;
  email: string;
  id: string;
  locked: boolean;
  name: string;
  nick: string;
  phone: string;
  lastLoginAt: string;
  pwdExpireAt: string;
}

export const fetchLog = ({
  fetchApi,
  ...rest
}: {
  [k: string]: any;
  fetchApi?: string;
}): { lines: COMMON.LogItem[] } => {
  return agent
    .get(fetchApi || '/api/runtime/logs')
    .query(rest)
    .then((response: any) => response.body);
};

const convert = (payload: MEMBER.UpdateMemberBody) => {
  if (payload.scope.type === MemberScope.MSP) {
    return {
      ...payload,
      scope: {
        ...payload.scope,
        type: MemberScope.PROJECT,
      },
    };
  }
  return payload;
};

export function getMembers(payload: MEMBER.GetListServiceQuery) {
  const url = payload.scopeType === MemberScope.MSP ? '/api/msp/members' : '/api/members';
  const newPayload = { ...payload };
  if (newPayload.scopeType === MemberScope.MSP) {
    newPayload.scopeType = MemberScope.PROJECT;
  }
  return agent
    .get(url)
    .query(newPayload)
    .then((response: any) => response.body);
}

export const getPlatformUserList = (
  params: IPagingReq,
): Promise<RAW_RESPONSE<{ list: IPlatformUser[]; total: number }>> => {
  return agent
    .get('/api/users/actions/paging')
    .query(params)
    .then((response: any) => response.body);
};

export const searchPlatformUserList = (
  params: { q: string } & IPagingReq,
): Promise<RAW_RESPONSE<{ users: IPlatformUser[] }>> => {
  return agent
    .get('/api/users/actions/search')
    .query(params)
    .then((response: any) => response.body);
};

export function getRoleMap(payload: MEMBER.GetRoleTypeQuery): IPagingResp<MEMBER.IRoleType> {
  const url =
    payload.scopeType === MemberScope.MSP ? '/api/msp/members/action/list-roles' : '/api/members/actions/list-roles';
  const newPayload = { ...payload };
  if (newPayload.scopeType === MemberScope.MSP) {
    newPayload.scopeType = MemberScope.PROJECT;
  }
  return agent
    .get(url)
    .query(newPayload)
    .then((response: any) => response.body);
}

export function updateMembers(payload: MEMBER.UpdateMemberBody) {
  const url = payload.scope.type === MemberScope.MSP ? '/api/msp/members' : '/api/members';
  return agent
    .post(url)
    .send({ ...convert(payload), options: { rewrite: true } }) // 接口上写死options.rewrite=true，避免新增用户（已有的用户）设置角色无用
    .then((response: any) => response.body);
}

export function removeMember(payload: MEMBER.RemoveMemberBody) {
  const url = payload.scope.type === MemberScope.MSP ? '/api/msp/members/action/remove' : '/api/members/actions/remove';
  return agent
    .post(url)
    .send(convert(payload))
    .then((response: any) => response.body);
}

export const getMemberLabels = (): { list: Array<{ label: string; name: string }> } => {
  return agent.get('/api/members/actions/list-labels').then((response: any) => response.body);
};

export const getUsers = (payload: { q?: string; userID?: string | string[] }) => {
  return agent
    .get('/api/users')
    .query(payload)
    .then((response: any) => response.body);
};

export const getUsersNew = (payload: Merge<IPagingReq, { q?: string }>) => {
  return agent
    .get('/api/users/actions/search')
    .query(payload)
    .then((response: any) => response.body);
};

export const getApps = ({
  pageSize,
  pageNo,
  projectId,
  q,
  searchKey,
  memberID,
  ...rest
}: APPLICATION.GetAppList): IPagingResp<IApplication> => {
  return agent
    .get('/api/applications')
    .query({ pageSize, pageNo, projectId, q: q || searchKey, memberID, ...rest })
    .then((response: any) => response.body);
};

export const uploadFile = (file: any): IUploadFile => {
  return agent
    .post('/api/files')
    .send(file)
    .then((response: any) => response.body);
};

export const genOrgInviteCode = (payload: { orgId: number }): { verifyCode: string } => {
  return agent
    .post('/api/orgs/actions/gen-verify-code')
    .send(payload)
    .then((response: any) => response.body);
};

export const getRenderPageLayout = (payload: CONFIG_PAGE.RenderConfig) => {
  return agent
    .post(`/api/component-protocol/actions/render?scenario=${payload.scenario.scenarioKey}`)
    .send(payload)
    .then((response: any) => response.body);
};
// can ignore the default pageSize
