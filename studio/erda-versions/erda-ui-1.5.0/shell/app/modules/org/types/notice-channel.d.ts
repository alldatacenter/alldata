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

declare namespace NOTIFY_CHANNEL {
  interface ChannelProvider {
    name: string;
    displayName: string;
  }

  interface ChannelTypeOption {
    name: string;
    displayName: string;
    providers: ChannelProvider[];
  }

  interface NotifyChannelData {
    page: number;
    pageSize: number;
    total: number;
    data: NotifyChannel[];
  }

  interface NotifyChannel {
    page: number;
    pageSize: number;
    total: number;
    channelProviderType: {
      displayName: string;
      name: string;
    };
    config: Obj;
    createAt: string;
    creatorName: string;
    enable: boolean;
    id: string;
    name: string;
    type: {
      displayName: string;
      name: string;
    };
  }

  interface IChannelBody {
    channelProviderType: string;
    config: object;
    name: string;
    type: string;
    enable: boolean;
    id?: string;
  }

  interface ChannelEnableStatus {
    hasEnable: string;
    enableChannelName: string;
  }

  interface ChannelBody {
    data: NotifyChannel;
  }

  interface ChannelStatusBody {
    data: ChannelEnableStatus;
    success: boolean;
  }
}
