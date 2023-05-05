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

declare namespace ConfigCenter {
  interface GetAppList {
    pageNo: number;
    tenantId: string;
    keyword: string;
  }

  interface RespAppList {
    total: number;
    list: string[];
  }

  interface GetConfigList {
    tenantId: string;
    groupId: string;
  }

  interface RespConfigList {
    [k: string]: any[];
  }

  interface SaveConfig {
    groupId: string;
    tenantId: string;
    configs: object[];
    operationType?: string;
  }
}
