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

declare namespace APP_NOTIFY {
  interface IUpdateNotifyQuery {
    id?: number;
    scopeType?: string;
    scopeId?: string;
    name?: string;
    enabled?: boolean;
    withGroup: boolean;
    notifyGroupId: number;
    notifyItemIds: number[];
    channels: string;
  }
  interface INotifyItem {
    category: string;
    dingdingTemplate: string;
    displayName: string;
    emailTemplate: string;
    id: number;
    label: string;
    mboxTemplate: string;
    mobileTemplate: string;
    name: string;
    params: string;
    scopeType: string;
  }
  interface INotifySource {
    name: string;
    params: any;
    sourceId: string;
    sourceType: string;
  }
  interface INotify {
    channels: string;
    createdAt: string;
    creator: string;
    enabled: boolean;
    id: number;
    name: string;
    notifyGroup: COMMON_NOTIFY.INotifyGroup;
    notifyItems: INotifyItem[];
    notifySources: INotifySource[];
    scopeId: string;
    scopeType: string;
    updatedAt: string;
  }
}
