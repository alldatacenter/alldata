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

import { apiCreator } from 'core/service';

const apis = {
  getNotifyChannelTypes: {
    api: '/api/notify-channel/types',
  },
  getNotifyChannels: {
    api: '/api/notify-channels',
  },
  getNotifyChannelEnableStatus: {
    api: '/api/notify-channel/enabled/status',
  },
  setNotifyChannelEnable: {
    api: 'put@/api/notify-channel/enabled',
  },
  getNotifyChannelMethods: {
    api: 'get@/api/notify-channel/channels/enabled',
  },
  getNotifyChannel: {
    api: '/api/notify-channel',
  },
  addNotifyChannel: {
    api: 'post@/api/notify-channel',
  },
  editNotifyChannel: {
    api: 'put@/api/notify-channel',
  },
  deleteNotifyChannel: {
    api: 'delete@/api/notify-channel',
  },
};

export const getNotifyChannelTypes = apiCreator<() => NOTIFY_CHANNEL.ChannelTypeOption[]>(apis.getNotifyChannelTypes);

export const getNotifyChannels = apiCreator<
  (payload: { pageNo: number; pageSize: number; type: string }) => NOTIFY_CHANNEL.NotifyChannelData
>(apis.getNotifyChannels);

export const getNotifyChannelEnableStatus = apiCreator<(payload: { id: string; type: string }) => string>(
  apis.getNotifyChannelEnableStatus,
);
export const setNotifyChannelEnable = apiCreator<(payload: { id: string; enable: boolean }) => void>(
  apis.setNotifyChannelEnable,
);

export const getNotifyChannelMethods = apiCreator<() => Obj<string>>(apis.getNotifyChannelMethods);

export const getNotifyChannel = apiCreator<(payload: { id: string }) => NOTIFY_CHANNEL.NotifyChannel>(
  apis.getNotifyChannel,
);

export const addNotifyChannel = apiCreator<(payload: NOTIFY_CHANNEL.IChannelBody) => void>(apis.addNotifyChannel);

export const editNotifyChannel = apiCreator<(payload: NOTIFY_CHANNEL.IChannelBody) => void>(apis.editNotifyChannel);

export const deleteNotifyChannel = apiCreator<(payload: { id: string }) => void>(apis.deleteNotifyChannel);
