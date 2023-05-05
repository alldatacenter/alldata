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

declare namespace COMMON_NOTIFY {
  type ScopeType = 'org' | 'app' | 'project' | 'msp_env';

  interface INotifyGroupTarget {
    type: string;
    values: Array<{
      receiver: string;
      secret?: string;
    }>;
  }

  interface INotifyGroup {
    createdAt: string;
    creator: string;
    id: number;
    name: string;
    scopeId: string;
    scopeType: ScopeType;
    targets: INotifyGroupTarget[];
  }

  interface ICreateNotifyGroupQuery {
    id?: number;
    scopeType?: ScopeType;
    scopeId?: string;
    label?: string;
    name: string;
    targets: INotifyGroupTarget[];
  }

  interface IGetNotifyGroupQuery {
    scopeType: ScopeType;
    scopeId: string;
    label?: string;
    pageNo?: number;
    pageSize?: number;
  }

  interface ExternalUserInfo {
    uniKey: string;
    username: string;
    email: string;
    mobile: string;
  }
}
