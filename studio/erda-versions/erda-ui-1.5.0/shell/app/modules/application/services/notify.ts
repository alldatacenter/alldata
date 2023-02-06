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
import { apiCreator } from 'core/service';

const apis = {
  getNotifyChannelMethods: {
    api: 'get@/api/notify-channel/channels/enabled',
  },
};

export const getNotifyConfigs = (query: COMMON_NOTIFY.IGetNotifyGroupQuery): IPagingResp<APP_NOTIFY.INotify> => {
  return agent
    .get('/api/notifies')
    .query(query)
    .then((response: any) => response.body);
};

export const deleteNotifyConfigs = (id: string | number) => {
  return agent.delete(`/api/notifies/${id}`).then((response: any) => response.body);
};

export const createNotifyConfigs = (payload: APP_NOTIFY.IUpdateNotifyQuery) => {
  return agent
    .post('/api/notifies')
    .send(payload)
    .then((response: any) => response.body);
};

export const updateNotifyConfigs = ({ id, ...rest }: APP_NOTIFY.IUpdateNotifyQuery) => {
  return agent
    .put(`/api/notifies/${id}`)
    .send(rest)
    .then((response: any) => response.body);
};

export const toggleNotifyConfigs = ({ id, action }: { id: number; action: string }) => {
  return agent.put(`/api/notifies/${id}/actions/${action}`).then((response: any) => response.body);
};

export const getNotifyItems = (query?: { scopeType: string; module: string }): IPagingResp<APP_NOTIFY.INotifyItem> => {
  return agent
    .get('/api/notify-items')
    .query(query)
    .then((response: any) => response.body);
};

export const getNotifyChannelMethods = apiCreator<() => Obj<string>>(apis.getNotifyChannelMethods);
