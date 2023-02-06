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

const getUrl = (scopeType?: COMMON_NOTIFY.ScopeType) =>
  scopeType === 'msp_env' ? '/api/msp/notify-groups' : '/api/notify-groups';

export const getNotifyGroups = (
  query?: COMMON_NOTIFY.IGetNotifyGroupQuery,
): IPagingResp<COMMON_NOTIFY.INotifyGroup> => {
  return agent
    .get(getUrl(query?.scopeType))
    .query(query)
    .then((response: any) => response.body);
};

export const deleteNotifyGroups = ({ id, scopeType }: { id: string; scopeType: COMMON_NOTIFY.ScopeType }) => {
  return agent.delete(`${getUrl(scopeType)}/${id}`).then((response: any) => response.body);
};

export const createNotifyGroups = (payload: COMMON_NOTIFY.ICreateNotifyGroupQuery) => {
  return agent
    .post(getUrl(payload.scopeType))
    .send(payload)
    .then((response: any) => response.body);
};

export const updateNotifyGroups = ({ id, ...rest }: COMMON_NOTIFY.ICreateNotifyGroupQuery) => {
  return agent
    .put(`${getUrl(rest.scopeType)}/${id}`)
    .send(rest)
    .then((response: any) => response.body);
};
